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
package io.papermc.fill.graphql;

import io.papermc.fill.model.Commit;
import io.papermc.fill.util.git.GitRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record GraphqlCommit(
  String sha,
  String message,
  @Nullable String url
) {
  public static GraphqlCommit from(final Commit commit, final @Nullable GitRepository repository) {
    final String url = repository == null ? null : repository.commitUrl(commit.sha());
    return new GraphqlCommit(commit.sha(), commit.message(), url);
  }
}
