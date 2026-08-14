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
package io.papermc.fill.database;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "webhooks")
@NullMarked
public class WebhookEntity extends AbstractEntity {
  private String url;
  // Stored in plaintext so deliveries can be signed; anyone with DB access can forge signatures.
  private String secret;
  private @Nullable String lastDeliveryStatus;
  private @Nullable Instant lastDeliveryAt;

  public WebhookEntity() {
  }

  public static WebhookEntity create(final ObjectId _id, final Instant createdAt, final String url, final String secret) {
    final WebhookEntity entity = new WebhookEntity();
    entity._id = _id;
    entity.createdAt = createdAt;
    entity.url = url;
    entity.secret = secret;
    return entity;
  }

  public String id() {
    return this._id.toHexString();
  }

  public String url() {
    return this.url;
  }

  public String secret() {
    return this.secret;
  }

  public @Nullable String lastDeliveryStatus() {
    return this.lastDeliveryStatus;
  }

  public @Nullable Instant lastDeliveryAt() {
    return this.lastDeliveryAt;
  }

}
