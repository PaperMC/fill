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
package io.papermc.fill.model.request;

import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PublishRequest(
  UUID id,
  String project,
  String family,
  String version,
  int build,
  Instant time,
  BuildChannel channel,
  List<Commit> commits,
  Map<String, Download> downloads
) {
  @NullMarked
  public static final class Builder {
    private static final int BUILD_NOT_SET = Integer.MIN_VALUE;
    private @Nullable UUID id;
    private @Nullable String project;
    private @Nullable String family;
    private @Nullable String version;
    private int build = BUILD_NOT_SET;
    private @Nullable Instant time;
    private @Nullable BuildChannel channel;
    private final List<Commit> commits = new ArrayList<>();
    private final Map<String, Download> downloads = new HashMap<>();

    public Builder id(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder project(final String project) {
      this.project = project;
      return this;
    }

    public Builder family(final String family) {
      this.family = family;
      return this;
    }

    public Builder version(final String version) {
      this.version = version;
      return this;
    }

    public Builder build(final int build) {
      this.build = build;
      return this;
    }

    public Builder time(final Instant time) {
      this.time = time;
      return this;
    }

    public Builder channel(final BuildChannel channel) {
      this.channel = channel;
      return this;
    }

    public Builder addCommit(final Commit commit) {
      this.commits.add(commit);
      return this;
    }

    public Builder addDownload(final String key, final Download download) {
      this.downloads.put(key, download);
      return this;
    }

    public PublishRequest build() {
      if (this.id == null) throw new IllegalStateException("id must be set");
      if (this.project == null) throw new IllegalStateException("project must be set");
      if (this.family == null) throw new IllegalStateException("family must be set");
      if (this.version == null) throw new IllegalStateException("version must be set");
      if (this.build == BUILD_NOT_SET) throw new IllegalStateException("build must be set");
      if (this.time == null) throw new IllegalStateException("time must be set");
      if (this.channel == null) throw new IllegalStateException("channel must be set");
      return new PublishRequest(
        this.id,
        this.project,
        this.family,
        this.version,
        this.build,
        this.time,
        this.channel,
        List.copyOf(this.commits),
        Map.copyOf(this.downloads)
      );
    }
  }
}
