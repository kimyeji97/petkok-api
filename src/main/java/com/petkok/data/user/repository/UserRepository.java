package com.petkok.data.user.repository;

import com.petkok.data.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  /** 소프트 딜리트된 사용자는 제외한다. 탈퇴한 계정이 살아 있는 것처럼 조회되면 안 된다. */
  Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
