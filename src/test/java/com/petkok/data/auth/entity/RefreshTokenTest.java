package com.petkok.data.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * refresh 토큰 엔티티 매핑 · 무효화 계약. 검증 계약 REQ-07-08 · 19 · 20 (PLAN-REQ-07 § 검증 계약).
 *
 * <p>{@code token_hash} 는 SHA-256 hex 64자 고정이라 {@code varchar(64)} 로 좁혔다(REQ-07-07 이 그 전제를 지킨다).
 * <b>UNIQUE 인덱스가 걸린 컬럼</b>이라 길이를 늘리면 인덱스까지 따라 바뀐다.
 *
 * <p>이 값은 {@code V2__refresh_tokens.sql} 과 반드시 일치해야 한다 — 어긋나면 {@code ddl-auto: validate} 가 기동을 막는다.
 * 그 실패는 애플리케이션이 뜬 뒤가 아니라 <b>기동 시점</b>에 나므로, 여기서 먼저 잡는 편이 싸다.
 */
class RefreshTokenTest {

  private static final OffsetDateTime REVOKED_AT =
      OffsetDateTime.of(2026, 7, 30, 10, 0, 0, 0, ZoneOffset.UTC);

  private static RefreshToken token() {
    return RefreshToken.of(UUID.randomUUID(), "0".repeat(64), REVOKED_AT.plusDays(14));
  }

  @Test
  @DisplayName("[REQ-07-08] token_hash 컬럼 길이는 64다")
  void req_07_08_tokenHashColumnLengthIs64() throws ReflectiveOperationException {
    Field field = RefreshToken.class.getDeclaredField("tokenHash");

    assertThat(field.getAnnotation(Column.class).length()).isEqualTo(64);
  }

  @Test
  @DisplayName("[REQ-07-19] revoke 하면 revoked_at 이 찍힌다")
  void req_07_19_revokeMarksRevokedAt() {
    RefreshToken token = token();

    token.revoke(REVOKED_AT);

    assertThat(token.getRevokedAt()).isEqualTo(REVOKED_AT);
  }

  /**
   * 재사용 감지는 "언제 revoke 됐는가"를 근거로 삼는다. 두 번째 호출이 시각을 덮어쓰면 <b>탈취 시점이 지워지고</b> 정상 로그아웃과 구분되지 않는다. 로테이션
   * · 로그아웃 · 재사용 감지가 같은 행에 연달아 닿을 수 있어 실제로 두 번 불린다.
   */
  @Test
  @DisplayName("[REQ-07-20] 이미 revoke 된 토큰을 다시 revoke 해도 최초 시각을 유지한다")
  void req_07_20_revokeKeepsFirstTimestamp() {
    RefreshToken token = token();
    token.revoke(REVOKED_AT);

    token.revoke(REVOKED_AT.plusHours(1));

    assertThat(token.getRevokedAt()).isEqualTo(REVOKED_AT);
  }
}
