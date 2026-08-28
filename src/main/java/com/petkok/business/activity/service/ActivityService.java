package com.petkok.business.activity.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.activity.dto.ActivityCreateRequest;
import com.petkok.data.activity.dto.ActivityResponse;
import com.petkok.data.activity.dto.ActivityUpdateRequest;
import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.activity.repository.ActivityLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활동 기록 CRUD + 종별 검증. 검증 계약 REQ-10-24 ~ 42 (PLAN-REQ-10 § 검증 계약). {@code WeightService} 형태를
 * 복제했다(D9).
 *
 * <p><b>종별 검증은 여기서 한다</b> (REQ-09 D4) — 가드가 실어 준 {@code species} 로 {@link
 * ActivityType#isAllowedFor} 를 진입 시 적용한다. 등록뿐 아니라 <b>PATCH 로 유형이 바뀔 때도</b> 다시 건다(REQ-10-29). {@code
 * distance_km} 는 검증하지 않는다(D13).
 */
@Slf4j
@Service
public class ActivityService {

  private final PetAccessGuard petAccessGuard;
  private final ActivityLogRepository activityLogRepository;
  private final CursorCodec cursorCodec;

  public ActivityService(
      PetAccessGuard petAccessGuard,
      ActivityLogRepository activityLogRepository,
      CursorCodec cursorCodec) {
    this.petAccessGuard = petAccessGuard;
    this.activityLogRepository = activityLogRepository;
    this.cursorCodec = cursorCodec;
  }

  @Transactional
  public ActivityResponse create(UUID userId, UUID petId, ActivityCreateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    validateType(pet.species(), request.activityType());
    ActivityLog saved =
        activityLogRepository.save(
            ActivityLog.of(
                pet.id(),
                request.activityType(),
                request.durationMinutes(),
                request.distanceKm(),
                request.memo(),
                request.loggedAt()));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CursorPage<ActivityResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<ActivityLog> rows;
    if (request.hasCursor()) {
      ActivityCursor cursor = cursorCodec.decode(request.cursor(), ActivityCursor.class);
      rows =
          activityLogRepository.findPageAfter(pet.id(), cursor.loggedAt(), cursor.id(), pageable);
    } else {
      rows = activityLogRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<ActivityLog> page = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = null;
    if (hasNext) {
      ActivityLog last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new ActivityCursor(last.getLoggedAt(), last.getId()));
    }
    return CursorPage.of(
        page.stream().map(ActivityService::toResponse).toList(), nextCursor, hasNext);
  }

  /** 수정 — 보낸 필드만 반영 (D10). {@code activityType} 이 오면 종 검증을 다시 건다. */
  @Transactional
  public ActivityResponse update(
      UUID userId, UUID petId, UUID logId, ActivityUpdateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    ActivityLog entry = findOwnedLog(pet.id(), logId);
    if (request.activityType() != null) {
      validateType(pet.species(), request.activityType());
    }

    entry.update(
        request.activityType() != null ? request.activityType() : entry.getActivityType(),
        request.durationMinutes() != null ? request.durationMinutes() : entry.getDurationMinutes(),
        request.distanceKm() != null ? request.distanceKm() : entry.getDistanceKm(),
        request.memo() != null ? request.memo() : entry.getMemo(),
        request.loggedAt() != null ? request.loggedAt() : entry.getLoggedAt());

    return toResponse(entry);
  }

  @Transactional
  public void delete(UUID userId, UUID petId, UUID logId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    activityLogRepository.delete(findOwnedLog(pet.id(), logId));
    log.info("ActivityLog deleted. petId={}, logId={}", petId, logId);
  }

  private static void validateType(Species species, ActivityType type) {
    if (!type.isAllowedFor(species)) {
      throw new BusinessException(ErrorCode.INVALID_SPECIES_ACTIVITY);
    }
  }

  private ActivityLog findOwnedLog(UUID petId, UUID logId) {
    return activityLogRepository
        .findByIdAndPetId(logId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private static ActivityResponse toResponse(ActivityLog entry) {
    return new ActivityResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getActivityType(),
        entry.getDurationMinutes(),
        entry.getDistanceKm(),
        entry.getMemo(),
        entry.getLoggedAt(),
        entry.getCreatedAt());
  }
}
