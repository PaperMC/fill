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
import io.papermc.fill.exception.NoPayloadException;
import io.papermc.fill.exception.NoSuchDownloadException;
import io.papermc.fill.exception.NoSuchProjectException;
import io.papermc.fill.exception.NoSuchVersionException;
import io.papermc.fill.exception.PublishFailedException;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.request.PublishRequest;
import io.papermc.fill.model.request.UploadRequest;
import io.papermc.fill.model.response.PublishResponse;
import io.papermc.fill.model.response.UploadResponse;
import io.papermc.fill.service.BucketService;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
  private final BucketService buckets;

  private final LoadingCache<UUID, Payload> payloads = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(5))
    .build(_ -> new Payload());

  @Autowired
  public PublishController(
    final ProjectRepository projects,
    final VersionRepository versions,
    final BuildRepository builds,
    final BucketService buckets
  ) {
    this.projects = projects;
    this.versions = versions;
    this.builds = builds;
    this.buckets = buckets;
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
    final Payload payload = this.payloads.get(request.id());
    try {
      payload.downloads.put(file.getName(), file.getBytes());
    } catch (final IOException e) {
      throw new PublishFailedException(e);
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
    final Payload payload = this.payloads.getIfPresent(request.id());
    if (payload == null) {
      throw new PublishFailedException(new NoPayloadException());
    } else {
      this.payloads.invalidate(request.id());
    }

    final ProjectEntity project = this.projects.findByName(request.project()).orElseThrow(NoSuchProjectException::new);
    final VersionEntity version = this.versions.findByProjectAndName(project, request.version()).orElseThrow(NoSuchVersionException::new);

    if (this.builds.findByProjectAndVersionAndNumber(project, version, request.build()).isPresent()) {
      throw new BuildAlreadyExistsException();
    }

    final Map<String, Download> downloads = request.downloads();

    final BuildEntity build = BuildEntity.create(
      new ObjectId(),
      request.time(),
      project,
      version,
      request.build(),
      request.channel(),
      request.commits(),
      request.downloads()
    );

    for (final Map.Entry<String, Download> entry : downloads.entrySet()) {
      final Download download = entry.getValue();
      final byte[] bytes = payload.downloads.remove(download.name());
      if (bytes == null) {
        LOGGER.error("Failed to publish: download {} has no associated file", download.name());
        throw new PublishFailedException(new NoSuchDownloadException());
      }
      final String sha256 = Hashing.sha256().hashBytes(bytes).toString();
      if (!download.checksums().sha256().equals(sha256)) {
        LOGGER.error("Failed to publish: download {} has a mis-matching sha256 checksum (expected {}, got {})", download.name(), download.checksums().sha256(), sha256);
        throw new PublishFailedException(new NoSuchDownloadException());
      }
      try {
        this.buckets.putObject(build, download, bytes);
      } catch (final SdkException e) {
        LOGGER.error("Failed to publish: could not put object into bucket for {}", download.name(), e);
        throw new PublishFailedException(e);
      }
    }

    if (!payload.downloads.isEmpty()) {
      LOGGER.error("Failed to publish: additional files ({}) were provided that have no defined downloads", String.join(", ", payload.downloads.keySet()));
      throw new PublishFailedException(new NoSuchDownloadException());
    }

    this.builds.save(build);

    return Responses.created(new PublishResponse(true, build._id()));
  }

  @NullMarked
  static class Payload {
    final Map<String, byte[]> downloads = new HashMap<>();
  }
}
