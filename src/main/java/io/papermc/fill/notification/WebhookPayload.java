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

  public sealed interface Data permits Data.BuildPublished, Data.BuildPromoted, Data.VersionCreated, Data.VersionUpdated {
    record BuildPublished(ProjectRef project, VersionRef version, BuildRef build) implements Data {
    }

    record BuildPromoted(ProjectRef project, VersionRef version, BuildRef build) implements Data {
    }

    record VersionCreated(ProjectRef project, VersionRef version) implements Data {
    }

    record VersionUpdated(ProjectRef project, VersionRef version) implements Data {
    }
  }

  public record ProjectRef(String id, String key) {
  }

  public record VersionRef(String id, String key) {
  }

  public record BuildRef(String id, int number, BuildChannel channel) {
  }

  public static WebhookPayload from(final FillEvent event) {
    final ProjectRef project = new ProjectRef(event.project().id(), event.project().key());
    final VersionRef version = new VersionRef(event.version().id(), event.version().key());
    final Data data = switch (event) {
      case FillEvent.BuildPublished e -> new Data.BuildPublished(
        project,
        version,
        new BuildRef(e.build().id(), e.build().number(), e.build().channel())
      );
      case FillEvent.BuildPromoted e -> new Data.BuildPromoted(
        project,
        version,
        new BuildRef(e.build().id(), e.build().number(), e.build().channel())
      );
      case FillEvent.VersionCreated _ -> new Data.VersionCreated(project, version);
      case FillEvent.VersionUpdated _ -> new Data.VersionUpdated(project, version);
    };
    return new WebhookPayload(event.type(), event.time(), data);
  }
}
