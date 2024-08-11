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

import io.papermc.fill.exception.DiscontinuedException;
import io.swagger.v3.oas.annotations.Hidden;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Deprecated
@Hidden
@NullMarked
@RestController
public class Meta1Controller {
  @GetMapping({
    "/v1/{project:[a-z]+}",
    "/v1/{project:[a-z]+}/{version:[0-9pre.-]+}",
    "/v1/{project:[a-z]+}/{version:[0-9pre.-]+}/{build:\\d+}",
    "/v1/{project:[a-z]+}/{version:[0-9pre.-]+}/latest"
  })
  @SuppressWarnings("MVCPathVariableInspection")
  public ResponseEntity<?> gone() {
    throw new DiscontinuedException();
  }
}
