package com.careconnect.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  @Value("${careconnect.jwt.secret}")
  private String secret;

  private SecretKey key;

  @Value("${careconnect.production:false}")
  private boolean production;

  @PostConstruct
  void initialize() {
    if (secret.length() < 32)
      throw new IllegalStateException("CARECONNECT_JWT_SECRET must be at least 32 characters.");
    if (production
        && (secret.equals("local-development-only-careconnect-secret-change-me-2026")
            || secret.equals("replace-this-with-a-random-secret-at-least-32-characters")
            || secret.toLowerCase().contains("change-me")))
      throw new IllegalStateException(
          "Set a unique CARECONNECT_JWT_SECRET before starting the production profile.");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String issue(AppPrincipal user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.id())
        .claim("name", user.name())
        .claim("email", user.email())
        .claim("role", user.role())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(28800)))
        .signWith(key)
        .compact();
  }

  public AppPrincipal parse(String token) {
    Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return new AppPrincipal(
        c.getSubject(),
        c.get("name", String.class),
        c.get("email", String.class),
        c.get("role", String.class));
  }
}
