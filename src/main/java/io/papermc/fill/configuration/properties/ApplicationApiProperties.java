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
package io.papermc.fill.configuration.properties;

import io.papermc.fill.s3.S3Configuration;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.api")
@NullMarked
public record ApplicationApiProperties(
  @Deprecated(forRemoval = true)
  Map<String, List<LegacyDownloadKeyMapping>> legacyDownloadKeyMappings,
  Metadata metadata,
  Storage storage
) {
  @NullMarked
  public record LegacyDownloadKeyMapping(
    String from,
    String to
  ) {
  }

  @NullMarked
  public record Metadata(
    String title,
    @Nullable String url,
    String version
  ) {
  }

  @NullMarked
  public record Storage(
    S3 s3,
    URI url,
    /*
     * The following variables are available:
     * - "project_name": the project name
     * - "version_name": the version name
     * - "build_number": the build number
     * - "download_filename": the file name of the download
     * - "download_sha256": the sha256 hash of the download
     */
    String path
  ) {
    @NullMarked
    public record S3(
      @Nullable URI endpoint,
      String region,
      String accessKeyId,
      String secretAccessKey,
      String bucket,
      boolean usePathStyleAccess
    ) implements S3Configuration {
    }
  }
}
