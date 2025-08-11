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
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Version extends Timestamped {
  Comparator<Version> COMPARATOR_CREATED_AT = Comparator.comparing(Version::createdAt);
  Comparator<Version> COMPARATOR_CREATED_AT_REVERSE = COMPARATOR_CREATED_AT.reversed();

  String name();

  Support support();

  static Predicate<Version> isSupportStatus(final @Nullable SupportStatus status) {
    return version -> status == null || version.support().status() == status;
  }

  @Nullable Java java();
}
