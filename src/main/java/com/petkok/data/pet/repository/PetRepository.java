package com.petkok.data.pet.repository;

import com.petkok.data.pet.entity.Pet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ⚠️ <b>소프트 딜리트 필터는 메서드 이름에 명시한다</b> (PLAN-REQ-09 D6). {@code @SQLRestriction} 을 쓰지 않기로 했으므로 <b>여기서
 * 빠뜨리면 삭제된 펫이 조회된다</b> — {@code UserRepository} 와 같은 방식이다.
 */
public interface PetRepository extends JpaRepository<Pet, UUID> {

  /** 소유권 검사는 서비스가 한다 — 여기서 걸러 버리면 "없는 펫"과 "남의 펫"을 구분할 수 없다. */
  Optional<Pet> findByIdAndDeletedAtIsNull(UUID id);

  List<Pet> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
