package com.petkok.data.activity.repository;

import com.petkok.data.activity.entity.ActivityLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 활동 기록 저장소. 정렬은 {@code logged_at desc, id desc} (D8) — 인덱스 {@code idx_activity_pet_logged_at}. ⚠️
 * 기록 조회는 반드시 {@code pet_id} 를 함께 건다 (D6). {@code WeightLogRepository} 와 같은 형태.
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

  Optional<ActivityLog> findByIdAndPetId(UUID id, UUID petId);

  @Query("select a from ActivityLog a where a.petId = :petId order by a.loggedAt desc, a.id desc")
  List<ActivityLog> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  @Query(
      "select a from ActivityLog a where a.petId = :petId"
          + " and (a.loggedAt < :loggedAt or (a.loggedAt = :loggedAt and a.id < :id))"
          + " order by a.loggedAt desc, a.id desc")
  List<ActivityLog> findPageAfter(
      @Param("petId") UUID petId,
      @Param("loggedAt") OffsetDateTime loggedAt,
      @Param("id") UUID id,
      Pageable pageable);
}
