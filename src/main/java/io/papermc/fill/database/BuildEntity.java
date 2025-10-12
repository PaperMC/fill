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

import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.LegacyBuildChannel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndex(def = "{'project': 1}")
@CompoundIndex(def = "{'version': 1}")
@CompoundIndex(def = "{'version': 1, 'number': 1}", unique = true)
@CompoundIndex(def = "{'version': 1, 'number': -1}")
@CompoundIndex(def = "{'version': 1, 'channel': 1, 'number': -1}")
@Document(collection = "builds")
@NullMarked
public class BuildEntity extends AbstractEntity implements BuildWithDownloads<Download> {
  private ObjectId project;
  private ObjectId version;
  private int number;
  private BuildChannel channel;
  @Deprecated
  private @Nullable LegacyBuildChannel channelO;
  private List<Commit> commits;
  private Map<String, Download> downloads;

  public BuildEntity() {
  }

  public static BuildEntity create(
    final ObjectId _id,
    final Instant createdAt,
    final ProjectEntity project,
    final VersionEntity version,
    final int number,
    final BuildChannel channel,
    final List<Commit> commits,
    final Map<String, Download> downloads
  ) {
    final BuildEntity entity = new BuildEntity();
    entity._id = _id;
    entity.createdAt = createdAt;
    entity.project = project._id();
    entity.version = version._id();
    entity.number = number;
    entity.channel = channel;
    entity.commits = commits;
    entity.downloads = downloads;
    return entity;
  }

  public ObjectId project() {
    return this.project;
  }

  public ObjectId version() {
    return this.version;
  }

  @Override
  public String id() {
    return this._id.toHexString();
  }

  @Override
  public int number() {
    return this.number;
  }

  @Override
  public BuildChannel channel() {
    return this.channel;
  }

  public void setChannel(final BuildChannel channel) {
    this.channel = channel;
  }

  @Deprecated
  public @Nullable LegacyBuildChannel channelO() {
    return this.channelO;
  }

  @Override
  public List<Commit> commits() {
    return this.commits;
  }

  @Override
  public Map<String, Download> downloads() {
    return this.downloads;
  }
}
