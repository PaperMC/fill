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
import io.papermc.fill.exception.StorageReadException;
import io.papermc.fill.exception.StorageWriteException;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Version;
import io.papermc.fill.s3.S3Configuration;
import io.papermc.fill.util.http.Headers;
import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
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
    final BuildWithDownloads<Download> build,
    final Download download
  ) {
    return StorageService.createUri(this.properties.storage(), project, version, build, download);
  }

  @Override
  public void putObject(
    final Project project,
    final Version version,
    final BuildWithDownloads<Download> build,
    final Download download,
    final byte[] content,
    final MediaType type
  ) throws StorageWriteException {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    final String path = StorageService.createPath(properties.path(), project, version, build, download);
    final PutObjectRequest.Builder request = PutObjectRequest.builder()
      .bucket(properties.s3().bucket())
      .key(path)
      .contentLength((long) content.length)
      .contentType(type.toString());
    try {
      this.s3.putObject(request.build(), RequestBody.fromBytes(content));
    } catch (final SdkException e) {
      throw createStorageWriteException(download, path, "s3 exception", e);
    }
  }

  @Deprecated
  @Override
  public @Nullable Asset getObject(
    final Project project,
    final Version version,
    final BuildWithDownloads<Download> build,
    final Download download
  ) throws StorageReadException {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    return switch (properties.legacyRetrievalStrategy()) {
      case BUCKET -> {
        final String path = StorageService.createPath(properties.path(), project, version, build, download);
        final GetObjectRequest request = GetObjectRequest.builder()
          .bucket(properties.s3().bucket())
          .key(path)
          .build();
        final ResponseInputStream<GetObjectResponse> response;
        try {
          response = this.s3.getObject(request);
        } catch (final S3Exception e) {
          throw createStorageReadException(download, path, "s3 exception", e);
        }
        LOGGER.debug("Retrieved object {} from bucket", download);
        try {
          yield new Asset(response.readAllBytes(), HttpHeaders.EMPTY);
        } catch (final IOException e) {
          throw createStorageReadException(download, path, "i/o exception", e);
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
              LOGGER.info("Retrieved object [{}] from bucket [{}]", download, uri);
              final HttpHeaders oldHeaders = response.getHeaders();
              final HttpHeaders newHeaders = Headers.copySharedHeaders(oldHeaders);
              yield new Asset(content, newHeaders);
            } else {
              throw createStorageReadException(download, uri, "no content", new NoSuchElementException());
            }
          } else {
            throw createStorageReadException(download, uri, String.format("non-2xx response [%s]", response.getStatusCode()), new NoSuchElementException());
          }
        } catch (final HttpClientErrorException e) {
          throw createStorageReadException(download, uri, "http exception", e);
        }
      }
    };
  }

  private static StorageReadException createStorageReadException(final Download download, final Object path, final String reason, final Throwable throwable) {
    final String message = String.format("Failed to read object [%s] from storage [%s]: %s", download, path, reason);
    LOGGER.error(message, throwable);
    return new StorageReadException(message, throwable);
  }

  private static StorageWriteException createStorageWriteException(final Download download, final Object path, final String reason, final Throwable throwable) {
    final String message = String.format("Failed to write object [%s] to storage [%s]: %s", download, path, reason);
    LOGGER.error(message, throwable);
    return new StorageWriteException(message, throwable);
  }
}
