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

import io.papermc.fill.model.Family;
import io.papermc.fill.model.Java;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@CompoundIndex(def = "{'project': 1}")
@CompoundIndex(def = "{'project': 1, 'name': 1}", unique = true)
@Document(collection = "families")
@NullMarked
public class FamilyEntity extends AbstractEntity implements Family {
  @DocumentReference
  private ProjectEntity project;
  private String name;
  private Java java;

  public FamilyEntity() {
  }

  public static FamilyEntity create(
    final ObjectId _id,
    final ProjectEntity project,
    final String name,
    final Java java
  ) {
    final FamilyEntity entity = new FamilyEntity();
    entity._id = _id;
    entity.project = project;
    entity.name = name;
    entity.java = java;
    return entity;
  }

  public ProjectEntity project() {
    return this.project;
  }

  @Override
  public String name() {
    return this.name;
  }

  @Override
  public Java java() {
    return this.java;
  }
}
