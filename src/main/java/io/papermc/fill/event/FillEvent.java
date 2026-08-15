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
package io.papermc.fill.event;

import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.Download;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface FillEvent permits FillEvent.ProjectEvent {
  String type();

  Instant time();

  /** An event concerning a project. */
  @NullMarked
  sealed interface ProjectEvent extends FillEvent permits FillEvent.VersionEvent, FillEvent.FamilyEvent {
    ProjectEntity project();
  }

  /** An event concerning a version (and therefore its project). */
  @NullMarked
  sealed interface VersionEvent extends ProjectEvent permits FillEvent.BuildPublished, FillEvent.BuildPromoted, FillEvent.VersionCreated, FillEvent.VersionUpdated {
    VersionEntity version();
  }

  /** An event concerning a version group (a family). */
  @NullMarked
  sealed interface FamilyEvent extends ProjectEvent permits FillEvent.FamilyCreated, FillEvent.FamilyUpdated, FillEvent.FamilyDeleted {
    FamilyEntity family();
  }

  @NullMarked
  record BuildPublished(
    Instant time,
    ProjectEntity project,
    VersionEntity version,
    BuildWithDownloads<Download> build
  ) implements VersionEvent {
    @Override
    public String type() {
      return "build.published";
    }
  }

  @NullMarked
  record BuildPromoted(
    Instant time,
    ProjectEntity project,
    VersionEntity version,
    BuildEntity build
  ) implements VersionEvent {
    @Override
    public String type() {
      return "build.promoted";
    }
  }

  @NullMarked
  record VersionCreated(
    Instant time,
    ProjectEntity project,
    VersionEntity version
  ) implements VersionEvent {
    @Override
    public String type() {
      return "version.created";
    }
  }

  @NullMarked
  record VersionUpdated(
    Instant time,
    ProjectEntity project,
    VersionEntity version
  ) implements VersionEvent {
    @Override
    public String type() {
      return "version.updated";
    }
  }

  @NullMarked
  record FamilyCreated(
    Instant time,
    ProjectEntity project,
    FamilyEntity family
  ) implements FamilyEvent {
    @Override
    public String type() {
      return "family.created";
    }
  }

  @NullMarked
  record FamilyUpdated(
    Instant time,
    ProjectEntity project,
    FamilyEntity family
  ) implements FamilyEvent {
    @Override
    public String type() {
      return "family.updated";
    }
  }

  @NullMarked
  record FamilyDeleted(
    Instant time,
    ProjectEntity project,
    FamilyEntity family
  ) implements FamilyEvent {
    @Override
    public String type() {
      return "family.deleted";
    }
  }
}
