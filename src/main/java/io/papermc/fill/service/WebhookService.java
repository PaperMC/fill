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
package io.papermc.fill.service;

import io.papermc.fill.database.WebhookEntity;
import io.papermc.fill.database.WebhookRepository;
import io.papermc.fill.exception.WebhookNotFoundException;
import io.papermc.fill.model.Timestamped;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class WebhookService {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebhookService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final WebhookRepository webhooks;
  private final Clock clock;

  @Autowired
  public WebhookService(final WebhookRepository webhooks, final Clock clock) {
    this.webhooks = webhooks;
    this.clock = clock;
  }

  public WebhookEntity create(final String url) {
    final Instant createdAt = this.clock.instant();
    final WebhookEntity entity = WebhookEntity.create(
      new ObjectId(Date.from(createdAt)),
      createdAt,
      url,
      generateSecret()
    );
    return this.webhooks.save(entity);
  }

  public boolean delete(final String id) {
    if (!ObjectId.isValid(id)) {
      throw new WebhookNotFoundException();
    }
    final ObjectId objectId = new ObjectId(id);
    if (!this.webhooks.existsById(objectId)) {
      throw new WebhookNotFoundException();
    }
    this.webhooks.deleteById(objectId);
    return true;
  }

  public List<WebhookEntity> list() {
    return this.webhooks.findAll()
      .stream()
      .sorted(Timestamped.CREATED_AT_ASC)
      .toList();
  }

  public void recordDelivery(final WebhookEntity webhook, final String status) {
    try {
      this.webhooks.updateDelivery(webhook._id(), status, this.clock.instant());
    } catch (final Exception exception) {
      LOGGER.warn("Failed to record delivery status for webhook {}", webhook.id(), exception);
    }
  }

  private static String generateSecret() {
    final byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return "whsec_" + Base64.getEncoder().encodeToString(bytes);
  }
}
