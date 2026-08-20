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

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.configuration.properties.ApplicationApiProperties;
import io.papermc.fill.exception.StorageReadException;
import io.papermc.fill.exception.StorageWriteException;
import io.papermc.fill.model.BuildWithDownloads;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Project;
import io.papermc.fill.model.Version;
import io.papermc.fill.s3.S3Configuration;
import io.papermc.fill.util.http.Headers;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@NullMarked
@Service
public class StorageServiceImpl implements StorageService {
  private static final String METADATA_SHA256 = "sha256";
  // The bucket must expire abandoned objects under this prefix with a lifecycle rule
  // to prevent object leaks from failed publications.
  private static final String STAGING_PREFIX = "staging/";
  private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(15);
  private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);
  private final ApplicationApiProperties properties;
  private final S3Client s3;
  private final S3Presigner presigner;
  private final RestClient http;

  @Autowired
  public StorageServiceImpl(
    final ApplicationApiProperties properties
  ) {
    this.properties = properties;
    this.s3 = S3Configuration.createClient(properties.storage().s3());
    this.presigner = S3Configuration.createPresigner(properties.storage().s3());
    this.http = RestClient.builder()
      .defaultHeader(HttpHeaders.USER_AGENT, "Fill (Internal)")
      .build();
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
    final MimeType type
  ) throws StorageWriteException {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    final String path = StorageService.createPath(properties.path(), project, version, build, download);
    final PutObjectRequest request = PutObjectRequest.builder()
      .bucket(properties.s3().bucket())
      .key(path)
      .contentLength((long) content.length)
      .contentType(type.toString())
      .build();
    try {
      this.s3.putObject(request, RequestBody.fromBytes(content));
    } catch (final SdkException e) {
      throw createStorageWriteException(download, path, "s3 exception", e);
    }
  }

  @Override
  public URI createUploadUrl(
    final UUID id,
    final Download download
  ) throws StorageWriteException {
    final String path = createStagingPath(id, download.name());
    final PutObjectRequest request = PutObjectRequest.builder()
      .bucket(this.properties.storage().s3().bucket())
      .key(path)
      .contentLength((long) download.size())
      .contentMD5(generateContentMd5(download.checksums().md5()))
      .contentType(download.type())
      .metadata(Map.of(METADATA_SHA256, download.checksums().sha256()))
      .build();
    try {
      return URI.create(this.presigner.presignPutObject(
        PutObjectPresignRequest.builder()
          .signatureDuration(UPLOAD_URL_DURATION)
          .putObjectRequest(request)
          .build()
      ).url().toString());
    } catch (final SdkException e) {
      throw createStorageWriteException(download, path, "s3 exception", e);
    }
  }

  @VisibleForTesting
  static String generateContentMd5(final String string) {
    return Base64.getEncoder().encodeToString(HexFormat.of().parseHex(string));
  }

  @Override
  public void verifyStagedObject(final UUID id, final Download download) throws StorageWriteException {
    final String path = createStagingPath(id, download.name());
    try {
      final HeadObjectResponse response = this.s3.headObject(
        HeadObjectRequest.builder()
          .bucket(this.properties.storage().s3().bucket())
          .key(path)
          .build()
      );
      if (response.contentLength() != download.size()) {
        throw createStorageWriteException(download, path, String.format("expected size %d but got %d", download.size(), response.contentLength()), new IllegalArgumentException());
      }
      final String actualSha256 = response.metadata().get(METADATA_SHA256);
      if (!download.checksums().sha256().equals(actualSha256)) {
        throw createStorageWriteException(download, path, String.format("expected SHA-256 %s but got %s", download.checksums().sha256(), actualSha256), new IllegalArgumentException());
      }
    } catch (final SdkException e) {
      throw createStorageWriteException(download, path, "s3 exception", e);
    }
  }

  @Override
  public void promoteStagedObject(
    final UUID id,
    final Project project,
    final Version version,
    final BuildWithDownloads<Download> build,
    final Download download
  ) throws StorageWriteException {
    final ApplicationApiProperties.Storage properties = this.properties.storage();
    final String source = createStagingPath(id, download.name());
    final String destination = StorageService.createPath(properties.path(), project, version, build, download);
    try {
      this.s3.copyObject(
        CopyObjectRequest.builder()
          .sourceBucket(properties.s3().bucket())
          .sourceKey(source)
          .destinationBucket(properties.s3().bucket())
          .destinationKey(destination)
          .build()
      );
    } catch (final SdkException e) {
      throw createStorageWriteException(download, destination, "s3 exception", e);
    }
  }

  @Override
  public void deleteStagedObject(final UUID id, final String filename) throws StorageWriteException {
    final String path = createStagingPath(id, filename);
    try {
      this.s3.deleteObject(
        DeleteObjectRequest.builder()
          .bucket(this.properties.storage().s3().bucket())
          .key(path)
          .build()
      );
    } catch (final SdkException e) {
      throw createStorageWriteException(filename, path, "s3 exception", e);
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
        try (final ResponseInputStream<GetObjectResponse> response = this.s3.getObject(request)) {
          LOGGER.debug("Retrieved object {} from bucket", download);
          final byte[] bytes = response.readAllBytes();
          yield new Asset(bytes, HttpHeaders.EMPTY);
        } catch (final S3Exception e) {
          throw createStorageReadException(download, path, "s3 exception", e);
        } catch (final IOException e) {
          throw createStorageReadException(download, path, "i/o exception", e);
        }
      }
      case HTTP -> {
        final URI uri = this.getDownloadUrl(project, version, build, download);
        try {
          final ResponseEntity<byte[]> response = this.http.get()
            .uri(uri)
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

  private static String createStagingPath(final UUID id, final String filename) {
    return String.format("%s%s/%s", STAGING_PREFIX, id, filename);
  }

  @PreDestroy
  public void close() {
    this.presigner.close();
    this.s3.close();
  }

  private static StorageReadException createStorageReadException(final Download download, final Object path, final String reason, final Throwable throwable) {
    final String message = String.format("Failed to read object [%s] from storage [%s]: %s", download, path, reason);
    LOGGER.error(message, throwable);
    return new StorageReadException(message, throwable);
  }

  private static StorageWriteException createStorageWriteException(final Object object, final Object path, final String reason, final Throwable throwable) {
    final String message = String.format("Failed to write object [%s] to storage [%s]: %s", object, path, reason);
    LOGGER.error(message, throwable);
    return new StorageWriteException(message, throwable);
  }
}
