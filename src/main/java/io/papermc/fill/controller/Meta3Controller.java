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

import com.google.common.collect.Lists;
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
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.DownloadWithUrl;
import io.papermc.fill.model.Family;
import io.papermc.fill.model.Keyed;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Version;
import io.papermc.fill.model.response.ErrorResponse;
import io.papermc.fill.model.response.v3.BuildResponse;
import io.papermc.fill.model.response.v3.ProjectResponse;
import io.papermc.fill.model.response.v3.ProjectsResponse;
import io.papermc.fill.model.response.v3.VersionResponse;
import io.papermc.fill.model.response.v3.VersionsResponse;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.http.Caching;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
@Tag(name = "Meta v3")
public class Meta3Controller {
  private static final Duration CACHE_LENGTH_PROJECTS = Duration.ofHours(1);
  private static final Duration CACHE_LENGTH_PROJECT = Duration.ofMinutes(30);
  private static final Duration CACHE_LENGTH_VERSIONS = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_VERSION = Duration.ofHours(1);
  private static final Duration CACHE_LENGTH_BUILDS = Duration.ofMinutes(5);
  private static final Duration CACHE_LENGTH_BUILD = Duration.ofMinutes(30);
  private static final Duration CACHE_LENGTH_BUILD_LATEST = Duration.ofMinutes(5);

  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;

  private final StorageService storage;

  @Autowired
  public Meta3Controller(
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

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ProjectsResponse.class)
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get a list of all projects"
  )
  public ResponseEntity<?> getProjects() {
    final List<ProjectEntity> projects = this.projects.findAll()
      .stream()
      .sorted(Project.COMPARATOR_KEY)
      .toList();
    final ProjectsResponse response = new ProjectsResponse(
      Lists.transform(projects, this::createProjectResponse)
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECTS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ProjectResponse.class)
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get details of a specific project"
  )
  public ResponseEntity<?> getProject(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final ProjectResponse response = this.createProjectResponse(project);
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_PROJECT));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}/versions")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          array = @ArraySchema(
            schema = @Schema(implementation = VersionResponse.class)
          ),
          mediaType = MediaType.APPLICATION_JSON_VALUE
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get a list of versions for a specific project"
  )
  public ResponseEntity<?> getVersions(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final List<VersionEntity> versions = this.versions.findAllByProject(project).toList();
    final List<ObjectId> versionIds = versions.stream().map(AbstractEntity::_id).toList();
    final Map<ObjectId, List<BuildEntity>> buildsByVersion = this.builds.findAllByVersionIn(versionIds)
      .collect(Collectors.groupingBy(BuildEntity::version));
    final VersionsResponse response = new VersionsResponse(
      Lists.transform(versions, version -> {
        final List<BuildEntity> buildsIn = buildsByVersion.getOrDefault(version._id(), List.of());
        return this.createVersionResponse(version, buildsIn);
      })
    );
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSIONS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}/versions/{version}")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = VersionResponse.class)
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get details of a specific version for a project"
  )
  public ResponseEntity<?> getVersion(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey,
    @Parameter(description = "The key of the version")
    @PathVariable("version")
    final String versionKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    final List<BuildEntity> builds = this.builds.findAllByVersion(version).toList();
    final VersionResponse response = this.createVersionResponse(version, builds);
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_VERSION));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}/versions/{version}/builds")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          array = @ArraySchema(
            schema = @Schema(implementation = BuildResponse.class)
          ),
          mediaType = MediaType.APPLICATION_JSON_VALUE
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get a list of builds for a specific version of a project"
  )
  public ResponseEntity<?> getBuilds(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey,
    @Parameter(description = "The key of the version")
    @PathVariable("version")
    final String versionKey,
    @Parameter(in = ParameterIn.QUERY, description = "Filter builds by one or more channels")
    @RequestParam(name = "channel", required = false)
    final @Nullable List<BuildChannel> filterByChannel
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    final List<BuildEntity> builds = this.builds.findByVersionAndOptionalChannelIn(version, filterByChannel, Pageable.unpaged()).toList();
    final List<BuildResponse> response = builds.stream()
      .map(build -> this.createBuildResponse(project, version, build))
      .toList();
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILDS));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}/versions/{version}/builds/{build}")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = BuildResponse.class)
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get details of a specific build for a version of a project"
  )
  public ResponseEntity<?> getBuild(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey,
    @Parameter(description = "The key of the version")
    @PathVariable("version")
    final String versionKey,
    @Parameter(description = "The number of the build")
    @PathVariable("build")
    @PositiveOrZero
    final int buildNumber
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    final BuildEntity build = this.builds.findByVersionAndNumber(version, buildNumber).orElseThrow(BuildNotFoundException::new);
    final BuildResponse response = this.createBuildResponse(project, version, build);
    return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILD));
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping("/v3/projects/{project}/versions/{version}/builds/latest")
  @Operation(
    responses = {
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = BuildResponse.class)
        ),
        responseCode = "200" // OK
      ),
      @ApiResponse(
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ErrorResponse.class)
        ),
        responseCode = "404" // Not Found
      )
    },
    summary = "Get details of the latest build for a version of a project"
  )
  public ResponseEntity<?> getLatestBuild(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey,
    @Parameter(description = "The key of the version")
    @PathVariable("version")
    final String versionKey
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    final List<BuildEntity> builds = this.builds.findAllByVersion(version).toList();
    if (builds.isEmpty()) {
      throw new BuildNotFoundException();
    } else {
      final BuildEntity build = builds.getFirst();
      final BuildResponse response = this.createBuildResponse(project, version, build);
      return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILD_LATEST));
    }
  }

  private ProjectResponse createProjectResponse(final ProjectEntity project) {
    final Map<ObjectId, FamilyEntity> families = this.families.findAllByProject(project)
      .collect(Collectors.toMap(
        AbstractEntity::_id,
        Function.identity()
      ));
    final Map<String, List<String>> versions = this.versions.findAllByProject(project)
      .collect(Collectors.groupingBy(
        version -> families.get(version.family()),
        () -> new TreeMap<>(Family.COMPARATOR_CREATED_AT_REVERSE),
        Collectors.collectingAndThen(
          Collectors.toList(),
          list -> {
            list.sort(Version.COMPARATOR_CREATED_AT_REVERSE);
            return list;
          }
        )
      ))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        e -> e.getKey().key(),
        e -> Keyed.keysOf(e.getValue()),
        (a, b) -> b,
        LinkedHashMap::new
      ));
    return new ProjectResponse(
      new ProjectResponse.Project(
        project.key(),
        project.name()
      ),
      versions
    );
  }

  private VersionResponse createVersionResponse(final VersionEntity version, final List<BuildEntity> builds) {
    return new VersionResponse(
      new VersionResponse.Version(
        version.key(),
        version.support(),
        Objects.requireNonNullElseGet(version.java(), () -> {
          final FamilyEntity family = this.families.findById(version.family()).orElseThrow(FamilyNotFoundException::new);
          return family.java();
        })
      ),
      Lists.transform(builds, BuildEntity::number)
    );
  }

  private BuildResponse createBuildResponse(final Project project, final Version version, final BuildWithDownloads<Download> build) {
    final Map<String, DownloadWithUrl> downloads = build.downloads().entrySet()
      .stream()
      .map(entry -> {
        final Download download = entry.getValue();
        final URI url = this.storage.getDownloadUrl(project, version, build, download);
        final DownloadWithUrl downloadWithUrl = download.withUrl(url);
        return Map.entry(entry.getKey(), downloadWithUrl);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new BuildResponse(build.number(), build.createdAt(), build.channel(), build.commits(), downloads);
  }
}
