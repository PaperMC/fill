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
package io.papermc.fill.event;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public final class AsyncEventPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEventPublisher.class);

  private final ApplicationEventPublisher delegate;
  private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
    Thread.ofVirtual().name("fill-event-", 0).factory()
  );

  public AsyncEventPublisher(final ApplicationEventPublisher delegate) {
    this.delegate = delegate;
  }

  public void publish(final FillEvent event) {
    this.executor.execute(() -> {
      try {
        this.delegate.publishEvent(event);
      } catch (final Exception exception) {
        LOGGER.error("Event listener failed for {}", event.type(), exception);
      }
    });
  }

  @PreDestroy
  public void close() {
    this.executor.shutdown();
  }
}
