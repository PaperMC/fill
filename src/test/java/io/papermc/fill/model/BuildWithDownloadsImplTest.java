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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class BuildWithDownloadsImplTest {
  @Test
  public void testConstructorWithDownloads() {
    final Instant now = Instant.now();
    final Commit commit = new Commit("afa6c8b3a2fae95785dc7d9685a57835d703ac88", now, "Test");
    final Download download = new Download("paper.jar", "application/java-archive", new Checksums("sha256", "md5"), 1234);
    final BuildWithDownloadsImpl<Download> base = new BuildWithDownloadsImpl<>(
      "id",
      1,
      now,
      now,
      BuildChannel.STABLE,
      List.of(commit),
      Map.of("paper", download)
    );

    final BuildWithDownloadsImpl<Download> updated = new BuildWithDownloadsImpl<>(base, Map.of("paper", download));
    assertEquals(base.id(), updated.id());
    assertEquals(base.number(), updated.number());
    assertEquals(base.commits(), updated.commits());
    assertEquals(Map.of("paper", download), updated.downloads());
  }

  @Test
  public void testConstructorWithCustomCommitsAndDownloads() {
    final Instant now = Instant.now();
    final Commit commit = new Commit("afa6c8b3a2fae95785dc7d9685a57835d703ac88", now, "Test");
    final Commit commitWithUrl = commit.withUrl("https://github.com/PaperMC/Paper/commit/afa6c8b3a2fae95785dc7d9685a57835d703ac88");
    final Download download = new Download("paper.jar", "application/java-archive", new Checksums("sha256", "md5"), 1234);
    final BuildWithDownloadsImpl<Download> base = new BuildWithDownloadsImpl<>(
      "id",
      1,
      now,
      now,
      BuildChannel.STABLE,
      List.of(commit),
      Map.of("paper", download)
    );

    final BuildWithDownloadsImpl<Download> updated = new BuildWithDownloadsImpl<>(base, List.of(commitWithUrl), Map.of("paper", download));
    assertEquals(List.of(commitWithUrl), updated.commits());
    assertEquals("https://github.com/PaperMC/Paper/commit/afa6c8b3a2fae95785dc7d9685a57835d703ac88", updated.commits().getFirst().url());
  }
}
