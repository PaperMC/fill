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

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;

@NullMarked
public final class Headers {
  private static final Set<String> SHARED = Set.of(
    HttpHeaders.CACHE_CONTROL,
    HttpHeaders.CONTENT_LENGTH,
    HttpHeaders.CONTENT_TYPE,
    HttpHeaders.DATE,
    HttpHeaders.ETAG,
    HttpHeaders.LAST_MODIFIED
  );

  private Headers() {
  }

  public static HttpHeaders copySharedHeaders(final HttpHeaders oldHeaders) {
    return copyHeaders(oldHeaders, SHARED);
  }

  public static HttpHeaders copyHeaders(final HttpHeaders oldHeaders, final Set<String> keys) {
    final HttpHeaders newHeaders = new HttpHeaders();
    for (final String key : keys) {
      final List<String> values = oldHeaders.get(key);
      if (values != null) {
        newHeaders.put(key, values);
      }
    }
    return newHeaders;
  }
}
