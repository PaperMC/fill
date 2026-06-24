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
package io.papermc.fill.filter;

import io.papermc.fill.SharedConstants;
import io.papermc.fill.api.ApiRoute;
import io.papermc.fill.api.ApiVersion;
import io.papermc.fill.exception.SunsetException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@NullMarked
public class SunsetFilter extends OncePerRequestFilter {
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Autowired
  public SunsetFilter(
    final HandlerExceptionResolver handlerExceptionResolver
  ) {
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  protected void doFilterInternal(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final FilterChain filterChain
  ) throws IOException, ServletException {
    if (ApiRoute.isApiRoute(request, ApiVersion.V2)) {
      // https://datatracker.ietf.org/doc/html/rfc8594
      response.setHeader("Sunset", DateTimeFormatter.RFC_1123_DATE_TIME.format(SharedConstants.API_V2_SUNSET.atZone(ZoneOffset.UTC)));

      if (Instant.now().isAfter(SharedConstants.API_V2_SUNSET)) {
        this.handlerExceptionResolver.resolveException(request, response, null, new SunsetException());
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
