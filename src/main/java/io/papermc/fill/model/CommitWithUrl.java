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
package io.papermc.fill.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Schema(name = "Commit")
public record CommitWithUrl(
  @Schema(pattern = "\\b[0-9a-f]{40}\\b")
  String sha,
  Instant time,
  String message,
  @Nullable String url
) {
  public static CommitWithUrl of(final Commit commit, final @Nullable String url) {
    return new CommitWithUrl(commit.sha(), commit.time(), commit.message(), url);
  }

  public static String getShortSha(final CommitWithUrl commit) {
    return commit.sha().substring(0, 7);
  }

  public String summary() {
    return this.message.split("\\R")[0];
  }
}
