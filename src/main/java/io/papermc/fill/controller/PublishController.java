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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.hash.Hashing;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.BuildAlreadyExistsException;
import io.papermc.fill.exception.ChecksumMismatchException;
import io.papermc.fill.exception.InvalidStagingInstanceException;
import io.papermc.fill.exception.NoSuchDownloadException;
import io.papermc.fill.exception.NoSuchProjectException;
import io.papermc.fill.exception.NoSuchVersionException;
import io.papermc.fill.exception.PublishFailedException;
import io.papermc.fill.model.Checksums;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.request.PublishRequest;
import io.papermc.fill.model.request.UploadRequest;
import io.papermc.fill.model.response.PublishResponse;
import io.papermc.fill.model.response.UploadResponse;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.BuildPublishListener;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

@Hidden
@NullMarked
@RestController
public class PublishController {
  private static final Logger LOGGER = LoggerFactory.getLogger(PublishController.class);

  private final ProjectRepository projects;
  private final VersionRepository versions;
  private final BuildRepository builds;
  private final StorageService storage;
  private final Set<BuildPublishListener> buildPublishListeners;

  private final LoadingCache<UUID, StagingInstance> instances = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(5))
    .build(_ -> new StagingInstance());

  @Autowired
  public PublishController(
    final ProjectRepository projects,
    final VersionRepository versions,
    final BuildRepository builds,
    final StorageService storage,
    final Set<BuildPublishListener> buildPublishListeners
  ) {
    this.projects = projects;
    this.versions = versions;
    this.builds = builds;
    this.storage = storage;
    this.buildPublishListeners = buildPublishListeners;
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    path = "/upload"
  )
  @PreAuthorize("hasRole('API_PUBLISH')")
  public ResponseEntity<?> upload(
    @RequestPart
    final UploadRequest request,
    @RequestParam
    final MultipartFile file
  ) {
    final StagingInstance instance = this.instances.get(request.id());
    try {
      final String filename = file.getOriginalFilename();
      if (filename == null || filename.isBlank()) {
        final String message = "Missing filename";
        throw createPublishFailedException(request, message, new IllegalArgumentException(message));
      }
      instance.addStagedFile(filename, file.getBytes());
    } catch (final IOException e) {
      throw createPublishFailedException(request, "i/o exception", e);
    }

    return Responses.ok(new UploadResponse(true));
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    path = "/publish"
  )
  @PreAuthorize("hasRole('API_PUBLISH')")
  public ResponseEntity<?> publish(
    @RequestBody
    final PublishRequest request
  ) {
    final StagingInstance instance = this.instances.getIfPresent(request.id());
    if (instance == null) {
      throw createPublishFailedException(request, "Invalid staging instance", new InvalidStagingInstanceException());
    } else {
      this.instances.invalidate(request.id());
    }

    final ProjectEntity project = this.projects.findByName(request.project()).orElseThrow(NoSuchProjectException::new);
    final VersionEntity version = this.versions.findByProjectAndName(project, request.version()).orElseThrow(NoSuchVersionException::new);

    if (this.builds.findByProjectAndVersionAndNumber(project, version, request.build()).isPresent()) {
      throw createPublishFailedException(request, "Build already exists", new BuildAlreadyExistsException());
    }

    final List<Commit> commits = request.commits().reversed();
    final Map<String, Download> downloads = request.downloads();

    Commit.checkOrder(commits);

    final BuildEntity build = BuildEntity.create(
      new ObjectId(),
      request.time(),
      project,
      version,
      request.build(),
      request.channel(),
      commits,
      downloads
    );

    for (final Map.Entry<String, Download> entry : downloads.entrySet()) {
      final Download download = entry.getValue();
      final byte[] bytes = instance.removeStagedFile(download.name());
      if (bytes == null) {
        throw createPublishFailedException(request, String.format("Download %s has no associated file", download.name()), new NoSuchDownloadException());
      }
      final Checksums checksums = createChecksums(bytes);
      if (!download.checksums().equals(checksums)) {
        final String message = String.format(
          "Download %s has mis-matching checksums (expected %s, got %s)",
          download.name(),
          download.checksums(),
          checksums
        );
        throw createPublishFailedException(request, message, new ChecksumMismatchException(message));
      }
      try {
        this.storage.putObject(project, version, build, download, bytes, checksums);
      } catch (final SdkException e) {
        throw createPublishFailedException(request, String.format("Could not put object into bucket for %s", download.name()), e);
      }
    }

    if (!instance.hasAnyStagedFiles()) {
      throw createPublishFailedException(request, String.format("Additional files (%s) were provided that have no defined downloads", String.join(", ", instance.files.keySet())), new NoSuchDownloadException());
    }

    this.builds.save(build);

    for (final BuildPublishListener listener : this.buildPublishListeners) {
      listener.onBuildPublished(project, version, build);
    }

    return Responses.created(new PublishResponse(true, build._id()));
  }

  private static Checksums createChecksums(final byte[] bytes) {
    return new Checksums(
      Hashing.sha256().hashBytes(bytes).toString()
    );
  }

  private static PublishFailedException createPublishFailedException(final Object request, final String message, final Throwable throwable) {
    LOGGER.error("Failed to publish [{}]: {}", request, message, throwable);
    return new PublishFailedException("Publishing the build failed: " + message, throwable);
  }

  @NullMarked
  static final class StagingInstance {
    private final Map<String, byte[]> files = new HashMap<>();

    public boolean hasAnyStagedFiles() {
      return !this.files.isEmpty();
    }

    public byte @Nullable [] removeStagedFile(final String name) {
      return this.files.get(name);
    }

    public void addStagedFile(final String name, final byte[] bytes) {
      this.files.put(name, bytes);
    }
  }
}
