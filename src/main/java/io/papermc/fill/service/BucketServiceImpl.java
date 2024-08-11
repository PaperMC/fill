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
import io.papermc.fill.exception.DownloadFailedException;
import io.papermc.fill.model.Download;
import io.papermc.fill.s3.S3Configuration;
import io.papermc.fill.util.http.Headers;
import io.papermc.fill.util.http.MediaTypes;
import java.io.IOException;
import java.net.URI;
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
public class BucketServiceImpl implements BucketService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BucketServiceImpl.class);
  private static final boolean GET_ASSET_FROM_BUCKET = true;
  private final ApplicationApiProperties properties;
  private final S3Client s3;
  private final RestClient http;

  @Autowired
  public BucketServiceImpl(
    final ApplicationApiProperties properties,
    final RestClient.Builder http
  ) {
    this.properties = properties;
    this.s3 = S3Configuration.createClient(properties.storage().s3());
    this.http = http.build();
  }

  @Override
  public URI getDownloadUrl(final BuildEntity build, final Download download) {
    return BucketService.createUri(this.properties.storage(), build, download);
  }

  @Override
  public void putObject(final BuildEntity build, final Download download, final byte[] content) {
    final PutObjectRequest request = PutObjectRequest.builder()
      .bucket(this.properties.storage().s3().bucket())
      .key(BucketService.createPath(this.properties.storage().path(), build, download))
      .contentLength((long) content.length)
      .contentType(MediaTypes.APPLICATION_JAVA_ARCHIVE_VALUE)
      .build();
    this.s3.putObject(request, RequestBody.fromBytes(content));
  }

  @Override
  public @Nullable Asset getAsset(final BuildEntity build, final Download download) {
    if (GET_ASSET_FROM_BUCKET) {
      try {
        final GetObjectRequest request = GetObjectRequest.builder()
          .bucket(this.properties.storage().s3().bucket())
          .key(BucketService.createPath(this.properties.storage().path(), build, download))
          .build();
        final ResponseInputStream<GetObjectResponse> response = this.s3.getObject(request);
        LOGGER.debug("Retrieved asset {} from bucket", download);
        return new Asset(response.readAllBytes(), HttpHeaders.EMPTY);
      } catch (final IOException | S3Exception e) {
        LOGGER.error("Failed to retrieve asset [{}] from bucket", download, e);
        return null;
      }
    }
    final URI uri = this.getDownloadUrl(build, download);
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
          return new Asset(content, newHeaders);
        }
      }
    } catch (final HttpClientErrorException e) {
      LOGGER.error("Failed to retrieve asset [{}] from bucket [{}]", download, uri, e);
      final DownloadFailedException exception = new DownloadFailedException();
      exception.addSuppressed(e);
      throw exception;
    }
    return null;
  }
}
