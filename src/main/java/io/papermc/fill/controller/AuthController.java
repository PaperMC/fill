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

import io.papermc.fill.model.response.ErrorResponse;
import io.papermc.fill.model.response.LoginResponse;
import io.papermc.fill.service.JwtService;
import io.papermc.fill.util.http.Responses;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
public class AuthController {
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

  @PostMapping("/auth/login")
  public ResponseEntity<?> login(
    @RequestParam
    final String username,
    @RequestParam
    final String password
  ) {
    final Authentication authentication;
    try {
      authentication = this.authentication.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    } catch (final BadCredentialsException e) {
      return Responses.unauthorized(new ErrorResponse("bad_credentials"));
    }
    final UserDetails user = this.users.loadUserByUsername(authentication.getName());
    return Responses.ok(new LoginResponse(true, user.getUsername(), this.jwts.createJwt(user)));
  }
}
