package com.petkok.business.environment.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.environment.dto.EnvironmentCreateRequest;
import com.petkok.data.environment.dto.EnvironmentDailySummaryResponse;
import com.petkok.data.environment.dto.EnvironmentResponse;
import com.petkok.data.environment.dto.EnvironmentUpdateRequest;
import com.petkok.data.environment.entity.EnvironmentLog;
import com.petkok.data.environment.repository.EnvironmentLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.constant.TimeConstant;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게코 사육 환경(온습도) 기록 CRUD + 일간 평균 조회. 검증 계약 REQ-20-02 ~ 15(PLAN-REQ-20 § 검증 계약). {@code ShedService}
 * 형태를 복제했다 — {@code measured_at} 이 timestamptz 라 keyset 커서는 {@code FeedingCursor} 형태를 쓴다.
 *
 * <p><b>🦎 게코 전용 — 다섯 엔드포인트 전부 진입 시 종을 검증한다.</b> 게코 외 종은 {@code FEATURE_NOT_SUPPORTED_SPECIES}(거식
 * 스트릭·탈피 예측과 공통 코드).
 */
@Slf4j
@Service
public class EnvironmentService {

  private final PetAccessGuard petAccessGuard;
  private final EnvironmentLogRepository environmentLogRepository;
  private final CursorCodec cursorCodec;

  public EnvironmentService(
      PetAccessGuard petAccessGuard,
      EnvironmentLogRepository environmentLogRepository,
      CursorCodec cursorCodec) {
    this.petAccessGuard = petAccessGuard;
    this.environmentLogRepository = environmentLogRepository;
    this.cursorCodec = cursorCodec;
  }

  @Transactional
  public EnvironmentResponse create(UUID userId, UUID petId, EnvironmentCreateRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    EnvironmentLog saved =
        environmentLogRepository.save(
            EnvironmentLog.of(
                pet.id(),
                request.temperature(),
                request.humidity(),
                request.measuredAt(),
                request.memo()));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CursorPage<EnvironmentResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<EnvironmentLog> rows;
    if (request.hasCursor()) {
      EnvironmentCursor cursor = cursorCodec.decode(request.cursor(), EnvironmentCursor.class);
      rows =
          environmentLogRepository.findPageAfter(
              pet.id(), cursor.measuredAt(), cursor.id(), pageable);
    } else {
      rows = environmentLogRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<EnvironmentLog> page = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = null;
    if (hasNext) {
      EnvironmentLog last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new EnvironmentCursor(last.getMeasuredAt(), last.getId()));
    }
    return CursorPage.of(
        page.stream().map(EnvironmentService::toResponse).toList(), nextCursor, hasNext);
  }

  /** 수정 — 보낸 필드만 반영. */
  @Transactional
  public EnvironmentResponse update(
      UUID userId, UUID petId, UUID logId, EnvironmentUpdateRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    EnvironmentLog entry = findOwnedLog(pet.id(), logId);

    entry.update(
        request.temperature() != null ? request.temperature() : entry.getTemperature(),
        request.humidity() != null ? request.humidity() : entry.getHumidity(),
        request.measuredAt() != null ? request.measuredAt() : entry.getMeasuredAt(),
        request.memo() != null ? request.memo() : entry.getMemo());

    return toResponse(entry);
  }

  @Transactional
  public void delete(UUID userId, UUID petId, UUID logId) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    environmentLogRepository.delete(findOwnedLog(pet.id(), logId));
    log.info("EnvironmentLog deleted. petId={}, logId={}", petId, logId);
  }

  /** 🦎 게코 전용 — 지정 날짜(KST 자정 경계)의 온습도 일간 평균. 검증 계약 REQ-20-03·09 ~ 15. */
  @Transactional(readOnly = true)
  public EnvironmentDailySummaryResponse getDailySummary(UUID userId, UUID petId, LocalDate date) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    OffsetDateTime from = date.atStartOfDay(TimeConstant.KST).toOffsetDateTime();
    OffsetDateTime toExclusive = date.plusDays(1).atStartOfDay(TimeConstant.KST).toOffsetDateTime();
    List<EnvironmentLog> logs =
        environmentLogRepository.findByPetIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
            pet.id(), from, toExclusive);
    return EnvironmentSummaryCalculator.calculate(
        logs.stream().map(EnvironmentLog::getTemperature).toList(),
        logs.stream().map(EnvironmentLog::getHumidity).toList());
  }

  private static OwnedPetResponse validateGecko(OwnedPetResponse pet) {
    if (pet.species() != Species.CRESTED_GECKO) {
      throw new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
    }
    return pet;
  }

  private EnvironmentLog findOwnedLog(UUID petId, UUID logId) {
    return environmentLogRepository
        .findByIdAndPetId(logId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private static EnvironmentResponse toResponse(EnvironmentLog entry) {
    return new EnvironmentResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getTemperature(),
        entry.getHumidity(),
        entry.getMeasuredAt(),
        entry.getMemo(),
        entry.getCreatedAt());
  }
}
