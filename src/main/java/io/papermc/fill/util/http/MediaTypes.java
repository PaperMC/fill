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
package io.papermc.fill.util.http;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;

@NullMarked
public final class MediaTypes {
  public static final String APPLICATION_JAVA_ARCHIVE_VALUE = "application/java-archive";
  public static final MediaType APPLICATION_JAVA_ARCHIVE = MediaType.parseMediaType(APPLICATION_JAVA_ARCHIVE_VALUE);

  private MediaTypes() {
  }
}
