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

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.exception.AmbiguousPaginationException;
import io.papermc.fill.exception.ExcessivePaginationException;
import io.papermc.fill.exception.InvalidPaginationException;
import io.papermc.fill.exception.MissingPaginationBoundariesException;
import io.papermc.fill.graphql.Connection;
import io.papermc.fill.graphql.Edge;
import io.papermc.fill.graphql.PageInfo;
import io.papermc.fill.model.OrderDirection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CursorPaginator<I, T> {
  @VisibleForTesting
  static final int MAX_FIRST = 100;
  @VisibleForTesting
  static final int MAX_LAST = 100;

  private final String name;
  private final Function<T, I> idGetter;
  private final Comparator<I> idComparator;
  private final CursorCodec<I> cursorCodec;

  public CursorPaginator(
    final String name,
    final Function<T, I> idGetter,
    final CursorCodec<I> cursorCodec,
    final Comparator<I> idComparator
  ) {
    this.name = name;
    this.idGetter = idGetter;
    this.idComparator = idComparator;
    this.cursorCodec = cursorCodec;
  }

  public Connection<T> paginate(
    final Stream<T> items,
    final @Nullable OrderDirection direction,
    final @Nullable String after,
    final @Nullable String before,
    final @Nullable Integer first,
    final @Nullable Integer last
  ) {
    return this.paginate(items.toList(), direction, after, before, first, last);
  }

  public Connection<T> paginate(
    final List<T> items,
    final @Nullable OrderDirection direction,
    final @Nullable String after,
    final @Nullable String before,
    final @Nullable Integer first,
    final @Nullable Integer last
  ) {
    checkConnectionParameters(this.name, after, before, first, last);

    final Comparator<I> comparator = this.resolveComparator(direction);

    final List<T> sortedItems = items
      .stream()
      .sorted((item1, item2) -> comparator.compare(this.idGetter.apply(item1), this.idGetter.apply(item2)))
      .toList();

    final List<T> filteredItems = this.applyCursorFilters(sortedItems, comparator, after, before);

    List<T> slice;
    boolean hasNextPage = false;
    boolean hasPreviousPage = false;

    if (first != null) {
      final int limit = Math.min(first + 1, filteredItems.size());
      slice = filteredItems.subList(0, limit);
      hasNextPage = slice.size() > first;
      if (hasNextPage) {
        slice = slice.subList(0, first);
      }
      hasPreviousPage = after != null;
    } else if (last != null) {
      final int size = filteredItems.size();
      final int start = Math.max(0, size - last - 1);
      slice = filteredItems.subList(start, size);
      hasPreviousPage = slice.size() > last;
      if (hasPreviousPage) {
        slice = slice.subList(1, slice.size());
      }
      hasNextPage = before != null;
    } else {
      slice = List.of();
    }

    final List<Edge<T>> edges = slice.stream()
      .map(item -> new Edge<>(item, this.cursorCodec.encode(this.idGetter.apply(item))))
      .toList();

    final PageInfo pageInfo = this.buildPageInfo(edges, hasPreviousPage, hasNextPage);

    return new Connection<>(edges, slice, pageInfo, items.size());
  }

  private Comparator<I> resolveComparator(final @Nullable OrderDirection direction) {
    if (direction == null) return this.idComparator;
    return direction == OrderDirection.ASC
      ? this.idComparator
      : this.idComparator.reversed();
  }

  private List<T> applyCursorFilters(
    final List<T> items,
    final Comparator<I> comparator,
    final @Nullable String after,
    final @Nullable String before
  ) {
    if (after != null || before != null) {
      Stream<T> filtered = items.stream();
      if (after != null) {
        final I afterId = this.cursorCodec.decode(after);
        filtered = filtered.filter(item -> comparator.compare(this.idGetter.apply(item), afterId) > 0);
      }
      if (before != null) {
        final I beforeId = this.cursorCodec.decode(before);
        filtered = filtered.filter(item -> comparator.compare(this.idGetter.apply(item), beforeId) < 0);
      }
      return filtered.toList();
    }
    return items;
  }

  @VisibleForTesting
  static void checkConnectionParameters(
    final String name,
    final @Nullable String after,
    final @Nullable String before,
    final @Nullable Integer first,
    final @Nullable Integer last
  ) {
    if (first == null && last == null) {
      throw new MissingPaginationBoundariesException(String.format("You must provide a `first` or `last` value to properly paginate the `%s` connection.", name));
    } else if (first != null && last != null) {
      throw new AmbiguousPaginationException(String.format("Passing both `first` and `last` to paginate the `%s` connection is not supported.", name));
    } else if (first != null && first < 0) {
      throw new InvalidPaginationException(String.format("`first` on the `%s` connection cannot be less than zero.", name));
    } else if (first != null && first > MAX_FIRST) {
      throw new ExcessivePaginationException(String.format("Requesting %d records on the `%s` connection exceeds the `first` limit of %d records.", first, name, MAX_FIRST));
    } else if (last != null && last < 0) {
      throw new InvalidPaginationException(String.format("`last` on the `%s` connection cannot be less than zero.", name));
    } else if (last != null && last > MAX_LAST) {
      throw new ExcessivePaginationException(String.format("Requesting %d records on the `%s` connection exceeds the `last` limit of %d records.", last, name, MAX_LAST));
    }
  }

  private PageInfo buildPageInfo(
    final List<Edge<T>> edges,
    final boolean hasPreviousPage,
    final boolean hasNextPage
  ) {
    if (edges.isEmpty()) {
      return PageInfo.EMPTY;
    }

    final String startCursor = edges.getFirst().cursor();
    final String endCursor = edges.getLast().cursor();

    return new PageInfo(startCursor, endCursor, hasPreviousPage, hasNextPage);
  }
}
