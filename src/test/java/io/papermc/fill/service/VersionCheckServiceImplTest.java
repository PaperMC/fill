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

import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class VersionCheckServiceImplTest {
  @MethodSource("indexedTargets")
  @ParameterizedTest
  void returnsCorrectIndex_forVariousPositions(final List<Person> items, final String target, final int expected) {
    assertEquals(expected, VersionCheckServiceImpl.findDistance(items, target, Person::name));
  }

  static Stream<Arguments> indexedTargets() {
    final List<Person> items = List.of(
      new Person("alice"),
      new Person("bob")
    );
    return Stream.of(
      Arguments.of(items, "bob", 1),
      Arguments.of(items, "alice", 0)
    );
  }

  @NullMarked
  record Person(String name) {
  }
}
