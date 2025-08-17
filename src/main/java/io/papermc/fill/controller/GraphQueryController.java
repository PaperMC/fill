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
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.graphql.input.BuildFilters;
import io.papermc.fill.graphql.input.VersionFilters;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.DownloadWithUrl;
import io.papermc.fill.model.Java;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.SupportStatus;
import io.papermc.fill.model.Version;
import io.papermc.fill.service.StorageService;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@NullMarked
public class GraphQueryController {
  private final ProjectRepository projects;
  private final VersionRepository versions;
  private final BuildRepository builds;
  private final StorageService storage;

  @Autowired
  public GraphQueryController(
    final ProjectRepository projects,
    final VersionRepository versions,
    final BuildRepository builds,
    final StorageService storage
  ) {
    this.projects = projects;
    this.versions = versions;
    this.builds = builds;
    this.storage = storage;
  }

  @QueryMapping("projects")
  public List<ProjectEntity> getProjects() {
    return this.projects.findAll()
      .stream()
      .sorted(Project.COMPARATOR_NAME)
      .toList();
  }

  @QueryMapping("project")
  public Optional<ProjectEntity> getProject(
    @Argument
    final String id
  ) {
    return this.projects.findByName(id);
  }

  @SchemaMapping(typeName = "Project", field = "id")
  public String mapProjectId(final ProjectEntity project) {
    return project.name();
  }

  @SchemaMapping(typeName = "Project", field = "versions")
  public List<VersionEntity> mapProjectVersions(
    final ProjectEntity project,
    @Argument
    final @Nullable VersionFilters filterBy,
    @Argument
    final @Nullable Integer last
  ) {
    Stream<VersionEntity> versions = this.versions.findAllByProject(project);
    versions = versions.sorted(Version.COMPARATOR_CREATED_AT_REVERSE);
    if (filterBy != null) {
      final SupportStatus filterBySupportStatus = filterBy.supportStatus();
      if (filterBySupportStatus != null) {
        versions = versions.filter(Version.isSupportStatus(filterBySupportStatus));
      }
    }
    if (last != null) {
      versions = versions.limit(last);
    }
    return versions.toList();
  }

  @SchemaMapping(typeName = "Project", field = "version")
  public @Nullable VersionEntity mapProjectVersion(
    final ProjectEntity project,
    @Argument
    final String id
  ) {
    return this.versions.findByProjectAndName(project, id).orElse(null);
  }

  @SchemaMapping(typeName = "Family", field = "id")
  public String mapFamilyId(final FamilyEntity family) {
    return family.name();
  }

  @SchemaMapping(typeName = "Version", field = "id")
  public String mapVersionId(final VersionEntity version) {
    return version.name();
  }

  @SchemaMapping(typeName = "Version", field = "support")
  public Support mapVersionSupport(final VersionEntity version) {
    return version.support();
  }

  @SchemaMapping(typeName = "Version", field = "java")
  public Java mapVersionJava(final VersionEntity version) {
    return Objects.requireNonNullElse(version.java(), version.family().java());
  }

  @SchemaMapping(typeName = "Version", field = "builds")
  public List<BuildEntity> mapVersionBuilds(
    final VersionEntity version,
    @Argument
    final @Nullable BuildFilters filterBy,
    @Argument
    final @Nullable Integer last
  ) {
    Stream<BuildEntity> builds;
    if (last != null) {
      builds = this.builds.findAllByProjectAndVersion(version.project(), version, PageRequest.of(0, last, Sort.by(Sort.Direction.DESC, "_id")))
        .getContent()
        .stream();
    } else {
      builds = this.builds.findAllByProjectAndVersion(version.project(), version);
    }
    builds = builds.sorted(Build.COMPARATOR_ID_REVERSE);
    if (filterBy != null) {
      final BuildChannel filterByChannel = filterBy.channel();
      if (filterByChannel != null) {
        builds = builds.filter(Build.isChannel(filterByChannel));
      }
    }
    return builds.toList();
  }

  @SchemaMapping(typeName = "Build", field = "id")
  public int mapBuildId(final BuildEntity build) {
    return build.id();
  }

  @SchemaMapping(typeName = "Build", field = "time")
  public ZonedDateTime mapBuildTime(final BuildEntity build) {
    return build.createdAt().atZone(ZoneOffset.UTC);
  }

  @SchemaMapping(typeName = "Build", field = "channel")
  public BuildChannel mapBuildChannel(final BuildEntity build) {
    return build.channel();
  }

  @SchemaMapping(typeName = "Build", field = "downloads")
  public List<DownloadWithUrl> mapBuildDownloads(final BuildEntity build) {
    return build.downloads().values()
      .stream()
      .map(value -> value.withUrl(this.storage.getDownloadUrl(build, value)))
      .toList();
  }

  @SchemaMapping(typeName = "Build", field = "download")
  public @Nullable DownloadWithUrl mapBuildDownload(
    final BuildEntity build,
    @Argument
    final String name
  ) {
    final Download download = build.getDownloadByKey(name);
    return download != null
      ? download.withUrl(this.storage.getDownloadUrl(build, download))
      : null;
  }
}
