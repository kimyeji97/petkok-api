package com.petkok.data.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * refresh 토큰 엔티티 매핑 계약. 검증 계약 REQ-07-08 (PLAN-REQ-07 § 검증 계약).
 *
 * <p>{@code token_hash} 는 SHA-256 hex 64자 고정이라 {@code varchar(64)} 로 좁혔다(REQ-07-07 이 그 전제를 지킨다).
 * <b>UNIQUE 인덱스가 걸린 컬럼</b>이라 길이를 늘리면 인덱스까지 따라 바뀐다.
 *
 * <p>이 값은 {@code V2__refresh_tokens.sql} 과 반드시 일치해야 한다 — 어긋나면 {@code ddl-auto: validate} 가 기동을 막는다.
 * 그 실패는 애플리케이션이 뜬 뒤가 아니라 <b>기동 시점</b>에 나므로, 여기서 먼저 잡는 편이 싸다.
 */
class RefreshTokenTest {

  @Test
  @DisplayName("[REQ-07-08] token_hash 컬럼 길이는 64다")
  void req_07_08_tokenHashColumnLengthIs64() throws ReflectiveOperationException {
    Field field = RefreshToken.class.getDeclaredField("tokenHash");

    assertThat(field.getAnnotation(Column.class).length()).isEqualTo(64);
  }
}
