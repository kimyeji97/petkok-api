package com.petkok.data.shed.repository;

import com.petkok.data.shed.entity.ShedRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 탈피 기록 저장소. 정렬은 {@code shed_date desc, id desc} (D8) — 인덱스 {@code idx_shed_pet_date}. ⚠️ 기록 조회는
 * 반드시 {@code pet_id} 를 함께 건다 (D6). {@code WeightLogRepository} 와 같은 형태.
 */
public interface ShedRecordRepository extends JpaRepository<ShedRecord, UUID> {

  Optional<ShedRecord> findByIdAndPetId(UUID id, UUID petId);

  /** 첫 페이지. 탈피 예측(최근 3건)도 이 메서드를 {@code Pageable(0, 3)} 으로 재사용한다. */
  @Query("select s from ShedRecord s where s.petId = :petId order by s.shedDate desc, s.id desc")
  List<ShedRecord> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  @Query(
      "select s from ShedRecord s where s.petId = :petId"
          + " and (s.shedDate < :shedDate or (s.shedDate = :shedDate and s.id < :id))"
          + " order by s.shedDate desc, s.id desc")
  List<ShedRecord> findPageAfter(
      @Param("petId") UUID petId,
      @Param("shedDate") LocalDate shedDate,
      @Param("id") UUID id,
      Pageable pageable);
}
