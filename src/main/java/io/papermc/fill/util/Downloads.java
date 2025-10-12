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
package io.papermc.fill.util;

import io.papermc.fill.model.Download;
import io.papermc.fill.model.DownloadWithUrl;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Downloads {
  private Downloads() {
  }

  public static Map<String, DownloadWithUrl> map(final Map<String, Download> downloads, final Function<Download, DownloadWithUrl> mapper) {
    return downloads.entrySet()
      .stream()
      .map(entry -> {
        final String key = entry.getKey();
        final Download download = entry.getValue();
        return Map.entry(key, mapper.apply(download));
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
