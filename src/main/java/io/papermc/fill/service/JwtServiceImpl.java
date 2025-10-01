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
package io.papermc.fill.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.papermc.fill.configuration.properties.ApplicationSecurityProperties;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class JwtServiceImpl implements JwtService {
  private static final long EXPIRATION = Duration.ofHours(1).toMillis();
  private final ApplicationSecurityProperties properties;

  @Autowired
  public JwtServiceImpl(final ApplicationSecurityProperties properties) {
    this.properties = properties;
  }

  @Override
  public @Nullable String getUsername(final String token) {
    return this.getClaim(token, Claims::getSubject);
  }

  @Override
  public boolean isTokenValid(final UserDetails user, final String token) {
    final String username = this.getUsername(token);
    return username != null && username.equals(user.getUsername()) && !this.getClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T getClaim(final String token, final Function<Claims, T> getter) {
    final Claims claims = this.extractClaims(token);
    return getter.apply(claims);
  }

  private Claims extractClaims(final String token) {
    return Jwts.parser()
      .verifyWith(this.getSecretKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  @Override
  public String createJwt(final UserDetails user) {
    return this.createJwt(user, Map.of());
  }

  private String createJwt(final UserDetails user, final Map<String, Object> claims) {
    final long now = System.currentTimeMillis();
    return Jwts.builder()
      .claims(claims)
      .subject(user.getUsername())
      .issuedAt(new Date(now))
      .expiration(new Date(now + EXPIRATION))
      .signWith(this.getSecretKey(), Jwts.SIG.HS256)
      .compact();
  }

  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.properties.jwt().secret()));
  }
}
