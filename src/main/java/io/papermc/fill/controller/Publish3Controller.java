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
import io.papermc.fill.exception.DuplicateBuildException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.PublishFailedException;
import io.papermc.fill.exception.StorageWriteException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.request.PublishRequest;
import io.papermc.fill.model.request.v3.UploadRequest;
import io.papermc.fill.model.response.PublishResponse;
import io.papermc.fill.model.response.v3.UploadResponse;
import io.papermc.fill.notification.BuildListener;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.http.Responses;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
public class Publish3Controller {
  private static final boolean CREATE_MISSING_VERSIONS = true;
  private static final Logger LOGGER = LoggerFactory.getLogger(Publish3Controller.class);

  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;
  private final StorageService storage;
  private final Set<BuildListener> buildListeners;

  @Autowired
  public Publish3Controller(
    final ProjectRepository projects,
    final FamilyRepository families,
    final VersionRepository versions,
    final BuildRepository builds,
    final StorageService storage,
    final Set<BuildListener> buildListeners
  ) {
    this.projects = projects;
    this.families = families;
    this.versions = versions;
    this.builds = builds;
    this.storage = storage;
    this.buildListeners = buildListeners;
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    path = "/v3/upload"
  )
  @PreAuthorize("hasRole('API_PUBLISH')")
  public ResponseEntity<?> upload(@RequestBody final UploadRequest request) {
    if (request.download().name().isBlank() || request.download().checksums().sha256().isBlank() || request.download().size() < 0 || request.contentType().isBlank() || request.contentMd5().isBlank()) {
      final String message = "Invalid upload metadata";
      throw createPublishFailedException(request, message, new IllegalArgumentException(message));
    }
    try {
      return Responses.ok(new UploadResponse(
        true,
        this.storage.createUploadUrl(request.id(), request.download(), request.contentMd5(), MediaType.parseMediaType(request.contentType()))
      ));
    } catch (final StorageWriteException | IllegalArgumentException e) {
      throw createPublishFailedException(request, "Could not create upload URL", e);
    }
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    path = "/v3/publish"
  )
  @PreAuthorize("hasRole('API_PUBLISH')")
  public ResponseEntity<?> publish(
    @RequestBody
    final PublishRequest request
  ) {
    final Instant createdAt = request.time();

    final ProjectEntity project = this.projects.findByKey(request.project()).orElseThrow(ProjectNotFoundException::new);
    final FamilyEntity family = this.families.findByProjectAndKey(project, request.family()).orElseThrow(FamilyNotFoundException::new);
    VersionEntity version = this.versions.findByProjectAndKey(project, request.version()).orElse(null);
    if (version == null) {
      if (CREATE_MISSING_VERSIONS) {
        version = this.versions.save(VersionEntity.create(
          new ObjectId(Date.from(createdAt)),
          createdAt,
          project,
          family,
          request.version(),
          null,
          Support.SUPPORTED,
          null
        ));
      } else {
        throw new VersionNotFoundException();
      }
    }

    final List<Commit> commits = request.commits().reversed();
    final Map<String, Download> downloads = request.downloads();

    final BuildEntity existingBuild = this.builds.findByVersionAndNumber(version, request.build()).orElse(null);
    if (existingBuild != null) {
      if (isSameBuild(existingBuild, request, commits, downloads)) {
        this.deleteStagedObjects(request, downloads);
        return Responses.created(new PublishResponse(true, existingBuild._id()));
      }
      throw createPublishFailedException(request, "Build already exists", new DuplicateBuildException());
    }

    final BuildEntity build = BuildEntity.create(
      new ObjectId(Date.from(createdAt)),
      createdAt,
      project,
      version,
      request.build(),
      request.channel(),
      commits,
      downloads
    );

    for (final Download download : downloads.values()) {
      try {
        this.storage.verifyStagedObject(request.id(), download);
      } catch (final StorageWriteException e) {
        throw createPublishFailedException(request, String.format("Could not verify staged object for %s", download.name()), e);
      }
    }

    for (final Download download : downloads.values()) {
      try {
        this.storage.promoteStagedObject(request.id(), project, version, build, download);
      } catch (final StorageWriteException e) {
        throw createPublishFailedException(request, String.format("Could not promote staged object for %s", download.name()), e);
      }
    }

    this.builds.save(build);
    this.deleteStagedObjects(request, downloads);

    for (final BuildListener listener : this.buildListeners) {
      listener.onBuildPublished(project, version, build);
    }

    return Responses.created(new PublishResponse(true, build._id()));
  }

  private static PublishFailedException createPublishFailedException(final Object request, final String message, final Throwable throwable) {
    LOGGER.error("Failed to publish [{}]: {}", request, message, throwable);
    return new PublishFailedException("Publishing the build failed: " + message, throwable);
  }

  private static boolean isSameBuild(
    final BuildEntity build,
    final PublishRequest request,
    final List<Commit> commits,
    final Map<String, Download> downloads
  ) {
    return build.createdAt().equals(request.time()) &&
      build.channel() == request.channel() &&
      build.commits().equals(commits) &&
      build.downloads().equals(downloads);
  }

  private void deleteStagedObjects(final PublishRequest request, final Map<String, Download> downloads) {
    for (final Download download : downloads.values()) {
      try {
        this.storage.deleteStagedObject(request.id(), download.name());
      } catch (final StorageWriteException e) {
        LOGGER.warn("Failed to delete staged object for [{}]", download.name(), e);
      }
    }
  }
}
