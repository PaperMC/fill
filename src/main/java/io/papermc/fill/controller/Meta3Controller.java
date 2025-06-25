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
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.NoSuchBuildException;
import io.papermc.fill.exception.NoSuchProjectException;
import io.papermc.fill.exception.NoSuchVersionException;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.DownloadWithUrl;
import io.papermc.fill.model.FamilyComparator;
import io.papermc.fill.model.VersionComparator;
import io.papermc.fill.model.response.ErrorResponse;
import io.papermc.fill.model.response.v3.BuildResponse;
import io.papermc.fill.model.response.v3.ProjectResponse;
import io.papermc.fill.model.response.v3.ProjectsResponse;
import io.papermc.fill.model.response.v3.VersionResponse;
import io.papermc.fill.model.response.v3.VersionsResponse;
import io.papermc.fill.service.BucketService;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
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

  private static final Comparator<ProjectEntity> SORT_PROJECTS = Comparator.comparing(ProjectEntity::name);

  private final ProjectRepository projects;
  private final VersionRepository versions;
  private final BuildRepository builds;
  private final BucketService bucket;

  @Autowired
  public Meta3Controller(
    final ProjectRepository projects,
    final VersionRepository versions,
    final BuildRepository builds,
    final BucketService bucket
  ) {
    this.projects = projects;
    this.versions = versions;
    this.builds = builds;
    this.bucket = bucket;
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
    final List<ProjectEntity> projects = this.projects.findAll();
    final ProjectsResponse response = new ProjectsResponse(
      projects
        .stream()
        .sorted(SORT_PROJECTS)
        .map(this::createProjectResponse)
        .toList()
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final ProjectResponse response = this.createProjectResponse(pe);
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final List<VersionEntity> versions = this.versions.findAllByProject(pe)
      .toList()
      .reversed();
    final VersionsResponse response = new VersionsResponse(
      versions.stream()
        .sorted(VersionComparator.CREATED_AT.reversed())
        .map(this::createVersionResponse)
        .toList()
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project,
    @Parameter(description = "The name of the version")
    @PathVariable
    final String version
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final VersionResponse response = this.createVersionResponse(ve);
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project,
    @Parameter(description = "The name of the version")
    @PathVariable
    final String version,
    @Parameter(in = ParameterIn.QUERY, description = "Filter builds by channel")
    @RequestParam(name = "channel", required = false)
    final @Nullable BuildChannel filterByChannel
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final List<BuildEntity> bes = this.builds.findAllByProjectAndVersion(pe, ve)
      .filter(be -> (filterByChannel == null) || be.channel() == filterByChannel)
      .toList()
      .reversed();
    final List<BuildResponse> response = bes.stream()
      .map(this::createBuildResponse)
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project,
    @Parameter(description = "The name of the version")
    @PathVariable
    final String version,
    @Parameter(description = "The name of the build")
    @PathVariable
    @PositiveOrZero
    final int build
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final BuildEntity be = this.builds.findByProjectAndVersionAndNumber(pe, ve, build).orElseThrow(NoSuchBuildException::new);
    final BuildResponse response = this.createBuildResponse(be);
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
    @Parameter(description = "The name of the project")
    @PathVariable
    final String project,
    @Parameter(description = "The name of the version")
    @PathVariable
    final String version
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final List<BuildEntity> builds = this.builds.findAllByProjectAndVersion(pe, ve)
      .toList()
      .reversed();
    if (builds.isEmpty()) {
      throw new NoSuchBuildException();
    } else {
      final BuildEntity be = builds.getFirst();
      final BuildResponse response = this.createBuildResponse(be);
      return Responses.ok(response, Caching.publicShared(CACHE_LENGTH_BUILD_LATEST));
    }
  }

  private ProjectResponse createProjectResponse(final ProjectEntity project) {
    final Map<String, List<String>> versions = this.versions.findAllByProject(project)
      .toList()
      .reversed()
      .stream()
      .collect(Collectors.groupingBy(
        VersionEntity::family,
        () -> new TreeMap<>(FamilyComparator.CREATED_AT.reversed()),
        Collectors.collectingAndThen(
          Collectors.toList(),
          list -> {
            list.sort(VersionComparator.CREATED_AT.reversed());
            return list;
          }
        )
      ))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        e -> e.getKey().name(),
        e -> e.getValue().stream().map(VersionEntity::name).toList(),
        (a, b) -> b,
        LinkedHashMap::new
      ));
    return new ProjectResponse(
      new ProjectResponse.Project(
        project.name()
      ),
      versions
    );
  }

  private VersionResponse createVersionResponse(final VersionEntity version) {
    final List<BuildEntity> builds = this.builds.findAllByProjectAndVersion(version.project(), version).toList();
    return new VersionResponse(
      new VersionResponse.Version(
        version.name(),
        version.support(),
        Objects.requireNonNullElse(version.java(), version.family().java())
      ),
      builds
        .reversed()
        .stream()
        .map(BuildEntity::number)
        .toList()
    );
  }

  private BuildResponse createBuildResponse(final BuildEntity build) {
    final Map<String, DownloadWithUrl> downloads = build.downloads().entrySet()
      .stream()
      .map(entry -> {
        final Download download = entry.getValue();
        final URI url = this.bucket.getDownloadUrl(build, download);
        final DownloadWithUrl downloadWithUrl = download.withUrl(url);
        return Map.entry(entry.getKey(), downloadWithUrl);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new BuildResponse(build.number(), build.createdAt(), build.channel(), build.commits(), downloads);
  }
}
