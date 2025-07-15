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

import io.papermc.fill.model.Timestamped;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.MongoId;

@NullMarked
public abstract class AbstractEntity implements Timestamped {
  @MongoId
  protected ObjectId _id;
  @CreatedDate
  protected Instant createdAt;
  @LastModifiedDate
  protected Instant updatedAt;

  public final ObjectId _id() {
    return this._id;
  }

  @Override
  public final Instant createdAt() {
    return this.createdAt;
  }

  @Override
  public final Instant updatedAt() {
    return this.updatedAt;
  }
}
