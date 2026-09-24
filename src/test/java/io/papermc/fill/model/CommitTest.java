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
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class CommitTest {
  @Test
  public void testGetShortSha() {
    final Commit commit = new Commit("afa6c8b3a2fae95785dc7d9685a57835d703ac88", Instant.now(), "This is a test.");
    assertEquals("afa6c8b", Commit.getShortSha(commit));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "This is a test.\r\nA what?\r\nA test.\r\nA what?\r\nA test.\r\nOh, a test.",
    "This is a test.\n\nA what?\n\nA test.\n\nA what?\n\nA test.\n\nOh, a test."
  })
  public void testSummary(final String message) {
    final Commit commit = new Commit("7aec9744ba1554e4d38febae4278e74a5e764414", Instant.now(), message);
    assertEquals("This is a test.", commit.summary());
  }
}
