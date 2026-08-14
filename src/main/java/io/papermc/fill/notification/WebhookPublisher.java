/*
 * Copyright 2024 PaperMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.papermc.fill.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.database.WebhookEntity;
import io.papermc.fill.event.FillEvent;
import io.papermc.fill.service.WebhookService;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Delivers {@link FillEvent}s to registered webhook endpoints.
 *
 * <p>Deliveries follow the
 * <a href="https://www.standardwebhooks.com/spec/">Standard Webhooks</a> specification:
 * each delivery is signed with an HMAC-SHA256 over {@code webhook-id + "." + webhook-timestamp + "." + body}
 * using the webhook's own secret, and retried with exponential backoff.</p>
 *
 * <p>Events are notifications, not data: consumers are expected to refetch the affected
 * resources from the API after verifying a delivery.</p>
 */
@Component
@NullMarked
public class WebhookPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebhookPublisher.class);
  private static final int MAX_ATTEMPTS = 5;
  private static final int MAX_CONCURRENT_DELIVERIES = 16;
  private static final int MAX_QUEUED_DELIVERIES = 256;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final WebhookService webhooks;
  private final ObjectMapper json;
  private final RestClient http;
  private final ExecutorService executor = new ThreadPoolExecutor(
    MAX_CONCURRENT_DELIVERIES,
    MAX_CONCURRENT_DELIVERIES,
    0L,
    TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(MAX_QUEUED_DELIVERIES),
    Thread.ofVirtual().name("webhook-delivery-", 0).factory()
  );

  @Autowired
  public WebhookPublisher(final WebhookService webhooks) {
    this.webhooks = webhooks;
    this.json = new ObjectMapper();
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
    requestFactory.setReadTimeout(READ_TIMEOUT);
    this.http = RestClient.builder()
      .requestFactory(requestFactory)
      .defaultHeader(HttpHeaders.USER_AGENT, "Fill (Webhooks)")
      .build();
  }

  @EventListener
  public void onFillEvent(final FillEvent event) {
    final List<WebhookEntity> targets;
    try {
      targets = this.webhooks.list();
    } catch (final Exception exception) {
      LOGGER.warn("Failed to list webhooks for event {}", event.type(), exception);
      return;
    }
    for (final WebhookEntity webhook : targets) {
      try {
        this.executor.submit(() -> this.deliver(webhook, event));
      } catch (final RejectedExecutionException exception) {
        LOGGER.warn("Webhook delivery queue is full; dropping {} event for {}", event.type(), webhook.url());
      }
    }
  }

  @PreDestroy
  public void close() {
    this.executor.shutdownNow();
  }

  private void deliver(final WebhookEntity webhook, final FillEvent event) {
    final String deliveryId = "fill_" + UUID.randomUUID();
    final String timestamp = Long.toString(Instant.now().getEpochSecond());

    final byte[] body;
    final String signature;
    try {
      body = this.createPayload(event);
      signature = createSignature(webhook.secret(), deliveryId, timestamp, body);
    } catch (final Exception exception) {
      LOGGER.error("Failed to prepare webhook delivery {} to {}", deliveryId, webhook.url(), exception);
      this.webhooks.recordDelivery(webhook, "failed");
      return;
    }

    Exception lastFailure = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        this.http.post()
          .uri(webhook.url())
          .contentType(MediaType.APPLICATION_JSON)
          .header("webhook-id", deliveryId)
          .header("webhook-timestamp", timestamp)
          .header("webhook-signature", signature)
          .body(body)
          .retrieve()
          .toBodilessEntity();
        this.webhooks.recordDelivery(webhook, "delivered");
        return;
      } catch (final Exception exception) {
        lastFailure = exception;
        if (attempt < MAX_ATTEMPTS) {
          LOGGER.warn(
            "Failed to deliver webhook {} to {} (attempt {}/{}): {}",
            deliveryId,
            webhook.url(),
            attempt,
            MAX_ATTEMPTS,
            exception.getMessage()
          );
          if (!sleep(attempt)) {
            return;
          }
        }
      }
    }

    LOGGER.error("Giving up on webhook delivery {} to {} after {} attempts", deliveryId, webhook.url(), MAX_ATTEMPTS, lastFailure);
    this.webhooks.recordDelivery(webhook, "failed");
  }

  private byte[] createPayload(final FillEvent event) {
    final ObjectNode root = this.json.createObjectNode();
    root.put("type", event.type());
    root.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(event.time()));
    final ObjectNode data = root.putObject("data");
    data.put("project", event.project().key());
    data.put("version", event.version().key());
    switch (event) {
      case FillEvent.BuildPublished published -> {
        data.put("build", published.build().number());
        data.put("channel", published.build().channel().name());
      }
      case FillEvent.BuildPromoted promoted -> {
        data.put("build", promoted.build().number());
        data.put("channel", promoted.build().channel().name());
      }
      case FillEvent.VersionCreated _, FillEvent.VersionUpdated _ -> {
      }
    }
    try {
      return this.json.writeValueAsBytes(root);
    } catch (final JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize webhook payload", exception);
    }
  }

  @VisibleForTesting
  static String createSignature(final String secret, final String deliveryId, final String timestamp, final byte[] body) {
    try {
      final Mac mac = Mac.getInstance("HmacSHA256");
      if (!secret.startsWith("whsec_")) {
        throw new IllegalArgumentException("Invalid webhook secret prefix");
      }
      final byte[] key = Base64.getDecoder().decode(secret.substring("whsec_".length()));
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      mac.update(deliveryId.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) '.');
      mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) '.');
      mac.update(body);
      return "v1," + Base64.getEncoder().encodeToString(mac.doFinal());
    } catch (final GeneralSecurityException exception) {
      throw new IllegalStateException("Could not create webhook signature", exception);
    }
  }

  private static boolean sleep(final int attempt) {
    try {
      final long millis = (long) Math.pow(2.0, attempt - 1) * 1000L + ThreadLocalRandom.current().nextLong(250L);
      Thread.sleep(millis);
      return true;
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
