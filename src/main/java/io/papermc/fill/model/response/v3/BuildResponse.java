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
package io.papermc.fill.model.response.v3;

import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.DownloadWithUrl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record BuildResponse(
  int id,
  Instant time,
  BuildChannel channel,
  List<Commit> commits,
  Map<String, DownloadWithUrl> downloads
) {
}
