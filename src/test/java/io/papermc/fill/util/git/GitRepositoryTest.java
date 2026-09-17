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
package io.papermc.fill.util.git;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class GitRepositoryTest {
  @Test
  public void testDefaultGitHub() {
    final GitRepository repository = new GitRepository("PaperMC", "Paper");
    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("github.com", repository.host());
    assertEquals("https://github.com/PaperMC/Paper", repository.url());
    assertEquals("https://github.com/PaperMC/Paper/commit/{sha}", repository.commitUrlTemplate());
    assertEquals("https://github.com/PaperMC/Paper/commit/abc1234", repository.commitUrl("abc1234"));
    assertEquals("https://github.com/PaperMC/Paper/compare/{base}...{head}", repository.compareUrlTemplate());
    assertEquals("https://github.com/PaperMC/Paper/compare/abc1234...def5678", repository.compareUrl("abc1234", "def5678"));
  }

  @Test
  public void testCustomHost() {
    final GitRepository repository = new GitRepository(GitForge.GITHUB, "github.papermc.io", "PaperMC", "Paper");
    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("github.papermc.io", repository.host());
    assertEquals("https://github.papermc.io/PaperMC/Paper", repository.url());
    assertEquals("https://github.papermc.io/PaperMC/Paper/commit/abc1234", repository.commitUrl("abc1234"));
    assertEquals("https://github.papermc.io/PaperMC/Paper/compare/abc1234...def5678", repository.compareUrl("abc1234", "def5678"));
  }
}
