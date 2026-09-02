package com.petkok.data.diary.repository;

import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.diary.enums.ConditionTag;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 다이어리 저장소. 정렬은 {@code entry_date desc, id desc} (D8). ⚠️ 기록 조회는 반드시 {@code pet_id} 를 함께 건다 (D6).
 * {@code condition_tag} 필터가 있는 목록은 별도 쿼리 메서드를 쓴다({@code ShedRecordRepository} 확장형).
 */
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, UUID> {

  Optional<DiaryEntry> findByIdAndPetId(UUID id, UUID petId);

  @Query("select d from DiaryEntry d where d.petId = :petId order by d.entryDate desc, d.id desc")
  List<DiaryEntry> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  @Query(
      "select d from DiaryEntry d where d.petId = :petId"
          + " and (d.entryDate < :entryDate or (d.entryDate = :entryDate and d.id < :id))"
          + " order by d.entryDate desc, d.id desc")
  List<DiaryEntry> findPageAfter(
      @Param("petId") UUID petId,
      @Param("entryDate") LocalDate entryDate,
      @Param("id") UUID id,
      Pageable pageable);

  @Query(
      "select d from DiaryEntry d where d.petId = :petId and d.conditionTag = :conditionTag"
          + " order by d.entryDate desc, d.id desc")
  List<DiaryEntry> findFirstPageByConditionTag(
      @Param("petId") UUID petId,
      @Param("conditionTag") ConditionTag conditionTag,
      Pageable pageable);

  @Query(
      "select d from DiaryEntry d where d.petId = :petId and d.conditionTag = :conditionTag"
          + " and (d.entryDate < :entryDate or (d.entryDate = :entryDate and d.id < :id))"
          + " order by d.entryDate desc, d.id desc")
  List<DiaryEntry> findPageAfterByConditionTag(
      @Param("petId") UUID petId,
      @Param("conditionTag") ConditionTag conditionTag,
      @Param("entryDate") LocalDate entryDate,
      @Param("id") UUID id,
      Pageable pageable);
}
