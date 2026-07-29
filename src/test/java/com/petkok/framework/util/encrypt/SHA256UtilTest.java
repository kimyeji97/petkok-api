package com.petkok.framework.util.encrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * refresh 토큰 해시 길이 계약. 검증 계약 REQ-07-07 (PLAN-REQ-07 § 검증 계약).
 *
 * <p>{@code refresh_tokens.token_hash} 가 {@code varchar(64)} 인 근거가 이 고정 길이다. api-list.md 의 제안 스키마는
 * {@code varchar(255)} 였으나 SHA-256 hex 는 <b>64자 고정</b>이라 좁혔다. 해시 표현이 바뀌면(예: Base64 로 교체) 컬럼 길이를 넘겨
 * 저장이 깨지므로, 길이 자체를 계약으로 고정한다.
 */
class SHA256UtilTest {

  @Test
  @DisplayName("[REQ-07-07] 해시는 hex 소문자 64자 고정이다")
  void req_07_07_hashIsFixedLengthHex() throws NoSuchAlgorithmException {
    String hash = SHA256Util.encrypt("any-refresh-token-plaintext");

    assertThat(hash).matches("^[0-9a-f]{64}$");
  }
}
