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
package io.papermc.fill.notification;

import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class WebhookPublisherTest {
  @Test
  void signsWithDecodedStandardWebhookSecret() {
    final String signature = WebhookPublisher.createSignature(
      "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
      "fill_test",
      "1700000000",
      "{\"type\":\"build.published\"}".getBytes(StandardCharsets.UTF_8)
    );

    assertEquals("v1,gjBnzVZudyFih59/Knjh7oE1wC2z3CMPV2RkxEBJBQk=", signature);
  }
}
