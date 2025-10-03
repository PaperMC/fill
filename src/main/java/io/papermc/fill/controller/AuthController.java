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
package io.papermc.fill.controller;

import io.jsonwebtoken.Claims;
import io.papermc.fill.model.response.AuthErrorResponse;
import io.papermc.fill.model.response.AuthTokenResponse;
import io.papermc.fill.service.JwtService;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@NullMarked
@RestController
public class AuthController {
  private static final String GRANT_TYPE_PASSWORD = "password";
  private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

  private final AuthenticationManager authentication;
  private final UserDetailsService users;
  private final JwtService jwts;

  @Autowired
  public AuthController(
    final AuthenticationManager authentication,
    final UserDetailsService users,
    final JwtService jwts
  ) {
    this.authentication = authentication;
    this.users = users;
    this.jwts = jwts;
  }

  @CrossOrigin(methods = RequestMethod.POST)
  @PostMapping(
    path = "/auth/token",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public ResponseEntity<?> token(
    @RequestParam(name = "grant_type")
    final String grantType,
    @RequestParam(required = false)
    final @Nullable String username,
    @RequestParam(required = false)
    final @Nullable String password,
    @RequestParam(name = "refresh_token", required = false)
    final @Nullable String refreshToken
  ) {
    return switch (grantType) {
      case GRANT_TYPE_PASSWORD -> {
        if (username == null) {
          yield errorMissingRequiredParameter("username");
        }
        if (password == null) {
          yield errorMissingRequiredParameter("password");
        }
        yield this.password(username, password);
      }
      case GRANT_TYPE_REFRESH_TOKEN -> {
        if (refreshToken == null) {
          yield errorMissingRequiredParameter("refresh_token");
        }
        yield this.refreshToken(refreshToken);
      }
      default -> Responses.badRequest(new AuthErrorResponse(AuthErrorResponse.ERROR_UNSUPPORTED_GRANT_TYPE, "The grant type is not supported by this authorization server"));
    };
  }

  private ResponseEntity<?> password(final String username, final String password) {
    final Authentication authentication;
    try {
      authentication = this.authentication.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    } catch (final BadCredentialsException e) {
      return Responses.unauthorized(new AuthErrorResponse(AuthErrorResponse.ERROR_INVALID_GRANT, "Bad credentials"));
    }
    final UserDetails user = this.users.loadUserByUsername(authentication.getName());
    final String access = this.jwts.createAccessToken(user);
    final String refresh = this.jwts.createRefreshToken(user);
    return Responses.ok(new AuthTokenResponse(access, AuthTokenResponse.TOKEN_TYPE_BEARER, this.jwts.getAccessTokenLifetime().toSeconds(), refresh));
  }

  private ResponseEntity<?> refreshToken(final String token) {
    final Claims claims = this.jwts.parseClaims(token);
    if (claims != null) {
      final String username = claims.getSubject();
      if (username != null) {
        final UserDetails user = this.users.loadUserByUsername(username);
        if (this.jwts.areClaimsValidFor(claims, user)) {
          final String access = this.jwts.createAccessToken(user);
          final String refresh = this.jwts.createRefreshToken(user);
          return Responses.ok(new AuthTokenResponse(access, AuthTokenResponse.TOKEN_TYPE_BEARER, this.jwts.getAccessTokenLifetime().toSeconds(), refresh));
        }
      }
    }
    return Responses.unauthorized(new AuthErrorResponse(AuthErrorResponse.ERROR_INVALID_GRANT, "Refresh token is invalid or expired"));
  }

  private static ResponseEntity<?> errorMissingRequiredParameter(final String name) {
    return Responses.badRequest(new AuthErrorResponse(AuthErrorResponse.ERROR_INVALID_REQUEST, "Missing required parameter: " + name));
  }
}
