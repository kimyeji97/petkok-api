package com.petkok.business.shed.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.shed.dto.ShedCreateRequest;
import com.petkok.data.shed.dto.ShedPredictionResponse;
import com.petkok.data.shed.dto.ShedResponse;
import com.petkok.data.shed.dto.ShedUpdateRequest;
import com.petkok.data.shed.entity.ShedRecord;
import com.petkok.data.shed.repository.ShedRecordRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈피 기록 CRUD + 탈피 예측 조회. 검증 계약 REQ-10-68 ~ 93 (PLAN-REQ-10 § 검증 계약). {@code WeightService} 형태를
 * 복제했다(D9) — {@code shed_date} 가 {@code LocalDate} 라 {@code Clock} 이 필요 없다.
 *
 * <p><b>🦎 게코 전용 — 다섯 엔드포인트 전부 진입 시 종을 검증한다</b>(api-list.md § 8). 게코 외 종은 {@code
 * FEATURE_NOT_SUPPORTED_SPECIES}(거식 스트릭과 공통 코드).
 */
@Slf4j
@Service
public class ShedService {

  private static final int PREDICTION_RECENT_LIMIT = 3;

  private final PetAccessGuard petAccessGuard;
  private final ShedRecordRepository shedRecordRepository;
  private final CursorCodec cursorCodec;

  public ShedService(
      PetAccessGuard petAccessGuard,
      ShedRecordRepository shedRecordRepository,
      CursorCodec cursorCodec) {
    this.petAccessGuard = petAccessGuard;
    this.shedRecordRepository = shedRecordRepository;
    this.cursorCodec = cursorCodec;
  }

  @Transactional
  public ShedResponse create(UUID userId, UUID petId, ShedCreateRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    ShedRecord saved =
        shedRecordRepository.save(
            ShedRecord.of(
                pet.id(),
                request.shedDate(),
                request.isComplete() == null || request.isComplete(),
                Boolean.TRUE.equals(request.isAssisted()),
                request.memo()));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CursorPage<ShedResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<ShedRecord> rows;
    if (request.hasCursor()) {
      ShedCursor cursor = cursorCodec.decode(request.cursor(), ShedCursor.class);
      rows = shedRecordRepository.findPageAfter(pet.id(), cursor.shedDate(), cursor.id(), pageable);
    } else {
      rows = shedRecordRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<ShedRecord> page = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = null;
    if (hasNext) {
      ShedRecord last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new ShedCursor(last.getShedDate(), last.getId()));
    }
    return CursorPage.of(page.stream().map(ShedService::toResponse).toList(), nextCursor, hasNext);
  }

  /** 수정 — 보낸 필드만 반영 (D10). */
  @Transactional
  public ShedResponse update(UUID userId, UUID petId, UUID recordId, ShedUpdateRequest request) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    ShedRecord entry = findOwnedRecord(pet.id(), recordId);

    entry.update(
        request.shedDate() != null ? request.shedDate() : entry.getShedDate(),
        request.isComplete() != null ? request.isComplete() : entry.isComplete(),
        request.isAssisted() != null ? request.isAssisted() : entry.isAssisted(),
        request.memo() != null ? request.memo() : entry.getMemo());

    return toResponse(entry);
  }

  @Transactional
  public void delete(UUID userId, UUID petId, UUID recordId) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    shedRecordRepository.delete(findOwnedRecord(pet.id(), recordId));
    log.info("ShedRecord deleted. petId={}, recordId={}", petId, recordId);
  }

  /** 🦎 게코 전용 — 다음 탈피 예측. 검증 계약 REQ-10-84 ~ 93. */
  @Transactional(readOnly = true)
  public ShedPredictionResponse getPrediction(UUID userId, UUID petId) {
    OwnedPetResponse pet = validateGecko(petAccessGuard.getOwnedPet(petId, userId));
    List<LocalDate> recentShedDates =
        shedRecordRepository
            .findFirstPage(pet.id(), PageRequest.of(0, PREDICTION_RECENT_LIMIT))
            .stream()
            .map(ShedRecord::getShedDate)
            .toList();
    return ShedPredictionCalculator.calculate(recentShedDates);
  }

  private static OwnedPetResponse validateGecko(OwnedPetResponse pet) {
    if (pet.species() != Species.CRESTED_GECKO) {
      throw new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
    }
    return pet;
  }

  private ShedRecord findOwnedRecord(UUID petId, UUID recordId) {
    return shedRecordRepository
        .findByIdAndPetId(recordId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private static ShedResponse toResponse(ShedRecord entry) {
    return new ShedResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getShedDate(),
        entry.isComplete(),
        entry.isAssisted(),
        entry.getMemo(),
        entry.getCreatedAt());
  }
}
