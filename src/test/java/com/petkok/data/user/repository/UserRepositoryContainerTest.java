package com.petkok.data.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.user.entity.User;
import com.petkok.framework.config.TestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testcontainers 실물 DB에 {@link User}를 저장→조회해 JPA Auditing 배선이 실제로 동작하는지 확인한다(카카오 자동가입 경로 재현). 검증
 * 계약 REQ-18-02 (PLAN-REQ-18 § 검증 계약).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
class UserRepositoryContainerTest {

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("[REQ-18-02] User를 저장 후 조회하면 JPA Auditing이 채운 createdAt이 채워져 있다")
  void req_18_02_userSaveAndRetrieveFillsAuditingCreatedAt() {
    User saved = userRepository.save(User.of("게코사랑", null, null));

    assertThat(userRepository.findByIdAndDeletedAtIsNull(saved.getId()))
        .get()
        .extracting(User::getCreatedAt)
        .isNotNull();
  }
}
