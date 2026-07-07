package com.petkok.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Access / Refresh JWT 발급·검증. subject = userId, claim "type" = ACCESS|REFRESH. */
@Component
public class JwtTokenProvider {

  public enum TokenType {
    ACCESS,
    REFRESH
  }

  private static final String CLAIM_TYPE = "type";

  private final SecretKey key;
  private final long accessValidityMs;
  private final long refreshValidityMs;

  public JwtTokenProvider(JwtProperties props) {
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    this.accessValidityMs = props.accessTokenValidityMs();
    this.refreshValidityMs = props.refreshTokenValidityMs();
  }

  public String createAccessToken(UUID userId) {
    return create(userId, TokenType.ACCESS, accessValidityMs);
  }

  public String createRefreshToken(UUID userId) {
    return create(userId, TokenType.REFRESH, refreshValidityMs);
  }

  private String create(UUID userId, TokenType type, long validityMs) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityMs);
    return Jwts.builder()
        .subject(userId.toString())
        .claim(CLAIM_TYPE, type.name())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public UUID getUserId(String token) {
    return UUID.fromString(parse(token).getSubject());
  }

  public boolean isAccessToken(String token) {
    return TokenType.ACCESS.name().equals(parse(token).get(CLAIM_TYPE, String.class));
  }

  public boolean validate(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
