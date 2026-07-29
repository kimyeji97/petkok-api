package com.petkok.data.user.repository;

import com.petkok.data.user.entity.UserSocialAccount;
import com.petkok.data.user.enums.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

  /**
   * {@code (provider, provider_user_id)} 로 소셜 계정을 찾는다. <b>이 조합이 유일한 사용자 식별자</b>다 — 카카오는 이메일을 주지 않아
   * 이메일 기반 조회가 성립하지 않는다.
   */
  Optional<UserSocialAccount> findByProviderAndProviderUserId(
      SocialProvider provider, String providerUserId);
}
