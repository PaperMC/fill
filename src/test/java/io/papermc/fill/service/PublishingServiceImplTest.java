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
package io.papermc.fill.service;

import io.papermc.fill.model.Numbered;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class PublishingServiceImplTest {
  @Test
  void returnsRequestedBuildNumberWhenPresent() {
    final Supplier<List<? extends Numbered>> builds = () -> List.of(numbered(1), numbered(2), numbered(3));
    assertEquals(42, PublishingServiceImpl.getNextBuildNumber(builds, OptionalInt.of(42)));
  }

  @Test
  void returnsOneWhenThereAreNoBuilds() {
    final Supplier<List<? extends Numbered>> builds = () -> List.of();
    assertEquals(1, PublishingServiceImpl.getNextBuildNumber(builds, OptionalInt.empty()));
  }

  @Test
  void returnsOneMoreThanHighestBuildNumber() {
    final Supplier<List<? extends Numbered>> builds = () -> List.of(numbered(1), numbered(3), numbered(7));
    assertEquals(8, PublishingServiceImpl.getNextBuildNumber(builds, OptionalInt.empty()));
  }

  private static Numbered numbered(final int number) {
    return () -> number;
  }
}
