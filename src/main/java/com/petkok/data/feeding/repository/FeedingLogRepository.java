package com.petkok.data.feeding.repository;

import com.petkok.data.feeding.entity.FeedingLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 급여 기록 저장소. 정렬은 {@code fed_at desc, id desc} (D8) — 인덱스 {@code idx_feeding_pet_fed_at}. ⚠️
 * 기록 조회는 반드시 {@code pet_id} 를 함께 건다 (D6). {@code ActivityLogRepository} 와 같은 형태.
 */
public interface FeedingLogRepository extends JpaRepository<FeedingLog, UUID> {

  Optional<FeedingLog> findByIdAndPetId(UUID id, UUID petId);

  @Query("select f from FeedingLog f where f.petId = :petId order by f.fedAt desc, f.id desc")
  List<FeedingLog> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  @Query(
      "select f from FeedingLog f where f.petId = :petId"
          + " and (f.fedAt < :fedAt or (f.fedAt = :fedAt and f.id < :id))"
          + " order by f.fedAt desc, f.id desc")
  List<FeedingLog> findPageAfter(
      @Param("petId") UUID petId,
      @Param("fedAt") OffsetDateTime fedAt,
      @Param("id") UUID id,
      Pageable pageable);

  /** 거식 스트릭 계산용 — 마지막 정상 급여(거식이 아닌 가장 최근 기록). 검증 계약 REQ-10-59 ~ 65. */
  Optional<FeedingLog> findFirstByPetIdAndIsRefusedFalseOrderByFedAtDesc(UUID petId);
}
