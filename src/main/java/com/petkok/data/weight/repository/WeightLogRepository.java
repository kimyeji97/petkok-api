package com.petkok.data.weight.repository;

import com.petkok.data.weight.entity.WeightLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 체중 기록 저장소. 정렬은 언제나 {@code measured_at desc, id desc} 다 (D8) — 인덱스 {@code
 * idx_weight_pet_measured_at (pet_id, measured_at desc)} 를 탄다.
 *
 * <p>⚠️ <b>기록 조회는 반드시 {@code pet_id} 를 함께 건다</b> (D6). {@code findById} 만 쓰면 남의 펫의 기록 id 를 내 펫 경로로
 * 불러도 200 이 나간다 — 가드는 펫만 보기 때문이다. 검증 계약 REQ-10-08.
 */
public interface WeightLogRepository extends JpaRepository<WeightLog, UUID> {

  Optional<WeightLog> findByIdAndPetId(UUID id, UUID petId);

  /** 첫 페이지. {@code Pageable} 은 크기만 쓴다(정렬은 쿼리에 고정). */
  @Query("select w from WeightLog w where w.petId = :petId order by w.measuredAt desc, w.id desc")
  List<WeightLog> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  /**
   * keyset 다음 페이지 — {@code (measured_at, id)} 가 커서보다 <b>작은</b> 것. {@code id} 타이브레이크가 없으면 같은 날짜 여러
   * 건에서 누락·중복이 난다 (D8). 검증 계약 REQ-10-10.
   *
   * <p>크기 1 로 부르면 "직전 기록" 조회가 된다 (D3).
   */
  @Query(
      "select w from WeightLog w where w.petId = :petId"
          + " and (w.measuredAt < :measuredAt or (w.measuredAt = :measuredAt and w.id < :id))"
          + " order by w.measuredAt desc, w.id desc")
  List<WeightLog> findPageAfter(
      @Param("petId") UUID petId,
      @Param("measuredAt") LocalDate measuredAt,
      @Param("id") UUID id,
      Pageable pageable);
}
