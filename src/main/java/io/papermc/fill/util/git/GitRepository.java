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
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceCreator;

@NullMarked
public record GitRepository(
  @Nullable GitForge forge,
  String name
) {
  public GitRepository(final String name) {
    this((GitForge) null, name);
  }

  @PersistenceCreator
  public GitRepository(final @Nullable GitForge forge, final @Value("#root.owner") @Nullable String owner, final String name) {
    this(forge, owner != null ? owner + "/" + name : name);
  }

  public GitRepository(final String owner, final String name) {
    this(null, owner, name);
  }

  @Override
  public GitForge forge() {
    return this.forge != null ? this.forge : GitForge.GITHUB;
  }

  public String url() {
    return switch (this.forge()) {
      case GITHUB -> "https://github.com/" + this.name;
    };
  }

  public String commitUrl(final String sha) {
    return switch (this.forge()) {
      case GITHUB -> this.url() + "/commit/" + sha;
    };
  }

  public String compareUrl(final String base, final String head) {
    return switch (this.forge()) {
      case GITHUB -> this.url() + "/compare/" + base + "..." + head;
    };
  }
}
