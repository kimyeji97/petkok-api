package com.petkok.data.auth.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * refresh 토큰. 저장소 = DB 확정(2026-07-23) — Redis 는 로그아웃·탈퇴 시 즉시 무효화가 필요해 기각했다.
 *
 * <p><b>토큰 원문은 저장하지 않는다.</b> SHA-256 해시만 보관한다 — DB 가 유출되면 원문은 그대로 재사용 가능해지기 때문이다.
 *
 * <p><b>{@code userId} 는 {@code UUID} 컬럼이고 {@code @ManyToOne} 연관관계가 아니다.</b> {@code User} 는 {@code
 * data/user} 에 있어 연관관계를 걸면 {@code data/auth → data/user} 도메인 간 참조가 되어 ArchUnit 규칙에 걸린다. 토큰 행에서 User
 * 로 탐색할 일이 없어 잃는 것도 없다.
 */
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_refresh_tokens_token_hash", columnNames = "token_hash"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  /** SHA-256 hex 는 64자 고정이라 varchar(64) 면 충분하다. UNIQUE 가 걸린 컬럼이다. */
  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private LocalDateTime expiresAt;

  /** 로테이션·로그아웃·재사용 감지로 무효화된 시각. {@code null} = 유효. */
  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  private RefreshToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  /**
   * @param tokenHash 토큰 <b>원문이 아니라</b> SHA-256 hex 64자
   */
  public static RefreshToken of(UUID userId, String tokenHash, LocalDateTime expiresAt) {
    return new RefreshToken(userId, tokenHash, expiresAt);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt.isBefore(now);
  }

  /** 이미 revoke 된 토큰을 다시 revoke 해도 최초 시각을 유지한다 — 재사용 감지 시점을 덮어쓰지 않기 위해서다. */
  public void revoke(LocalDateTime now) {
    if (revokedAt == null) {
      this.revokedAt = now;
    }
  }
}
