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

import io.papermc.fill.configuration.properties.ApplicationApiProperties;
import io.papermc.fill.util.http.Responses;
import java.net.URI;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@ConditionalOnProperty("app.api.api-docs-url")
@Controller
@NullMarked
public class ApiDocsController {
  private final ApplicationApiProperties properties;

  @Autowired
  public ApiDocsController(final ApplicationApiProperties properties) {
    this.properties = properties;
  }

  @GetMapping({
    "/",
    "/swagger-ui/index.html"
  })
  public ResponseEntity<?> redirectToApiDocs() {
    return Responses.found(URI.create(this.properties.apiDocsUrl()));
  }
}
