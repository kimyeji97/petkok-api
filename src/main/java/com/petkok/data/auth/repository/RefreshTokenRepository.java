package com.petkok.data.auth.repository;

import com.petkok.data.auth.entity.RefreshToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /**
   * 해시로 토큰 행을 찾는다. <b>revoke 된 행도 함께 반환해야 한다</b> — 재사용 감지가 "revoked_at 이 찍힌 토큰이 다시 왔는가"로 동작하므로, 여기서
   * revoke 된 행을 걸러내면 탈취를 그냥 "없는 토큰"으로 흘려보내게 된다.
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** 로그아웃·탈퇴·재사용 감지 시 해당 사용자의 유효 토큰을 한 번에 무효화한다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
  int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

  List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
