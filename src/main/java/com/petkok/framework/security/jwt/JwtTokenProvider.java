package com.petkok.framework.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    return create(userId, TokenType.ACCESS, accessValidityMs, null);
  }

  /**
   * refresh 토큰을 발급한다. <b>매번 다른 문자열임이 보장된다</b> — {@code jti} 에 랜덤 UUID 가 들어가기 때문이다.
   *
   * <p>⚠️ <b>{@code jti} 가 없으면 로테이션이 깨진다.</b> {@code iat}/{@code exp} 는 초 단위라 같은 초에 재발급하면 subject ·
   * type 이 같아 <b>이전 토큰과 완전히 같은 문자열</b>이 나온다. 그러면 새 토큰의 해시가 방금 revoke 한 행과 겹쳐 {@code
   * uq_refresh_tokens_token_hash} 를 위반하거나, 발급 즉시 revoke 된 토큰을 클라이언트에 주게 된다 (검증 계약 REQ-07-13).
   *
   * <p>access 토큰에는 넣지 않았다 — 저장·조회 대상이 아니라 충돌이 문제가 되지 않는다.
   */
  public String createRefreshToken(UUID userId) {
    return create(userId, TokenType.REFRESH, refreshValidityMs, UUID.randomUUID().toString());
  }

  private String create(UUID userId, TokenType type, long validityMs, String jwtId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityMs);
    var builder =
        Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_TYPE, type.name())
            .issuedAt(now)
            .expiration(expiry);
    if (jwtId != null) {
      builder.id(jwtId);
    }
    return builder.signWith(key).compact();
  }

  public UUID getUserId(String token) {
    return UUID.fromString(parse(token).getSubject());
  }

  public boolean isAccessToken(String token) {
    return TokenType.ACCESS.name().equals(parse(token).get(CLAIM_TYPE, String.class));
  }

  /**
   * 토큰의 만료 시각.
   *
   * <p>{@code refresh_tokens.expires_at} 을 이 값으로 채운다. TTL 을 저장 쪽에서 다시 계산하면 JWT 의 {@code exp} 와 어긋날
   * 수 있어, <b>토큰 자신이 들고 있는 값</b>을 그대로 쓴다.
   */
  public LocalDateTime getExpiresAt(String token) {
    return LocalDateTime.ofInstant(
        parse(token).getExpiration().toInstant(), ZoneId.systemDefault());
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
