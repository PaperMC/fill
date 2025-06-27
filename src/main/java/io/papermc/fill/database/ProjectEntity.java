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

import com.google.common.annotations.VisibleForTesting;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndex(def = "{'name': 1}", unique = true)
@Document(collection = "projects")
@NullMarked
public class ProjectEntity extends AbstractEntity {
  private String name;
  private String displayName;

  public ProjectEntity() {
  }

  @VisibleForTesting
  public static ProjectEntity create(
    final ObjectId _id,
    final String name,
    final String displayName
  ) {
    final ProjectEntity entity = new ProjectEntity();
    entity._id = _id;
    entity.name = name;
    entity.displayName = displayName;
    return entity;
  }

  public String name() {
    return this.name;
  }

  public String displayName() {
    return this.displayName;
  }
}
