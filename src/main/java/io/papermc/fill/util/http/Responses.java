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
package io.papermc.fill.util.http;

import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@NullMarked
public final class Responses {
  private Responses() {
  }

  public static <R> ResponseEntity<R> ok(final R body) {
    return create(HttpStatus.OK, body);
  }

  public static <R> ResponseEntity<R> ok(final R body, final CacheControl cache) {
    return ok(body, headers -> headers.setCacheControl(cache));
  }

  public static <R> ResponseEntity<R> ok(final R body, final Consumer<HttpHeaders> consumer) {
    return create(HttpStatus.OK, body, consumer);
  }

  public static <R> ResponseEntity<R> created(final R body) {
    return create(HttpStatus.CREATED, body);
  }

  public static <R> ResponseEntity<R> badRequest(final R body) {
    return create(HttpStatus.BAD_REQUEST, body);
  }

  public static <R> ResponseEntity<R> unauthorized(final R body) {
    return create(HttpStatus.UNAUTHORIZED, body);
  }

  public static <R> ResponseEntity<R> forbidden(final R body) {
    return create(HttpStatus.FORBIDDEN, body);
  }

  public static <R> ResponseEntity<R> notFound(final R body) {
    return create(HttpStatus.NOT_FOUND, body);
  }

  public static <R> ResponseEntity<R> conflict(final R body) {
    return create(HttpStatus.CONFLICT, body);
  }

  public static <R> ResponseEntity<R> gone(final R body) {
    return create(HttpStatus.GONE, body);
  }

  public static <R> ResponseEntity<R> tooManyRequests(final R body) {
    return create(HttpStatus.TOO_MANY_REQUESTS, body);
  }

  public static <R> ResponseEntity<R> error(final R body) {
    return create(HttpStatus.INTERNAL_SERVER_ERROR, body);
  }

  public static <R> ResponseEntity<R> create(final HttpStatusCode status, final @Nullable R body) {
    return new ResponseEntity<>(body, status);
  }

  public static <R> ResponseEntity<R> create(final HttpStatusCode status, final @Nullable R body, final Consumer<HttpHeaders> consumer) {
    final HttpHeaders headers = new HttpHeaders();
    consumer.accept(headers);
    return new ResponseEntity<>(body, headers, status);
  }
}
