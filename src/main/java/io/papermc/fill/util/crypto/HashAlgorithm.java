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
package io.papermc.fill.util.crypto;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jspecify.annotations.NullMarked;

@NullMarked
public enum HashAlgorithm {
  MD5(Hashing.md5()),
  SHA1(Hashing.sha1()),
  SHA256(Hashing.sha256()),
  SHA512(Hashing.sha512()),
  ;

  private final HashFunction function;

  HashAlgorithm(final HashFunction function) {
    this.function = function;
  }

  public HashCode hash(final byte[] bytes) {
    return this.function.hashBytes(bytes);
  }
}
