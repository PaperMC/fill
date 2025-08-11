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
import io.papermc.fill.exception.DownloadFailedException;
import io.papermc.fill.model.Build;
import io.papermc.fill.model.Checksums;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Version;
import io.papermc.fill.s3.S3Configuration;
import io.papermc.fill.util.http.Headers;
import io.papermc.fill.util.http.MediaTypes;
import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@NullMarked
@Service
public class StorageServiceImpl implements StorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);
  private final ApplicationApiProperties properties;
  private final S3Client s3;
  private final RestClient http;

  @Autowired
  public StorageServiceImpl(
    final ApplicationApiProperties properties,
    final RestClient.Builder http
  ) {
    this.properties = properties;
    this.s3 = S3Configuration.createClient(properties.storage().s3());
    this.http = http.build();
  }

  @Override
  public URI getDownloadUrl(
    final Project project,
    final Version version,
    final Build build,
    final Download download
  ) {
    return StorageService.createUri(this.properties.storage(), project, version, build, download);
  }

  @Override
  public void putObject(
    final Project project,
    final Version version,
    final Build build,
    final Download download,
    final byte[] content,
    final Checksums checksums
  ) {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    final PutObjectRequest.Builder request = PutObjectRequest.builder()
      .bucket(properties.s3().bucket())
      .key(StorageService.createPath(properties.path(), project, version, build, download))
      .contentLength((long) content.length)
      .contentType(MediaTypes.APPLICATION_JAVA_ARCHIVE_VALUE);
    this.s3.putObject(request.build(), RequestBody.fromBytes(content));
  }

  @Deprecated
  @Override
  public @Nullable Asset getAsset(
    final Project project,
    final Version version,
    final Build build,
    final Download download
  ) {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    return switch (properties.legacyRetrievalStrategy()) {
      case BUCKET -> {
        final String path = StorageService.createPath(properties.path(), project, version, build, download);
        try {
          final GetObjectRequest request = GetObjectRequest.builder()
            .bucket(properties.s3().bucket())
            .key(path)
            .build();
          final ResponseInputStream<GetObjectResponse> response = this.s3.getObject(request);
          LOGGER.debug("Retrieved asset {} from bucket", download);
          yield new Asset(response.readAllBytes(), HttpHeaders.EMPTY);
        } catch (final IOException | S3Exception e) {
          throw createDownloadFailedException(download, path, "s3 exception", e);
        }
      }
      case HTTP -> {
        final URI uri = this.getDownloadUrl(project, version, build, download);
        try {
          final ResponseEntity<byte[]> response = this.http.get()
            .uri(uri)
            .header(HttpHeaders.USER_AGENT, "Fill (Internal)")
            .retrieve()
            .toEntity(byte[].class);
          if (response.getStatusCode().is2xxSuccessful()) {
            final byte[] content = response.getBody();
            if (content != null) {
              LOGGER.info("Retrieved asset [{}] from bucket [{}]", download, uri);
              final HttpHeaders oldHeaders = response.getHeaders();
              final HttpHeaders newHeaders = Headers.copySharedHeaders(oldHeaders);
              yield new Asset(content, newHeaders);
            } else {
              throw createDownloadFailedException(download, uri, "no content", new NoSuchElementException());
            }
          } else {
            throw createDownloadFailedException(download, uri, String.format("non-2xx response [%s]", response.getStatusCode()), new NoSuchElementException());
          }
        } catch (final HttpClientErrorException e) {
          throw createDownloadFailedException(download, uri, "http exception", e);
        }
      }
    };
  }

  private static DownloadFailedException createDownloadFailedException(final Download download, final Object path, final String message, final Throwable throwable) {
    LOGGER.error("Failed to retrieve asset [{}] from bucket [{}]: {}", download, path, message, throwable);
    return new DownloadFailedException(throwable);
  }
}
