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
import io.papermc.fill.exception.NoSuchDownloadException;
import io.papermc.fill.exception.NoSuchProjectException;
import io.papermc.fill.exception.NoSuchVersionException;
import io.papermc.fill.model.Download;
import io.papermc.fill.service.BucketService;
import io.papermc.fill.util.http.Caching;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.Map;
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
  private final BucketService bucket;

  @Autowired
  public Data2Controller(
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

  @GetMapping("/v2/projects/{project:[a-z]+}/versions/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/builds/{build:\\d+}/downloads/{download:[a-zA-Z0-9._-]+}")
  public ResponseEntity<?> getDownload(
    @PathVariable
    final String project,
    @PathVariable
    final String version,
    @PathVariable
    @PositiveOrZero
    final int build,
    @PathVariable
    final String download
  ) {
    final ProjectEntity pe = this.projects.findByName(project).orElseThrow(NoSuchProjectException::new);
    final VersionEntity ve = this.versions.findByProjectAndName(pe, version).orElseThrow(NoSuchVersionException::new);
    final BuildEntity be = this.builds.findByProjectAndVersionAndNumber(pe, ve, build).orElseThrow(NoSuchBuildException::new);

    for (final Map.Entry<String, Download> entry : be.downloads().entrySet()) {
      final Download value = entry.getValue();
      final String name = value.name();
      if (name.equals(download)) {
        final BucketService.Asset asset = this.bucket.getAsset(be, value);
        if (asset != null) {
          return Responses.ok(new ByteArrayResource(asset.content()), headers -> {
            headers.putAll(asset.headers());
            if (!headers.containsKey(HttpHeaders.CACHE_CONTROL)) {
              headers.setCacheControl(Caching.publicShared(CACHE_LENGTH_DOWNLOAD));
            }
            headers.setContentDisposition(
              ContentDisposition.attachment()
                .filename(name)
                .build()
            );
            if (!headers.containsKey(HttpHeaders.ETAG)) {
              headers.setETag("\"%s\"".formatted(value.checksums().sha256()));
            }
          });
        }
      }
    }

    throw new NoSuchDownloadException();
  }
}
