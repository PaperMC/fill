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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.papermc.fill.configuration.properties.ApplicationSecurityProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class JwtServiceImpl implements JwtService {
  private static final Duration LIFETIME_ACCESS = Duration.ofHours(1);
  private static final Duration LIFETIME_REFRESH = Duration.ofDays(1);

  private final SecretKey key;

  @Autowired
  public JwtServiceImpl(final ApplicationSecurityProperties properties) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
  }

  @Override
  public @Nullable Claims parseClaims(final String token) {
    try {
      return Jwts.parser()
        .verifyWith(this.key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
    } catch (final JwtException e) {
      return null;
    }
  }

  @Override
  public boolean areClaimsValidFor(final Claims claims, final UserDetails user) {
    final String username = claims.getSubject();
    final Date expiration = claims.getExpiration();
    return username != null && username.equals(user.getUsername()) && expiration != null && !expiration.before(new Date());
  }

  @Override
  public String createAccessToken(final UserDetails user) {
    return this.createToken(user, LIFETIME_ACCESS);
  }

  @Override
  public Duration getAccessTokenLifetime() {
    return LIFETIME_ACCESS;
  }

  @Override
  public String createRefreshToken(final UserDetails user) {
    return this.createToken(user, LIFETIME_REFRESH);
  }

  private String createToken(final UserDetails user, final Duration lifetime) {
    final Instant now = Instant.now();
    return Jwts.builder()
      .subject(user.getUsername())
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plus(lifetime)))
      .claims(Map.of())
      .signWith(this.key, Jwts.SIG.HS256)
      .compact();
  }
}
