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

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.SharedConstants;
import io.papermc.fill.configuration.properties.ApplicationApiProperties;
import io.papermc.fill.database.AbstractEntity;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.FamilyRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Family;
import io.papermc.fill.model.Keyed;
import io.papermc.fill.model.Version;
import io.papermc.fill.model.response.v2.BuildResponse;
import io.papermc.fill.model.response.v2.BuildsResponse;
import io.papermc.fill.model.response.v2.FamilyBuildsResponse;
import io.papermc.fill.model.response.v2.FamilyResponse;
import io.papermc.fill.model.response.v2.LegacyChange;
import io.papermc.fill.model.response.v2.LegacyDownload;
import io.papermc.fill.model.response.v2.ProjectResponse;
import io.papermc.fill.model.response.v2.ProjectsResponse;
import io.papermc.fill.model.response.v2.VersionResponse;
import io.papermc.fill.util.http.Caching;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Deprecated
@NullMarked
@RestController
@Tag(name = "Meta v2")
public class Meta2Controller {
  private static final Duration CACHE_LENGTH_PROJECTS = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_PROJECT = Duration.ofMinutes(30);
  private static final Duration CACHE_LENGTH_FAMILY = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_FAMILY_BUILDS = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_VERSION = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_VERSION_BUILDS = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_BUILD = Duration.ofDays(7);

  private final ApplicationApiProperties properties;

  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;

  @Autowired
  public Meta2Controller(
    final ApplicationApiProperties properties,
    final ProjectRepository projects,
    final FamilyRepository families,
    final VersionRepository versions,
    final BuildRepository builds
  ) {
    this.properties = properties;
    this.projects = projects;
    this.families = families;
    this.versions = versions;
    this.builds = builds;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects")
  public ResponseEntity<?> getProjects() {
    final List<ProjectEntity> projects = this.projects.findAll();
    final ProjectsResponse response = new ProjectsResponse(Keyed.keysOf(projects));
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECTS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}")
  public ResponseEntity<?> getProject(
    @PathVariable("project")
    final String projectKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final List<FamilyEntity> families = this.families.findAllByProject(project)
      .filter(family -> family.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Family.COMPARATOR_CREATED_AT)
      .toList();
    final List<VersionEntity> versions = this.versions.findAllByProject(project)
      .filter(version -> version.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Version.COMPARATOR_CREATED_AT)
      .toList();
    final ProjectResponse response = new ProjectResponse(
      project.key(),
      project.name(),
      Keyed.keysOf(families),
      Keyed.keysOf(versions)
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECT));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/version_group/{family:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  public ResponseEntity<?> getFamily(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("family")
    final String familyKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final FamilyEntity family = this.families.findByProjectAndKey(project, familyKey).orElseThrow(FamilyNotFoundException::new);
    if (family.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new FamilyNotFoundException();
    }
    final List<VersionEntity> versions = this.versions.findAllByFamily(family)
      .filter(version -> version.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Version.COMPARATOR_CREATED_AT)
      .toList();
    final FamilyResponse response = new FamilyResponse(
      project.key(),
      project.name(),
      family.key(),
      Keyed.keysOf(versions)
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_FAMILY));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/version_group/{family:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds")
  public ResponseEntity<?> getFamilyBuilds(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("family")
    final String familyKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final FamilyEntity family = this.families.findByProjectAndKey(project, familyKey).orElseThrow(FamilyNotFoundException::new);
    if (family.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new FamilyNotFoundException();
    }
    final List<VersionEntity> versions = this.versions.findAllByFamily(family)
      .filter(version -> version.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Version.COMPARATOR_CREATED_AT)
      .toList();
    final Map<ObjectId, VersionEntity> versionsById = versions.stream()
      .collect(Collectors.toMap(AbstractEntity::_id, Function.identity()));
    final List<BuildEntity> builds = this.builds.findAllByVersionIn(versionsById.keySet())
      .filter(build -> build.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final FamilyBuildsResponse response = new FamilyBuildsResponse(
      project.key(),
      project.name(),
      family.key(),
      Keyed.keysOf(versions),
      builds.stream().map(build -> new FamilyBuildsResponse.Build(
        versionsById.get(build.version()).key(),
        build.number(),
        build.createdAt(),
        build.channel(),
        isPromoted(build),
        toChanges(build.commits()),
        this.toDownloads(project.key(), build.downloads())
      )).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_FAMILY_BUILDS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  public ResponseEntity<?> getVersion(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("version")
    final String versionKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    if (version.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new VersionNotFoundException();
    }
    final List<BuildEntity> builds = this.builds.findAllByVersion(version)
      .filter(build -> build.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final VersionResponse response = new VersionResponse(
      project.key(),
      project.name(),
      version.key(),
      builds.stream().map(BuildEntity::number).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSION));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds")
  public ResponseEntity<?> getVersionBuilds(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("version")
    final String versionKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    if (version.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new VersionNotFoundException();
    }
    final List<BuildEntity> builds = this.builds.findAllByVersion(version)
      .filter(build -> build.createdAt().isBefore(SharedConstants.API_V2_CUTOFF))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final BuildsResponse response = new BuildsResponse(
      project.key(),
      project.name(),
      version.key(),
      builds.stream().map(build -> new BuildsResponse.Build(
        build.number(),
        build.createdAt(),
        build.channel(),
        isPromoted(build),
        toChanges(build.commits()),
        this.toDownloads(project.key(), build.downloads())
      )).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSION_BUILDS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds/{build:\\d+}")
  public ResponseEntity<?> getVersionBuild(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("version")
    final String versionKey,
    @PathVariable("build")
    @PositiveOrZero
    final int buildNumber
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    if (version.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new VersionNotFoundException();
    }
    final BuildEntity build = this.builds.findByVersionAndNumber(version, buildNumber).orElseThrow(BuildNotFoundException::new);
    if (build.createdAt().isAfter(SharedConstants.API_V2_CUTOFF)) {
      throw new BuildNotFoundException();
    }
    final BuildResponse response = new BuildResponse(
      project.key(),
      project.name(),
      version.key(),
      build.number(),
      build.createdAt(),
      build.channel(),
      isPromoted(build),
      toChanges(build.commits()),
      this.toDownloads(project.key(), build.downloads())
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILD));
  }

  private static boolean isPromoted(final Build build) {
    return build.channel() == BuildChannel.RECOMMENDED;
  }

  private static List<LegacyChange> toChanges(final List<Commit> commits) {
    return commits.stream()
      .map(commit -> new LegacyChange(commit.sha(), commit.summary(), commit.message()))
      .toList();
  }

  private Map<String, LegacyDownload> toDownloads(final String project, final Map<String, Download> downloads) {
    final Map<String, String> legacyDownloadKeyMappings = this.properties.legacyDownloadKeyMappings().getOrDefault(project, List.of())
      .stream()
      .collect(Collectors.toMap(ApplicationApiProperties.LegacyDownloadKeyMapping::from, ApplicationApiProperties.LegacyDownloadKeyMapping::to));
    return toDownloads(downloads, legacyDownloadKeyMappings);
  }

  @VisibleForTesting
  static Map<String, LegacyDownload> toDownloads(final Map<String, Download> downloads, final Map<String, String> legacyDownloadKeyMappings) {
    return downloads.entrySet()
      .stream()
      .map(entry -> {
        final String key = entry.getKey();
        final Download value = entry.getValue();
        return Map.entry(
          legacyDownloadKeyMappings.getOrDefault(key, key),
          new LegacyDownload(value.name(), value.checksums().sha256())
        );
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
