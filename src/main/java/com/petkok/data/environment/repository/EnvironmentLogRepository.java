package com.petkok.data.environment.repository;

import com.petkok.data.environment.entity.EnvironmentLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게코 사육 환경 기록 저장소. 정렬은 {@code measured_at desc, id desc} — {@code FeedingLogRepository} 와 같은 형태. ⚠️
 * 기록 조회는 반드시 {@code pet_id} 를 함께 건다.
 */
public interface EnvironmentLogRepository extends JpaRepository<EnvironmentLog, UUID> {

  Optional<EnvironmentLog> findByIdAndPetId(UUID id, UUID petId);

  @Query(
      "select e from EnvironmentLog e where e.petId = :petId order by e.measuredAt desc, e.id desc")
  List<EnvironmentLog> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  @Query(
      "select e from EnvironmentLog e where e.petId = :petId"
          + " and (e.measuredAt < :measuredAt or (e.measuredAt = :measuredAt and e.id < :id))"
          + " order by e.measuredAt desc, e.id desc")
  List<EnvironmentLog> findPageAfter(
      @Param("petId") UUID petId,
      @Param("measuredAt") OffsetDateTime measuredAt,
      @Param("id") UUID id,
      Pageable pageable);

  /** 일간 평균 조회용 — 지정 날짜(KST 자정 경계)의 기록 전체. 검증 계약 REQ-20-09 ~ 15. */
  List<EnvironmentLog> findByPetIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
      UUID petId, OffsetDateTime from, OffsetDateTime toExclusive);
}
