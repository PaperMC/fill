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
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@NullMarked
public final class ApiRoute {
  private ApiRoute() {
  }

  public static boolean isApiRoute(final WebRequest request, final ApiVersion version) {
    if (request instanceof ServletWebRequest) {
      return isApiRoute((ServletWebRequest) request, version);
    }
    return false;
  }

  public static boolean isApiRoute(final ServletWebRequest request, final ApiVersion version) {
    return isApiRoute(request.getRequest(), version);
  }

  public static boolean isApiRoute(final HttpServletRequest request, final ApiVersion version) {
    final String uri = request.getRequestURI();
    return uri.startsWith(version.routePrefix());
  }

  public static boolean isApiRoute(final HttpServletRequest request) {
    final String uri = request.getRequestURI();
    return uri.startsWith(SharedConstants.API_V1_ROUTE_PREFIX) ||
           uri.startsWith(SharedConstants.API_V2_ROUTE_PREFIX) ||
           uri.startsWith(SharedConstants.API_V3_ROUTE_PREFIX);
  }
}
