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
package io.papermc.fill.api;

import io.papermc.fill.SharedConstants;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ApiVersion(
  String routePrefix
) {
  @Deprecated
  public static final ApiVersion V1 = new ApiVersion(SharedConstants.API_V1_ROUTE_PREFIX);
  @Deprecated
  public static final ApiVersion V2 = new ApiVersion(SharedConstants.API_V2_ROUTE_PREFIX);
  public static final ApiVersion V3 = new ApiVersion(SharedConstants.API_V3_ROUTE_PREFIX);
}
