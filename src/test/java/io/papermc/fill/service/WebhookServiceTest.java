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
import io.papermc.fill.model.DeliveryStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class WebhookServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

  @Test
  void createsStandardWebhookSecret() {
    final WebhookRepository repository = mock(WebhookRepository.class);
    when(repository.save(any(WebhookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    final WebhookService service = new WebhookService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    final WebhookEntity webhook = service.create("https://example.com/webhook");

    assertTrue(webhook.secret().startsWith("whsec_"));
    assertEquals(32, Base64.getDecoder().decode(webhook.secret().substring("whsec_".length())).length);
  }

  @Test
  void recordsDeliveryWithAnAtomicUpdate() {
    final WebhookRepository repository = mock(WebhookRepository.class);
    final WebhookService service = new WebhookService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    final WebhookEntity webhook = WebhookEntity.create(
      new org.bson.types.ObjectId("000000000000000000000001"),
      NOW,
      "https://example.com/webhook",
      "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    );

    service.recordDelivery(webhook, DeliveryStatus.DELIVERED);

    verify(repository).updateDelivery(webhook._id(), DeliveryStatus.DELIVERED, NOW);
  }
}
