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
import java.time.Duration;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
public class S3ConfigurationTest {
  @Test
  void createsSigV4PresignedUploadUrl() {
    try (final S3Presigner presigner = S3Configuration.createPresigner(new TestConfiguration())) {
      final PresignedPutObjectRequest request = presigner.presignPutObject(PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(5))
        .putObjectRequest(PutObjectRequest.builder()
          .bucket("fill")
          .key("staging/test/file.jar")
          .contentLength(10L)
          .contentMD5("6Afx/PgtEy+bsBjKZzihnw==")
          .contentType("application/java-archive")
          .metadata(Map.of("sha256", "test-sha256"))
          .build())
        .build());

      assertTrue(request.url().getQuery().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertEquals("10", request.signedHeaders().get("content-length").getFirst());
      assertEquals("6Afx/PgtEy+bsBjKZzihnw==", request.signedHeaders().get("content-md5").getFirst());
      assertEquals("application/java-archive", request.signedHeaders().get("content-type").getFirst());
      assertEquals("test-sha256", request.signedHeaders().get("x-amz-meta-sha256").getFirst());
    }
  }

  private record TestConfiguration() implements S3Configuration {
    @Override
    public URI endpoint() {
      return URI.create("https://example.invalid");
    }

    @Override
    public String region() {
      return "auto";
    }

    @Override
    public String accessKeyId() {
      return "access-key";
    }

    @Override
    public String secretAccessKey() {
      return "secret-key";
    }

    @Override
    public String bucket() {
      return "fill";
    }

    @Override
    public boolean usePathStyleAccess() {
      return true;
    }

    @Override
    public boolean useS3v4Signer() {
      return false;
    }
  }
}
