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

import io.papermc.fill.exception.AmbiguousPaginationException;
import io.papermc.fill.exception.ExcessivePaginationException;
import io.papermc.fill.exception.InvalidPaginationException;
import io.papermc.fill.exception.MissingPaginationBoundariesException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
public class CursorPaginatorTest {
  private static final String CONNECTION = "test";

  @Test
  public void testCheckConnectionParameters() {
    assertThrows(MissingPaginationBoundariesException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, null, null));
    assertThrows(AmbiguousPaginationException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, 1, 1));
    assertDoesNotThrow(() -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, 1, null));
    assertDoesNotThrow(() -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, null, 1));
    assertThrows(InvalidPaginationException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, -1, null));
    assertThrows(InvalidPaginationException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, null, -1));
    assertThrows(ExcessivePaginationException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, CursorPaginator.MAX_FIRST + 1, null));
    assertThrows(ExcessivePaginationException.class, () -> CursorPaginator.checkConnectionParameters(CONNECTION, null, null, null, CursorPaginator.MAX_LAST + 1));
  }
}
