package com.petkok.framework.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 포트의 위치 계약. 검증 계약 REQ-08-20 (PLAN-REQ-08 § 검증 계약).
 *
 * <p><b>이 인터페이스가 어디 있느냐가 Phase 3 설계의 전부다.</b> {@code business/user} 로 옮기면 필터가 그것을 참조하는 순간 {@code
 * FRAMEWORK_MUST_NOT_KNOW_DOMAIN} 과 {@code LAYER_DIRECTION} 이 <b>동시에</b> 깨진다.
 *
 * <p>ArchUnit 규칙이 이미 위반을 잡지만, 그것은 <b>"옮기면 빨간불"</b> 을 보장할 뿐 <b>"왜 여기 있어야 하는가"</b> 를 남기지 않는다. 이 케이스는
 * 위치 자체를 계약으로 고정한다.
 */
class UserStatusCheckerTest {

  @Test
  @DisplayName("[REQ-08-20] UserStatusChecker 는 framework.security 에 있다")
  void req_08_20_portLivesInFrameworkSecurity() {
    assertThat(UserStatusChecker.class.getPackageName()).isEqualTo("com.petkok.framework.security");
  }

  @Test
  @DisplayName("[REQ-08-20] 포트는 도메인 타입을 노출하지 않는다 (UUID · boolean 뿐)")
  void req_08_20_portDoesNotExposeDomainTypes() {
    boolean leaks =
        java.util.Arrays.stream(UserStatusChecker.class.getMethods())
            .flatMap(
                m ->
                    java.util.stream.Stream.concat(
                        java.util.Arrays.stream(m.getParameterTypes()),
                        java.util.stream.Stream.of(m.getReturnType())))
            .anyMatch(
                t ->
                    t.getName().startsWith("com.petkok.data.")
                        || t.getName().startsWith("com.petkok.business."));

    assertThat(leaks).isFalse();
  }
}
