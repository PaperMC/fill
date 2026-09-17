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

@NullMarked
public record GitRepository(
  @Nullable GitForge forge,
  @Nullable String host,
  String owner,
  String name
) {
  public GitRepository(final String owner, final String name) {
    this(null, null, owner, name);
  }

  @Override
  public GitForge forge() {
    return this.forge != null ? this.forge : GitForge.GITHUB;
  }

  @Override
  public String host() {
    if (this.host != null) {
      return this.host;
    }
    return switch (this.forge()) {
      case GITHUB -> "github.com";
    };
  }

  public String url() {
    return "https://" + this.host() + "/" + this.owner + "/" + this.name;
  }

  public String commitUrlTemplate() {
    return switch (this.forge()) {
      case GITHUB -> this.url() + "/commit/{sha}";
    };
  }

  public String commitUrl(final String sha) {
    return this.commitUrlTemplate().replace("{sha}", sha);
  }

  public String compareUrlTemplate() {
    return switch (this.forge()) {
      case GITHUB -> this.url() + "/compare/{base}...{head}";
    };
  }

  public String compareUrl(final String base, final String head) {
    return this.compareUrlTemplate().replace("{base}", base).replace("{head}", head);
  }
}
