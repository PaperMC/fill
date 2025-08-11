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

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.exception.CommitOrderValidationException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Commit(
  @Schema(pattern = "\\b[0-9a-f]{40}\\b")
  String sha,
  Instant time,
  String message
) {
  public static String getShortSha(final Commit commit) {
    return commit.sha().substring(0, 7);
  }

  @VisibleForTesting
  public static void checkOrder(final List<Commit> commits) {
    for (int i = 0; i < commits.size() - 1; i++) {
      final Commit current = commits.get(i);
      final Commit next = commits.get(i + 1);
      if (current.time().isBefore(next.time())) {
        throw new CommitOrderValidationException(String.format(
          "Commit order validation failed: index %d (%s) comes before index %d (%s); expected newest-to-oldest",
          i,
          current,
          i + 1,
          next
        ));
      }
    }
  }

  public String summary() {
    return this.message.split("\\R")[0];
  }
}
