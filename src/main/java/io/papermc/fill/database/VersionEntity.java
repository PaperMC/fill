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

import io.papermc.fill.model.GitRepository;
import io.papermc.fill.model.Java;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.Version;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@CompoundIndex(def = "{'project': 1}")
@CompoundIndex(def = "{'project': 1, 'family': 1}")
@CompoundIndex(def = "{'project': 1, 'name': 1}", unique = true)
@Document(collection = "versions")
@NullMarked
public class VersionEntity extends AbstractEntity implements Version {
  @DocumentReference
  private ProjectEntity project;
  @DocumentReference
  private FamilyEntity family;
  private String name;
  private @Nullable GitRepository gitRepository;
  private Support support;
  private @Nullable Java java;
  @DocumentReference
  private @Nullable BuildEntity promotedBuild;

  public VersionEntity() {
  }

  public static VersionEntity create(
    final ObjectId _id,
    final Instant createdAt,
    final ProjectEntity project,
    final FamilyEntity family,
    final String name,
    final @Nullable GitRepository gitRepository,
    final Support support,
    final @Nullable Java java
  ) {
    final VersionEntity entity = new VersionEntity();
    entity._id = _id;
    entity.createdAt = createdAt;
    entity.project = project;
    entity.family = family;
    entity.name = name;
    entity.gitRepository = gitRepository;
    entity.support = support;
    entity.java = java;
    return entity;
  }

  public ProjectEntity project() {
    return this.project;
  }

  public FamilyEntity family() {
    return this.family;
  }

  @Override
  public String name() {
    return this.name;
  }

  public @Nullable GitRepository gitRepository() {
    return this.gitRepository;
  }

  @Override
  public Support support() {
    return this.support;
  }

  @Override
  public @Nullable Java java() {
    return this.java;
  }

  public @Nullable BuildEntity promotedBuild() {
    return this.promotedBuild;
  }

  public void setPromotedBuild(final @Nullable BuildEntity promotedBuild) {
    this.promotedBuild = promotedBuild;
  }
}
