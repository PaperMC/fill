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
package io.papermc.fill.util.discord;

import discord4j.core.object.component.Container;
import discord4j.core.object.component.ICanBeUsedInContainerComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.rest.util.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import reactor.util.annotation.Nullable;

@NullMarked
public final class Components {
  public static final Color COLOR_BLUE = Color.of(0x4fc3f7);
  public static final Color COLOR_GREEN = Color.of(0x4db6ac);
  public static final Color COLOR_PINK = Color.of(0xf06292);
  public static final Color COLOR_ORANGE = Color.of(0xffb74d);
  public static final Color COLOR_PURPLE = Color.of(0x7e57c2);
  public static final Color COLOR_RED = Color.of(0xff6f61);
  public static final Color COLOR_YELLOW = Color.of(0xfff176);

  private Components() {
  }

  public static <C extends MessageComponent & ICanBeUsedInContainerComponent> Container container(final OptionalInt id, final Consumer<Builder<C>> consumer, final @Nullable Color color, final boolean spoiler) {
    final List<C> components = createList(consumer);
    return id.isPresent()
      ? Container.of(id.getAsInt(), color, spoiler, components)
      : Container.of(color, spoiler, components);
  }

  private static <T> List<T> createList(final Consumer<Builder<T>> consumer) {
    final List<T> list = new ArrayList<>();
    consumer.accept(list::add);
    return list;
  }

  @NullMarked
  public interface Builder<T> {
    void add(final T value);
  }
}
