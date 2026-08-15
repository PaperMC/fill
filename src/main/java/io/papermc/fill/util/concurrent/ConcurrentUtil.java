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
package io.papermc.fill.util.concurrent;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConcurrentUtil {
  private ConcurrentUtil() {
  }

  /**
   * Attempts a bounded graceful shutdown of the given executor, falling back to an
   * immediate forced shutdown if the timeout elapses before all tasks complete.
   *
   * @param service the executor to shut down
   * @param timeout how long to wait for graceful termination before forcing it
   */
  public static void shutdownExecutor(final ExecutorService service, final Duration timeout) {
    shutdownExecutor(service, TimeUnit.NANOSECONDS, timeout.toNanos());
  }

  /**
   * Attempts a bounded graceful shutdown of the given executor, falling back to an
   * immediate forced shutdown if the timeout elapses before all tasks complete.
   *
   * @param service the executor to shut down
   * @param timeoutUnit the unit of {@code timeoutLength}
   * @param timeoutLength how long to wait for graceful termination before forcing it
   */
  public static void shutdownExecutor(final ExecutorService service, final TimeUnit timeoutUnit, final long timeoutLength) {
    service.shutdown();
    boolean didShutdown;
    try {
      didShutdown = service.awaitTermination(timeoutLength, timeoutUnit);
    } catch (final InterruptedException ignore) {
      didShutdown = false;
    }
    if (!didShutdown) {
      service.shutdownNow();
    }
  }
}
