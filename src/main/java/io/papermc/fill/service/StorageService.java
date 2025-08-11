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
package io.papermc.fill.service;

import io.papermc.fill.configuration.properties.ApplicationApiProperties;
import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.Checksums;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Version;
import java.net.URI;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;

@NullMarked
public interface StorageService {
  String PROJECT_NAME = "project_name";
  String VERSION_NAME = "version_name";
  String BUILD_NUMBER = "build_number";
  String DOWNLOAD_FILENAME = "download_filename";
  String DOWNLOAD_SHA256 = "download_sha256";

  static URI createUri(
    final ApplicationApiProperties.Storage configuration,
    final Project project,
    final Version version,
    final Build build,
    final Download download
  ) {
    return configuration.url().resolve(createPath(configuration.path(), project, version, build, download));
  }

  static String createPath(
    final String template,
    final Project project,
    final Version version,
    final Build build,
    final Download download
  ) {
    return StringSubstitutor.replace(
      template,
      Map.of(
        PROJECT_NAME, project.name(),
        VERSION_NAME, version.name(),
        BUILD_NUMBER, build.id(),
        DOWNLOAD_FILENAME, download.name(),
        DOWNLOAD_SHA256, download.checksums().sha256()
      )
    );
  }

  @Deprecated
  default URI getDownloadUrl(
    final BuildEntity build,
    final Download download
  ) {
    return this.getDownloadUrl(build.project(), build.version(), build, download);
  }

  URI getDownloadUrl(
    final Project project,
    final Version version,
    final Build build,
    final Download download
  );

  void putObject(
    final Project project,
    final Version version,
    final Build build,
    final Download download,
    final byte[] content,
    final Checksums checksums
  );

  @Deprecated
  @Nullable Asset getAsset(
    final Project project,
    final Version version,
    final Build build,
    final Download download
  );

  @Deprecated
  @NullMarked
  record Asset(
    byte[] content,
    HttpHeaders headers
  ) {
  }
}
