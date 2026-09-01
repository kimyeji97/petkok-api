package com.petkok.business.feeding.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.feeding.dto.AnorexiaStreakResponse;
import com.petkok.data.feeding.dto.FeedingCreateRequest;
import com.petkok.data.feeding.dto.FeedingResponse;
import com.petkok.data.feeding.dto.FeedingUpdateRequest;
import com.petkok.data.feeding.entity.FeedingLog;
import com.petkok.data.feeding.repository.FeedingLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 급여 기록 CRUD + 거식 스트릭 조회. 검증 계약 REQ-10-43 ~ 67 (PLAN-REQ-10 § 검증 계약). {@code
 * ActivityService} 형태를 복제했다(D9).
 *
 * <p><b>{@code fed_at} 은 서버 현재 시각보다 미래면 거부한다</b>(PLAN-REQ-10 미결 질문 Phase 3) — 과거 소급은 허용한다.
 * 전용 {@code ErrorCode} 가 없어 기존 {@code INVALID_INPUT} 을 쓴다. <b>거식 스트릭은 게코 전용</b>이라 진입 시 종을
 * 검증한다 — 게코 외 종은 {@code FEATURE_NOT_SUPPORTED_SPECIES}.
 */
@Slf4j
@Service
public class FeedingService {

  private final PetAccessGuard petAccessGuard;
  private final FeedingLogRepository feedingLogRepository;
  private final CursorCodec cursorCodec;
  private final Clock clock;

  public FeedingService(
      PetAccessGuard petAccessGuard,
      FeedingLogRepository feedingLogRepository,
      CursorCodec cursorCodec,
      Clock clock) {
    this.petAccessGuard = petAccessGuard;
    this.feedingLogRepository = feedingLogRepository;
    this.cursorCodec = cursorCodec;
    this.clock = clock;
  }

  @Transactional
  public FeedingResponse create(UUID userId, UUID petId, FeedingCreateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    validateNotFuture(request.fedAt());
    FeedingLog saved =
        feedingLogRepository.save(
            FeedingLog.builder()
                .petId(pet.id())
                .foodType(request.foodType())
                .foodSize(request.foodSize())
                .amount(request.amount())
                .amountUnit(request.amountUnit())
                .isRefused(Boolean.TRUE.equals(request.isRefused()))
                .fedAt(request.fedAt())
                .memo(request.memo())
                .build());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CursorPage<FeedingResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<FeedingLog> rows;
    if (request.hasCursor()) {
      FeedingCursor cursor = cursorCodec.decode(request.cursor(), FeedingCursor.class);
      rows = feedingLogRepository.findPageAfter(pet.id(), cursor.fedAt(), cursor.id(), pageable);
    } else {
      rows = feedingLogRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<FeedingLog> page = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = null;
    if (hasNext) {
      FeedingLog last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new FeedingCursor(last.getFedAt(), last.getId()));
    }
    return CursorPage.of(page.stream().map(FeedingService::toResponse).toList(), nextCursor, hasNext);
  }

  /** 수정 — 보낸 필드만 반영 (D10). */
  @Transactional
  public FeedingResponse update(UUID userId, UUID petId, UUID logId, FeedingUpdateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    FeedingLog entry = findOwnedLog(pet.id(), logId);

    entry.update(
        request.foodType() != null ? request.foodType() : entry.getFoodType(),
        request.foodSize() != null ? request.foodSize() : entry.getFoodSize(),
        request.amount() != null ? request.amount() : entry.getAmount(),
        request.amountUnit() != null ? request.amountUnit() : entry.getAmountUnit(),
        request.isRefused() != null ? request.isRefused() : entry.isRefused(),
        request.fedAt() != null ? request.fedAt() : entry.getFedAt(),
        request.memo() != null ? request.memo() : entry.getMemo());

    return toResponse(entry);
  }

  @Transactional
  public void delete(UUID userId, UUID petId, UUID logId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    feedingLogRepository.delete(findOwnedLog(pet.id(), logId));
    log.info("FeedingLog deleted. petId={}, logId={}", petId, logId);
  }

  /** 🦎 게코 전용 — 거식 스트릭 현황. 검증 계약 REQ-10-59 ~ 67. */
  @Transactional(readOnly = true)
  public AnorexiaStreakResponse getAnorexiaStreak(UUID userId, UUID petId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    if (pet.species() != Species.CRESTED_GECKO) {
      throw new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
    }
    OffsetDateTime lastEatenAt =
        feedingLogRepository
            .findFirstByPetIdAndIsRefusedFalseOrderByFedAtDesc(pet.id())
            .map(FeedingLog::getFedAt)
            .orElse(null);
    return AnorexiaStreakCalculator.calculate(lastEatenAt, OffsetDateTime.now(clock));
  }

  private void validateNotFuture(OffsetDateTime fedAt) {
    if (fedAt.isAfter(OffsetDateTime.now(clock))) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private FeedingLog findOwnedLog(UUID petId, UUID logId) {
    return feedingLogRepository
        .findByIdAndPetId(logId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private static FeedingResponse toResponse(FeedingLog entry) {
    return new FeedingResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getFoodType(),
        entry.getFoodSize(),
        entry.getAmount(),
        entry.getAmountUnit(),
        entry.isRefused(),
        entry.getFedAt(),
        entry.getMemo(),
        entry.getCreatedAt());
  }
}
