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
package io.papermc.fill.discord;

import discord4j.common.util.TimestampFormat;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Section;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.Thumbnail;
import discord4j.core.object.component.UnfurledMediaItem;
import discord4j.core.object.emoji.CustomEmoji;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.AllowedMentions;
import io.papermc.fill.configuration.properties.ApplicationDiscordProperties;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.service.DiscordService;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.BuildPublishListener;
import io.papermc.fill.util.discord.Components;
import io.papermc.fill.util.discord.DiscordNotificationChannel;
import io.papermc.fill.util.git.GitRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("app.discord.token")
@NullMarked
public class DiscordNotifier implements BuildPublishListener {
  private final ApplicationDiscordProperties properties;
  private final BuildRepository builds;
  private final StorageService storage;
  private final DiscordService discord;

  @Autowired
  public DiscordNotifier(
    final ApplicationDiscordProperties properties,
    final BuildRepository builds,
    final StorageService storage,
    final DiscordService discord
  ) {
    this.properties = properties;
    this.builds = builds;
    this.storage = storage;
    this.discord = discord;
  }

  @Override
  public void onBuildPublished(final ProjectEntity project, final VersionEntity version, final BuildEntity build) {
    final GitRepository repository = Objects.requireNonNullElse(version.gitRepository(), project.gitRepository());

    final Container content = this.createContent(project, version, repository, build);

    for (final DiscordNotificationChannel channel : project.discordNotificationChannels()) {
      final MessageCreateSpec message = MessageCreateSpec.builder()
        .addFlag(Message.Flag.IS_COMPONENTS_V2)
        .addComponent(content)
        .addComponent(this.createButtons(project, version, repository, build, channel.includeGitCompare()))
        .allowedMentions(AllowedMentions.suppressEveryone())
        .build();
      this.discord.createMessage(channel.snowflake(), message).subscribe();
    }
  }

  private Container createContent(final ProjectEntity project, final VersionEntity version, final GitRepository repository, final BuildEntity build) {
    return Components.container(OptionalInt.empty(), container -> {
      container.add(
        Section.of(
          Thumbnail.of(UnfurledMediaItem.of(project.logoUrl().toString())),
          List.of(
            TextDisplay.of(String.format(
              "# Build %d for %s %s",
              build.id(),
              project.displayName(),
              version.name()
            )),
            TextDisplay.of(String.format(
              "**Channel**: %s",
              switch (build.channel()) {
                case ALPHA -> "Alpha";
                case BETA -> "Beta";
                case STABLE -> "Stable";
                case RECOMMENDED -> "Recommended";
              }
            )),
            TextDisplay.of(String.format(
              "**Published**: %s (%s)",
              TimestampFormat.SHORT_DATE_TIME.format(build.createdAt()),
              TimestampFormat.RELATIVE_TIME.format(build.createdAt())
            ))
          )
        )
      );
      container.add(Separator.of());
      container.add(TextDisplay.of(
        build.commits().stream()
          .map(commit -> String.format(
            "- %s: %s",
            String.format(
              "[%s](https://github.com/%s/%s/commit/%s)",
              Commit.getShortSha(commit),
              repository.owner(),
              repository.name(),
              commit.sha()
            ),
            commit.summary()
          )).collect(Collectors.joining("\n"))
      ));
    }, switch (build.channel()) {
      case ALPHA -> Components.COLOR_RED;
      case BETA -> Components.COLOR_YELLOW;
      case STABLE -> Components.COLOR_BLUE;
      case RECOMMENDED -> Components.COLOR_GREEN;
    }, false);
  }

  private ActionRow createButtons(final ProjectEntity project, final VersionEntity version, final GitRepository repository, final BuildEntity build, final boolean includeGitCompare) {
    final List<ActionComponent> row0 = new ArrayList<>();

    final Download download = build.getDownloadByKey(project.discordNotificationDownloadKey());
    if (download != null) {
      final URI url = this.storage.getDownloadUrl(project, version, build, download);
      row0.add(Button.link(url.toString(), createEmoji(this.properties.emojis().download()), "Download"));
    }

    if (includeGitCompare) {
      final List<BuildEntity> builds = this.builds.findAllByProjectAndVersion(project, version)
        .sorted(Build.COMPARATOR_ID)
        .toList();
      final BuildEntity buildBefore = getBuildBefore(builds);
      if (buildBefore != null) {
        final String url = String.format(
          "https://diffs.dev/?github_url=https://github.com/%s/%s/compare/%s..%s",
          repository.owner(),
          repository.name(),
          buildBefore.commits().getFirst().sha(),
          build.commits().getFirst().sha()
        );
        row0.add(Button.link(url, createEmoji(this.properties.emojis().gitCompare()), "GitHub Diff"));
      }
    }

    return ActionRow.of(row0);
  }

  // TODO: improve this logic
  private static @Nullable BuildEntity getBuildBefore(final List<BuildEntity> builds) {
    return builds.size() >= 2 ? builds.get(builds.size() - 2) : null;
  }

  private static Emoji createEmoji(final ApplicationDiscordProperties.Emojis.Emoji emoji) {
    return CustomEmoji.of(emoji.id(), emoji.name(), false);
  }
}
