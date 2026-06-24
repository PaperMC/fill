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
package io.papermc.fill.util.graphql;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.function.ThrowingFunction;

@NullMarked
public record CursorCodec<T>(
  ThrowingFunction<T, String> encoder,
  ThrowingFunction<String, T> decoder
) {
  public static final CursorCodec<Instant> INSTANT = new CursorCodec<>(Instant::toString, Instant::parse);
  public static final CursorCodec<Integer> INT = new CursorCodec<>(String::valueOf, Integer::parseInt);

  public String encode(final T value) {
    try {
      return toBase64(this.encoder.apply(value));
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException("Invalid cursor", e);
    }
  }

  public T decode(final String value) {
    try {
      return this.decoder.apply(fromBase64(value));
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException("Invalid cursor", e);
    }
  }

  private static String toBase64(final String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String fromBase64(final String value) {
    return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
  }
}
