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

import io.jsonwebtoken.Claims;
import io.papermc.fill.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@NullMarked
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER = "Bearer ";

  private final JwtService jwts;
  private final UserDetailsService users;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Autowired
  public JwtAuthenticationFilter(
    final JwtService jwts,
    final UserDetailsService users,
    final HandlerExceptionResolver handlerExceptionResolver
  ) {
    this.jwts = jwts;
    this.users = users;
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  protected void doFilterInternal(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final FilterChain filterChain
  ) throws IOException, ServletException {
    final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      try {
        final String jwt = header.substring(BEARER.length());
        final Claims claims = this.jwts.parseClaims(jwt);
        if (claims != null) {
          final String username = claims.getSubject();
          if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            final UserDetails user = this.users.loadUserByUsername(username);
            if (this.jwts.areClaimsValidFor(claims, user)) {
              final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
              );
              token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(token);
            }
          }
        }
      } catch (final Exception e) {
        this.handlerExceptionResolver.resolveException(request, response, null, e);
      }
    }

    filterChain.doFilter(request, response);
  }
}
