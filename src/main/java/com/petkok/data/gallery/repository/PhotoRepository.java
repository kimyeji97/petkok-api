package com.petkok.data.gallery.repository;

import com.petkok.data.gallery.entity.Photo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 갤러리 사진 저장소. 정렬은 언제나 {@code created_at desc, id desc} 다(계획서 범위 — "목록 (커서, `created_at` desc)") —
 * 인덱스 {@code idx_photos_pet_created_at (pet_id, created_at desc)} 를 탄다.
 *
 * <p>⚠️ <b>사진 조회는 반드시 {@code pet_id} 를 함께 건다</b>(REQ-10 D6 과 동일 관례). {@code findById} 만 쓰면 남의 펫의 사진
 * id 를 내 펫 경로로 불러도 200 이 나간다 — 가드는 펫만 보기 때문이다. 검증 계약 REQ-11-21 · 22.
 */
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

  Optional<Photo> findByIdAndPetId(UUID id, UUID petId);

  /** 첫 페이지. {@code Pageable} 은 크기만 쓴다(정렬은 쿼리에 고정). */
  @Query("select p from Photo p where p.petId = :petId order by p.createdAt desc, p.id desc")
  List<Photo> findFirstPage(@Param("petId") UUID petId, Pageable pageable);

  /**
   * keyset 다음 페이지 — {@code (created_at, id)} 가 커서보다 <b>작은</b> 것. {@code id} 타이브레이크가 없으면 같은 시각 여러
   * 건에서 누락·중복이 난다. 검증 계약 REQ-11-14 · 15.
   */
  @Query(
      "select p from Photo p where p.petId = :petId"
          + " and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))"
          + " order by p.createdAt desc, p.id desc")
  List<Photo> findPageAfter(
      @Param("petId") UUID petId,
      @Param("createdAt") OffsetDateTime createdAt,
      @Param("id") UUID id,
      Pageable pageable);
}
