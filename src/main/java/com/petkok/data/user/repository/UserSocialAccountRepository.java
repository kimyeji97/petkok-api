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

  /**
   * 탈퇴 시 해당 사용자의 소셜 연결을 <b>하드 삭제</b>한다 (PLAN-REQ-08 D1). 검증 계약 REQ-08-10.
   *
   * <p><b>소프트 딜리트가 아닌 이유는 선택이 아니라 제약이다</b> — {@code user_social_accounts} 에는 {@code deleted_at} 이
   * 없다({@link UserSocialAccount} 가 {@code BaseCreatedEntity} 를 상속).
   *
   * <p>⚠️ <b>이 행을 남기면 탈퇴가 조용히 깨진다.</b> {@code AuthService.findOrCreateUser} 는 {@code (provider,
   * provider_user_id)} 로 찾은 소셜 행의 유저를 <b>소프트 딜리트 여부를 보지 않고</b> 반환한다. 행이 남아 있으면 재로그인이 탈퇴한 계정으로 토큰을
   * 발급하고, {@code UNIQUE (provider, provider_user_id)} 때문에 새 계정 생성도 막혀 빠져나갈 길이 없다.
   */
  void deleteByUserId(UUID userId);
}
