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

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Build extends Identified, Timestamped {
  Comparator<Build> COMPARATOR_NUMBER = Comparator.comparing(Build::number);
  Comparator<Build> COMPARATOR_NUMBER_REVERSE = COMPARATOR_NUMBER.reversed();

  int number();

  BuildChannel channel();

  static Predicate<Build> isChannel(final @Nullable BuildChannel channel) {
    return build -> channel == null || build.channel() == channel;
  }

  // descending
  List<Commit> commits();
}
