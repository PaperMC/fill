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
package io.papermc.fill.controller;

import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.FamilyRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.graphql.BuildFilters;
import io.papermc.fill.graphql.BuildOrder;
import io.papermc.fill.graphql.Connection;
import io.papermc.fill.graphql.VersionFilters;
import io.papermc.fill.graphql.VersionOrder;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.BuildWithDownloadsImpl;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.DownloadWithUrl;
import io.papermc.fill.model.Family;
import io.papermc.fill.model.Java;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.SupportStatus;
import io.papermc.fill.model.Version;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.Downloads;
import io.papermc.fill.util.graphql.CursorCodec;
import io.papermc.fill.util.graphql.CursorPaginator;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@NullMarked
public class GraphQueryController {
  private static final CursorPaginator<Instant, VersionEntity> VERSION_PAGINATOR = new CursorPaginator<>(
    "versions",
    VersionEntity::createdAt,
    CursorCodec.INSTANT,
    Comparator.naturalOrder()
  );
  private static final CursorPaginator<Integer, BuildWithDownloads<DownloadWithUrl>> BUILD_PAGINATOR = new CursorPaginator<>(
    "builds",
    Build::number,
    CursorCodec.INT,
    Comparator.naturalOrder()
  );

  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;

  private final StorageService storage;

  @Autowired
  public GraphQueryController(
    final ProjectRepository projects,
    final FamilyRepository families,
    final VersionRepository versions,
    final BuildRepository builds,
    final StorageService storage
  ) {
    this.projects = projects;
    this.families = families;
    this.versions = versions;
    this.builds = builds;
    this.storage = storage;
  }

  @QueryMapping("projects")
  public List<ProjectEntity> getProjects() {
    return this.projects.findAll()
      .stream()
      .sorted(Project.COMPARATOR_KEY)
      .toList();
  }

  @QueryMapping("project")
  public Optional<ProjectEntity> getProject(
    @Argument
    final String key
  ) {
    return this.projects.findByKey(key);
  }

  @SchemaMapping(typeName = "Project", field = "id")
  public String mapProjectId(final ProjectEntity project) {
    return project.id();
  }

  @SchemaMapping(typeName = "Project", field = "key")
  public String mapProjectKey(final ProjectEntity project) {
    return project.key();
  }

  @SchemaMapping(typeName = "Project", field = "name")
  public String mapProjectName(final ProjectEntity project) {
    return project.name();
  }

  @SchemaMapping(typeName = "Project", field = "families")
  public List<FamilyEntity> mapProjectFamilies(
    final ProjectEntity project
  ) {
    Stream<FamilyEntity> families = this.families.findAllByProject(project);
    families = families.sorted(Family.COMPARATOR_CREATED_AT_REVERSE);
    return families.toList();
  }

  @SchemaMapping(typeName = "Project", field = "family")
  public @Nullable FamilyEntity mapProjectFamily(
    final ProjectEntity project,
    @Argument
    final String key
  ) {
    return this.families.findByProjectAndKey(project, key).orElse(null);
  }

  @SchemaMapping(typeName = "Family", field = "id")
  public String mapFamilyId(final FamilyEntity family) {
    return family.id();
  }

  @SchemaMapping(typeName = "Family", field = "key")
  public String mapFamilyKey(final FamilyEntity family) {
    return family.key();
  }

  @SchemaMapping(typeName = "Family", field = "java")
  public Java mapFamilyJava(final FamilyEntity family) {
    return family.java();
  }

  @SchemaMapping(typeName = "Project", field = "versions")
  public Connection<VersionEntity> mapProjectVersions(
    final ProjectEntity project,
    @Argument
    final @Nullable VersionOrder orderBy,
    @Argument
    final @Nullable VersionFilters filterBy,
    @Argument
    final @Nullable String after,
    @Argument
    final @Nullable String before,
    @Argument
    final @Nullable Integer first,
    @Argument
    final @Nullable Integer last
  ) {
    Stream<VersionEntity> versions = this.versions.findAllByProject(project);
    if (filterBy != null) {
      final String filterByFamilyKey = filterBy.familyKey();
      if (filterByFamilyKey != null) {
        versions = versions.filter(version -> {
          final FamilyEntity family = this.families.findById(version.family()).orElseThrow(FamilyNotFoundException::new);
          return family.key().equals(filterByFamilyKey);
        });
      }
      final SupportStatus filterBySupportStatus = filterBy.supportStatus();
      if (filterBySupportStatus != null) {
        versions = versions.filter(Version.isSupportStatus(filterBySupportStatus));
      }
    }
    return VERSION_PAGINATOR.paginate(
      versions,
      orderBy != null ? orderBy.direction() : null,
      after,
      before,
      first,
      last
    );
  }

  @SchemaMapping(typeName = "Project", field = "version")
  public @Nullable VersionEntity mapProjectVersion(
    final ProjectEntity project,
    @Argument
    final String key
  ) {
    return this.versions.findByProjectAndKey(project, key).orElse(null);
  }

  @SchemaMapping(typeName = "Version", field = "id")
  public String mapVersionId(final VersionEntity version) {
    return version.id();
  }

  @SchemaMapping(typeName = "Version", field = "key")
  public String mapVersionKey(final VersionEntity version) {
    return version.key();
  }

  @SchemaMapping(typeName = "Version", field = "family")
  public FamilyEntity mapVersionFamily(final VersionEntity version) {
    return this.families.findById(version.family()).orElseThrow(FamilyNotFoundException::new);
  }

  @SchemaMapping(typeName = "Version", field = "support")
  public Support mapVersionSupport(final VersionEntity version) {
    return version.support();
  }

  @SchemaMapping(typeName = "Version", field = "java")
  public @Nullable Java mapVersionJava(final VersionEntity version) {
    return version.java();
  }

  @SchemaMapping(typeName = "Version", field = "builds")
  public Connection<BuildWithDownloads<DownloadWithUrl>> mapVersionBuilds(
    final VersionEntity version,
    @Argument
    final @Nullable BuildOrder orderBy,
    @Argument
    final @Nullable BuildFilters filterBy,
    @Argument
    final @Nullable String after,
    @Argument
    final @Nullable String before,
    @Argument
    final @Nullable Integer first,
    @Argument
    final @Nullable Integer last
  ) {
    final ProjectEntity project = this.projects.findById(version.project()).orElseThrow(ProjectNotFoundException::new);
    final Pageable pageable = last != null ? Pageable.ofSize(last) : Pageable.unpaged();
    final Stream<BuildEntity> builds;
    if (filterBy != null) {
      final List<BuildChannel> filterByChannels = filterBy.channels();
      builds = this.builds.findByVersionAndOptionalChannelIn(version, filterByChannels, pageable);
    } else {
      builds = this.builds.findAllByVersion(version, pageable);
    }
    return BUILD_PAGINATOR.paginate(
      builds.map(build -> new BuildWithDownloadsImpl<>(build, Downloads.map(build.downloads(), download -> {
        final URI url = this.storage.getDownloadUrl(project, version, build, download);
        return download.withUrl(url);
      }))),
      orderBy != null ? orderBy.direction() : null,
      after,
      before,
      first,
      last
    );
  }

  @SchemaMapping(typeName = "Build", field = "id")
  public String mapBuildId(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.id();
  }

  @SchemaMapping(typeName = "Build", field = "number")
  public int mapBuildNumber(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.number();
  }

  @SchemaMapping(typeName = "Build", field = "createdAt")
  public ZonedDateTime mapBuildCreatedAt(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.createdAt().atZone(ZoneOffset.UTC);
  }

  @SchemaMapping(typeName = "Build", field = "channel")
  public BuildChannel mapBuildChannel(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.channel();
  }

  @SchemaMapping(typeName = "Build", field = "commits")
  public List<Commit> mapBuildCommits(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.commits();
  }

  @SchemaMapping(typeName = "Build", field = "downloads")
  public Collection<DownloadWithUrl> mapBuildDownloads(final BuildWithDownloads<DownloadWithUrl> build) {
    return build.downloads().values();
  }

  @SchemaMapping(typeName = "Build", field = "download")
  public @Nullable DownloadWithUrl mapBuildDownload(
    final BuildWithDownloads<DownloadWithUrl> build,
    @Argument
    final String key
  ) {
    return build.getDownloadByKey(key);
  }
}
