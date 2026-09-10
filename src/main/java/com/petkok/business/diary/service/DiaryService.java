package com.petkok.business.diary.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.diary.dto.DiaryCreateRequest;
import com.petkok.data.diary.dto.DiaryResponse;
import com.petkok.data.diary.dto.DiaryUpdateRequest;
import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.diary.enums.ConditionTag;
import com.petkok.data.diary.repository.DiaryEntryRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import com.petkok.framework.port.PhotoLookup;
import com.petkok.framework.port.PhotoSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다이어리 CRUD + {@code condition_tag} 필터 목록. 검증 계약 REQ-10-94 ~ 114 (PLAN-REQ-10 § 검증 계약). 종 제한 없음 —
 * 모든 종에 열려 있다({@code ShedService} 와 달리 {@code validateGecko} 가 없다).
 *
 * <p>{@code entry_date} 는 미래 불가(Phase 5 미결 확정 — KST 자정, ADR-0002 계산 존). {@code Clock} 을 주입받아 무인자
 * {@code now()} 를 쓰지 않는다(REQ-16 D5).
 */
@Slf4j
@Service
public class DiaryService {

  private static final int DIARY_MAX_LIMIT = 50;

  private final PetAccessGuard petAccessGuard;
  private final DiaryEntryRepository diaryEntryRepository;
  private final CursorCodec cursorCodec;
  private final Clock clock;
  private final PhotoLookup photoLookup;

  public DiaryService(
      PetAccessGuard petAccessGuard,
      DiaryEntryRepository diaryEntryRepository,
      CursorCodec cursorCodec,
      Clock clock,
      PhotoLookup photoLookup) {
    this.petAccessGuard = petAccessGuard;
    this.diaryEntryRepository = diaryEntryRepository;
    this.cursorCodec = cursorCodec;
    this.clock = clock;
    this.photoLookup = photoLookup;
  }

  @Transactional
  public DiaryResponse create(UUID userId, UUID petId, DiaryCreateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    validateNotFuture(request.entryDate());
    DiaryEntry saved =
        diaryEntryRepository.save(
            DiaryEntry.of(
                pet.id(),
                request.title(),
                request.content(),
                request.conditionTag(),
                request.entryDate()));
    return toDetailResponse(saved);
  }

  /** 목록. {@code conditionTag} 가 있으면 필터 전용 쿼리를 쓴다. {@code limit} 은 최대 {@value #DIARY_MAX_LIMIT}. */
  @Transactional(readOnly = true)
  public CursorPage<DiaryResponse> list(
      UUID userId, UUID petId, CursorRequest request, ConditionTag conditionTag) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    int limit = Math.min(request.limit(), DIARY_MAX_LIMIT);
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<DiaryEntry> rows;
    if (request.hasCursor()) {
      DiaryCursor cursor = cursorCodec.decode(request.cursor(), DiaryCursor.class);
      rows =
          conditionTag == null
              ? diaryEntryRepository.findPageAfter(
                  pet.id(), cursor.entryDate(), cursor.id(), pageable)
              : diaryEntryRepository.findPageAfterByConditionTag(
                  pet.id(), conditionTag, cursor.entryDate(), cursor.id(), pageable);
    } else {
      rows =
          conditionTag == null
              ? diaryEntryRepository.findFirstPage(pet.id(), pageable)
              : diaryEntryRepository.findFirstPageByConditionTag(pet.id(), conditionTag, pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<DiaryEntry> page = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = null;
    if (hasNext) {
      DiaryEntry last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new DiaryCursor(last.getEntryDate(), last.getId()));
    }
    return CursorPage.of(page.stream().map(this::toListItemResponse).toList(), nextCursor, hasNext);
  }

  /** 수정 — 보낸 필드만 반영 (D10). */
  @Transactional
  public DiaryResponse update(UUID userId, UUID petId, UUID entryId, DiaryUpdateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    DiaryEntry entry = findOwnedEntry(pet.id(), entryId);

    LocalDate entryDate = request.entryDate() != null ? request.entryDate() : entry.getEntryDate();
    validateNotFuture(entryDate);

    entry.update(
        request.title() != null ? request.title() : entry.getTitle(),
        request.content() != null ? request.content() : entry.getContent(),
        request.conditionTag() != null ? request.conditionTag() : entry.getConditionTag(),
        entryDate);

    return toDetailResponse(entry);
  }

  @Transactional
  public void delete(UUID userId, UUID petId, UUID entryId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    diaryEntryRepository.delete(findOwnedEntry(pet.id(), entryId));
    log.info("DiaryEntry deleted. petId={}, entryId={}", petId, entryId);
  }

  private void validateNotFuture(LocalDate entryDate) {
    if (entryDate != null && entryDate.isAfter(LocalDate.now(clock))) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private DiaryEntry findOwnedEntry(UUID petId, UUID entryId) {
    return diaryEntryRepository
        .findByIdAndPetId(entryId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 상세(생성·수정 응답) — {@code photos}를 채우고 {@code photoCount}는 비워 둔다(REQ-10-108이 뒤집힌 완료 기준). 검증 계약
   * REQ-11-30.
   */
  private DiaryResponse toDetailResponse(DiaryEntry entry) {
    List<PhotoSummary> photos = photoLookup.findByDiaryEntryId(entry.getId());
    return new DiaryResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getTitle(),
        entry.getContent(),
        entry.getConditionTag(),
        entry.getEntryDate(),
        entry.getCreatedAt(),
        entry.getUpdatedAt(),
        photos,
        null);
  }

  /**
   * 목록 항목 — {@code photoCount}만 채운다(REQ-10-109가 뒤집힌 완료 기준). 목록의 매 항목마다 전체 사진을 실으면 응답이 불필요하게 커져
   * count만 쓴다(2026-09-09 결정, N+1 수용). 검증 계약 REQ-11-31.
   */
  private DiaryResponse toListItemResponse(DiaryEntry entry) {
    int photoCount = photoLookup.countByDiaryEntryId(entry.getId());
    return new DiaryResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getTitle(),
        entry.getContent(),
        entry.getConditionTag(),
        entry.getEntryDate(),
        entry.getCreatedAt(),
        entry.getUpdatedAt(),
        null,
        photoCount);
  }
}
