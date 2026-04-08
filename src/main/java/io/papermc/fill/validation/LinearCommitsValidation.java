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
package io.papermc.fill.validation;

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.exception.CommitOrderValidationException;
import io.papermc.fill.model.Commit;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
@VisibleForTesting
public final class LinearCommitsValidation {
  private LinearCommitsValidation() {
  }

  public static void validate(final List<Commit> commits) throws CommitOrderValidationException {
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
}
