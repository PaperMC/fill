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

import io.papermc.fill.event.FillEvent;
import io.papermc.fill.model.BuildChannel;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

/** The payload delivered for a {@link FillEvent}. */
@NullMarked
public record WebhookPayload(String type, Instant timestamp, Data data) {
  @NullMarked
  public sealed interface Data permits Data.BuildPublished, Data.BuildPromoted, Data.VersionCreated, Data.VersionUpdated, Data.FamilyCreated, Data.FamilyUpdated, Data.FamilyDeleted {
    @NullMarked
    record BuildPublished(
      ProjectRef project,
      VersionRef version,
      BuildRef build
    ) implements Data {
    }

    @NullMarked
    record BuildPromoted(
      ProjectRef project,
      VersionRef version,
      BuildRef build
    ) implements Data {
    }

    @NullMarked
    record VersionCreated(
      ProjectRef project,
      VersionRef version
    ) implements Data {
    }

    @NullMarked
    record VersionUpdated(
      ProjectRef project,
      VersionRef version
    ) implements Data {
    }

    @NullMarked
    record FamilyCreated(
      ProjectRef project,
      FamilyRef family
    ) implements Data {
    }

    @NullMarked
    record FamilyUpdated(
      ProjectRef project,
      FamilyRef family
    ) implements Data {
    }

    @NullMarked
    record FamilyDeleted(
      ProjectRef project,
      FamilyRef family
    ) implements Data {
    }
  }

  @NullMarked
  public record ProjectRef(
    String id,
    String key
  ) {
  }

  @NullMarked
  public record VersionRef(
    String id,
    String key
  ) {
  }

  @NullMarked
  public record FamilyRef(
    String id,
    String key
  ) {
  }

  @NullMarked
  public record BuildRef(
    String id,
    int number,
    BuildChannel channel
  ) {
  }

  public static WebhookPayload from(final FillEvent event) {
    final Data data = switch (event) {
      case final FillEvent.BuildPublished e -> new Data.BuildPublished(
        project(e),
        version(e),
        new BuildRef(e.build().id(), e.build().number(), e.build().channel())
      );
      case final FillEvent.BuildPromoted e -> new Data.BuildPromoted(
        project(e),
        version(e),
        new BuildRef(e.build().id(), e.build().number(), e.build().channel())
      );
      case final FillEvent.VersionCreated e -> new Data.VersionCreated(project(e), version(e));
      case final FillEvent.VersionUpdated e -> new Data.VersionUpdated(project(e), version(e));
      case final FillEvent.FamilyCreated e -> new Data.FamilyCreated(project(e), family(e));
      case final FillEvent.FamilyUpdated e -> new Data.FamilyUpdated(project(e), family(e));
      case final FillEvent.FamilyDeleted e -> new Data.FamilyDeleted(project(e), family(e));
    };
    return new WebhookPayload(event.type(), event.time(), data);
  }

  private static ProjectRef project(final FillEvent.ProjectEvent event) {
    return new ProjectRef(event.project().id(), event.project().key());
  }

  private static VersionRef version(final FillEvent.VersionEvent event) {
    return new VersionRef(event.version().id(), event.version().key());
  }

  private static FamilyRef family(final FillEvent.FamilyEvent event) {
    return new FamilyRef(event.family().id(), event.family().key());
  }
}
