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
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@NullMarked
public class GraphqlCommitTest {
  private static final String SHA = "afa6c8b3a2fae95785dc7d9685a57835d703ac88";

  @Test
  public void testCommitUrl() {
    final Commit commit = new Commit(SHA, Instant.EPOCH, "Test");
    final GraphqlCommit graphqlCommit = GraphqlCommit.from(commit, new GitRepository("PaperMC/Paper"));

    assertEquals(commit.sha(), graphqlCommit.sha());
    assertEquals(commit.message(), graphqlCommit.message());
    assertEquals("https://github.com/PaperMC/Paper/commit/" + SHA, graphqlCommit.url());
  }

  @Test
  public void testCommitWithoutRepositoryHasNoUrl() {
    final GraphqlCommit graphqlCommit = GraphqlCommit.from(new Commit(SHA, Instant.EPOCH, "Test"), null);

    assertNull(graphqlCommit.url());
  }
}
