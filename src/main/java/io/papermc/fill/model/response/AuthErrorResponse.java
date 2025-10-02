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
package io.papermc.fill.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AuthErrorResponse(
  String error,
  @JsonProperty("error_description")
  String errorDescription
) {
  public static final String ERROR_INVALID_GRANT = "invalid_grant";
  public static final String ERROR_INVALID_REQUEST = "invalid_request";
  public static final String ERROR_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
}
