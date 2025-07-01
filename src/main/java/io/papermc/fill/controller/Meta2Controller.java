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
import io.papermc.fill.configuration.properties.ApplicationApiProperties;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.FamilyRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.NoSuchBuildException;
import io.papermc.fill.exception.NoSuchFamilyException;
import io.papermc.fill.exception.NoSuchProjectException;
import io.papermc.fill.exception.NoSuchVersionException;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Family;
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
import java.util.stream.Collectors;
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
    final ProjectsResponse response = new ProjectsResponse(projects.stream().map(ProjectEntity::name).toList());
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECTS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}")
  public ResponseEntity<?> getProject(
    @PathVariable
    final String project
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final List<FamilyEntity> families = this.families.findAllByProject(pe);
    final List<VersionEntity> versions = this.versions.findAllByProject(pe).toList();
    final ProjectResponse response = new ProjectResponse(
      pe.name(),
      pe.displayName(),
      families.stream().sorted(Family.COMPARATOR_CREATED_AT).map(FamilyEntity::name).toList(),
      versions.stream().sorted(Version.COMPARATOR_CREATED_AT).map(VersionEntity::name).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECT));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/version_group/{family:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  public ResponseEntity<?> getFamily(
    @PathVariable
    final String project,
    @PathVariable
    final String family
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final FamilyEntity fe = this.families.findByProjectAndName(pe, family).orElseThrow(NoSuchVersionException::new);
    final List<VersionEntity> versions = this.versions.findAllByProjectAndFamily(pe, fe).toList();
    final FamilyResponse response = new FamilyResponse(
      pe.name(),
      pe.displayName(),
      fe.name(),
      versions.stream().sorted(Version.COMPARATOR_CREATED_AT).map(VersionEntity::name).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_FAMILY));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/version_group/{family:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds")
  public ResponseEntity<?> getFamilyBuilds(
    @PathVariable
    final String project,
    @PathVariable
    final String family
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final FamilyEntity fe = this.families.findByProjectAndName(pe, family).orElseThrow(NoSuchFamilyException::new);
    final List<VersionEntity> versions = this.versions.findAllByProjectAndFamily(pe, fe).toList();
    final List<BuildEntity> builds = this.builds.findAllByProjectAndVersionIn(pe, versions)
      .filter(build -> isAllowed(build))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final FamilyBuildsResponse response = new FamilyBuildsResponse(
      pe.name(),
      pe.displayName(),
      fe.name(),
      versions.stream().sorted(Version.COMPARATOR_CREATED_AT).map(VersionEntity::name).toList(),
      builds.stream().map(be -> new FamilyBuildsResponse.Build(
        be.version().name(),
        be.number(),
        be.createdAt(),
        be.channel(),
        BuildEntity.isPromoted(be),
        toChanges(be.commits()),
        this.toDownloads(pe.name(), be.downloads())
      )).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_FAMILY_BUILDS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  public ResponseEntity<?> getVersion(
    @PathVariable
    final String project,
    @PathVariable
    final String version
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final List<BuildEntity> builds = this.builds.findAllByProjectAndVersion(pe, ve)
      .filter(build -> isAllowed(build))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final VersionResponse response = new VersionResponse(
      pe.name(),
      pe.displayName(),
      ve.name(),
      builds.stream().map(BuildEntity::number).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSION));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds")
  public ResponseEntity<?> getVersionBuilds(
    @PathVariable
    final String project,
    @PathVariable
    final String version
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final List<BuildEntity> builds = this.builds.findAllByProjectAndVersion(pe, ve)
      .filter(build -> isAllowed(build))
      .sorted(Build.COMPARATOR_NUMBER)
      .toList();
    final BuildsResponse response = new BuildsResponse(
      pe.name(),
      pe.displayName(),
      ve.name(),
      builds.stream().map(be -> new BuildsResponse.Build(
        be.number(),
        be.createdAt(),
        be.channel(),
        BuildEntity.isPromoted(be),
        toChanges(be.commits()),
        this.toDownloads(pe.name(), be.downloads())
      )).toList()
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSION_BUILDS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds/{build:\\d+}")
  public ResponseEntity<?> getVersionBuild(
    @PathVariable
    final String project,
    @PathVariable
    final String version,
    @PathVariable
    @PositiveOrZero
    final int build
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final BuildEntity be = this.builds.findByProjectAndVersionAndNumber(pe, ve, build).orElseThrow(NoSuchBuildException::new);
    if (!isAllowed(be)) {
      throw new NoSuchBuildException();
    }
    final BuildResponse response = new BuildResponse(
      pe.name(),
      pe.displayName(),
      ve.name(),
      be.number(),
      be.createdAt(),
      be.channel(),
      BuildEntity.isPromoted(be),
      toChanges(be.commits()),
      this.toDownloads(pe.name(), be.downloads())
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILD));
  }

  private static boolean isAllowed(final BuildEntity build) {
    return true;
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
