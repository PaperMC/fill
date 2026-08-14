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
import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.database.WebhookEntity;
import io.papermc.fill.event.FillEvent;
import io.papermc.fill.model.DeliveryStatus;
import io.papermc.fill.service.WebhookService;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
 * resources from the API after verifying a delivery. Events may be delivered out of order.</p>
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
  private final Clock clock;
  private final ObjectMapper json;
  private final RestClient http;
  private final RetryTemplate retry = new RetryTemplate(RetryPolicy.builder()
    .maxRetries(MAX_ATTEMPTS - 1)
    .delay(Duration.ofSeconds(1))
    .jitter(Duration.ofMillis(250))
    .multiplier(2)
    .maxDelay(Duration.ofSeconds(8))
    .excludes(Error.class)
    .predicate(WebhookPublisher::isRetryable)
    .build());
  private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
    Thread.ofVirtual().name("webhook-delivery-", 0).factory()
  );
  private final Semaphore admission = new Semaphore(MAX_CONCURRENT_DELIVERIES + MAX_QUEUED_DELIVERIES);
  private final Semaphore concurrency = new Semaphore(MAX_CONCURRENT_DELIVERIES);

  @Autowired
  public WebhookPublisher(final WebhookService webhooks, final Clock clock, final ObjectMapper json) {
    this.webhooks = webhooks;
    this.clock = clock;
    this.json = json;
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
      if (!this.admission.tryAcquire()) {
        LOGGER.warn("Webhook delivery queue is full; dropping {} event for {}", event.type(), webhook.url());
        continue;
      }
      try {
        this.executor.execute(() -> {
          try {
            this.concurrency.acquire();
            try {
              this.deliver(webhook, event);
            } finally {
              this.concurrency.release();
            }
          } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            this.admission.release();
          }
        });
      } catch (final RejectedExecutionException exception) {
        this.admission.release();
      }
    }
  }

  @PreDestroy
  public void close() {
    this.executor.shutdownNow();
  }

  private void deliver(final WebhookEntity webhook, final FillEvent event) {
    final String deliveryId = "fill_" + UUID.randomUUID();
    final String timestamp = Long.toString(this.clock.instant().getEpochSecond());

    final byte[] body;
    final String signature;
    try {
      body = this.createPayload(event);
      signature = createSignature(webhook.secret(), deliveryId, timestamp, body);
    } catch (final Exception exception) {
      LOGGER.error("Failed to prepare webhook delivery {} to {}", deliveryId, webhook.url(), exception);
      this.webhooks.recordDelivery(webhook, DeliveryStatus.FAILED);
      return;
    }

    try {
      this.retry.execute(() -> {
        this.http.post()
          .uri(webhook.url())
          .contentType(MediaType.APPLICATION_JSON)
          .header("webhook-id", deliveryId)
          .header("webhook-timestamp", timestamp)
          .header("webhook-signature", signature)
          .body(body)
          .exchange((request, response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
              throw new NonSuccessfulDelivery(response.getStatusCode());
            }
            return null;
          });
        return null;
      });
      this.webhooks.recordDelivery(webhook, DeliveryStatus.DELIVERED);
    } catch (final RetryException exception) {
      if (Thread.currentThread().isInterrupted()) {
        // Shutdown interrupted the delivery; leave its previous status unchanged.
        return;
      }
      if (exception.getLastException() instanceof final Error error) {
        throw error;
      }
      LOGGER.error(
        "Giving up on webhook delivery {} to {} after {} attempts",
        deliveryId,
        webhook.url(),
        exception.getExceptions().size(),
        exception
      );
      this.webhooks.recordDelivery(webhook, DeliveryStatus.FAILED);
    }
  }

  private static boolean isRetryable(final Throwable failure) {
    if (failure instanceof final NonSuccessfulDelivery exception) {
      final HttpStatusCode status = exception.status();
      return status.is5xxServerError()
        || status.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
        || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS);
    }
    return true;
  }

  private byte[] createPayload(final FillEvent event) {
    try {
      return this.json.writeValueAsBytes(WebhookPayload.from(event));
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

  private static final class NonSuccessfulDelivery extends RuntimeException {
    private final HttpStatusCode status;

    private NonSuccessfulDelivery(final HttpStatusCode status) {
      super("Webhook target responded with status " + status.value());
      this.status = status;
    }

    private HttpStatusCode status() {
      return this.status;
    }
  }

}
