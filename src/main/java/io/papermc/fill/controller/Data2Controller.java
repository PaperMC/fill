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

import io.papermc.fill.SharedConstants;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.DownloadFailedException;
import io.papermc.fill.exception.DownloadNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.StorageReadException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.Download;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.http.Caching;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@Deprecated
@Hidden
@NullMarked
public class Data2Controller {
  private static final Duration CACHE_LENGTH_DOWNLOAD = Duration.ofDays(7);

  private final ProjectRepository projects;
  private final VersionRepository versions;
  private final BuildRepository builds;

  private final StorageService storage;

  @Autowired
  public Data2Controller(
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

  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds/{build:\\d+}/downloads/{download:[a-zA-Z0-9._-]+}")
  public ResponseEntity<?> getDownload(
    @PathVariable("project")
    final String projectKey,
    @PathVariable("version")
    final String versionKey,
    @PathVariable("build")
    @PositiveOrZero
    final int buildNumber,
    @PathVariable("download")
    final String downloadName
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

    final Download download = build.getDownloadByName(downloadName);
    if (download != null) {
      final StorageService.Asset object;
      try {
        object = this.storage.getObject(project, version, build, download);
      } catch (final StorageReadException e) {
        throw new DownloadFailedException(e);
      }
      if (object != null) {
        return Responses.ok(new ByteArrayResource(object.content()), headers -> {
          headers.putAll(object.headers());
          if (!headers.containsKey(HttpHeaders.CACHE_CONTROL)) {
            headers.setCacheControl(Caching.publicShared(CACHE_LENGTH_DOWNLOAD));
          }
          headers.setContentDisposition(
            ContentDisposition.attachment()
              .filename(download.name())
              .build()
          );
          if (!headers.containsKey(HttpHeaders.ETAG)) {
            headers.setETag(String.format(
              "\"%s\"",
              download.checksums().sha256()
            ));
          }
        });
      }
    }

    throw new DownloadNotFoundException();
  }
}
