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
import io.papermc.fill.model.GitRepository;
import io.papermc.fill.model.DiscordNotificationChannel;
import io.papermc.fill.model.Project;
import java.net.URI;
import java.util.List;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndex(def = "{'name': 1}", unique = true)
@Document(collection = "projects")
@NullMarked
public class ProjectEntity extends AbstractEntity implements Project {
  private String name;
  private String displayName;
  private GitRepository gitRepository;
  private URI logoUrl;
  private List<DiscordNotificationChannel> discordNotificationChannels;
  private String discordNotificationDownloadKey;

  public ProjectEntity() {
  }

  @VisibleForTesting
  public static ProjectEntity create(
    final ObjectId _id,
    final String name,
    final String displayName,
    final GitRepository gitRepository,
    final URI logoUrl,
    final List<DiscordNotificationChannel> discordNotificationChannels,
    final String discordNotificationDownloadKey
  ) {
    final ProjectEntity entity = new ProjectEntity();
    entity._id = _id;
    entity.name = name;
    entity.displayName = displayName;
    entity.gitRepository = gitRepository;
    entity.logoUrl = logoUrl;
    entity.discordNotificationChannels = discordNotificationChannels;
    entity.discordNotificationDownloadKey = discordNotificationDownloadKey;
    return entity;
  }

  @Override
  public String name() {
    return this.name;
  }

  public String displayName() {
    return this.displayName;
  }

  public GitRepository gitRepository() {
    return this.gitRepository;
  }

  public URI logoUrl() {
    return this.logoUrl;
  }

  public List<DiscordNotificationChannel> discordNotificationChannels() {
    return this.discordNotificationChannels;
  }

  public String discordNotificationDownloadKey() {
    return this.discordNotificationDownloadKey;
  }
}
