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

import io.papermc.fill.util.crypto.HashAlgorithm;
import org.jspecify.annotations.NullMarked;

@NullMarked
public enum MavenHashType {
  MD5("md5", HashAlgorithm.MD5),
  SHA1("sha1", HashAlgorithm.SHA1),
  SHA256("sha256", HashAlgorithm.SHA256),
  SHA512("sha512", HashAlgorithm.SHA512);

  private final String extension;
  private final HashAlgorithm algorithm;

  MavenHashType(
    final String extension,
    final HashAlgorithm algorithm
  ) {
    this.extension = extension;
    this.algorithm = algorithm;
  }

  public String extension() {
    return this.extension;
  }

  public HashAlgorithm algorithm() {
    return this.algorithm;
  }
}
