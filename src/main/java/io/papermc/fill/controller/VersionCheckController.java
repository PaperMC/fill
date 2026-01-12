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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.papermc.fill.model.request.VersionCheckRequest;
import io.papermc.fill.model.response.VersionCheckResponse;
import io.papermc.fill.service.VersionCheckService;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@NullMarked
@RestController
public class VersionCheckController {
  private final LoadingCache<VersionCheckRequest, VersionCheckResponse> cache;

  @Autowired
  public VersionCheckController(
    final VersionCheckService service
  ) {
    this.cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .build(service::check);
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    path = "/version-check"
  )
  public ResponseEntity<?> check(
    @RequestBody
    final VersionCheckRequest request
  ) {
    return Responses.ok(this.cache.get(request));
  }
}
