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
package io.papermc.fill.s3;

import java.net.URI;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@NullMarked
public interface S3Configuration {
  @Nullable URI endpoint();

  String region();

  String accessKeyId();

  String secretAccessKey();

  String bucket();

  boolean usePathStyleAccess();

  boolean useS3v4Signer();

  static S3Client createClient(final S3Configuration properties) {
    final S3ClientBuilder client = S3Client.builder();
    client.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())));
    final URI endpoint = properties.endpoint();
    if (endpoint != null) {
      client.endpointOverride(endpoint);
    }
    client.region(Region.of(properties.region()));
    client.serviceConfiguration(configuration -> {
      if (properties.usePathStyleAccess()) {
        configuration.pathStyleAccessEnabled(true);
      }
    });
    client.overrideConfiguration(configuration -> {
      if (properties.useS3v4Signer()) {
        configuration.putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create());
      }
    });
    return client.build();
  }
}
