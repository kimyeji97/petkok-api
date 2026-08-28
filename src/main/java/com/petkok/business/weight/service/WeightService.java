package com.petkok.business.weight.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.weight.dto.WeightCreateRequest;
import com.petkok.data.weight.dto.WeightResponse;
import com.petkok.data.weight.dto.WeightUpdateRequest;
import com.petkok.data.weight.entity.WeightLog;
import com.petkok.data.weight.repository.WeightLogRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 체중 기록 CRUD + 파생 필드. 검증 계약 REQ-10-04 ~ 23 (PLAN-REQ-10 § 검증 계약).
 *
 * <p><b>REQ-10 하위 도메인의 기준 형태다</b> (D9) — activity · feeding · shed · diary 가 이 구조를 복제한다.
 *
 * <ul>
 *   <li><b>진입 = {@link PetAccessGuard}</b> (D5). {@code PetRepository}·{@code Pet} 은 참조하지 않는다 —
 *       ArchUnit 이 {@code business.pet.service}·{@code data.pet.dto}·{@code data.pet.enums} 만 열어
 *       두었다
 *   <li><b>기록 ↔ 펫 귀속 = {@code findByIdAndPetId}</b> (D6). 없으면 {@code RESOURCE_NOT_FOUND}(404) — 남의
 *       기록이 존재한다는 정보를 흘리지 않는다
 *   <li><b>목록 = keyset 커서</b> (D8). {@code limit + 1} 건을 읽어 {@code has_next} 를 판정하고, 커서는 마지막 항목의
 *       {@code (measured_at, id)}
 *   <li><b>삭제 = 행 삭제</b> (D7)
 * </ul>
 */
@Slf4j
@Service
public class WeightService {

  private static final double WARNING_THRESHOLD_PERCENT = 20.0;
  private static final Pageable ONE = PageRequest.of(0, 1);

  private final PetAccessGuard petAccessGuard;
  private final WeightLogRepository weightLogRepository;
  private final CursorCodec cursorCodec;

  public WeightService(
      PetAccessGuard petAccessGuard,
      WeightLogRepository weightLogRepository,
      CursorCodec cursorCodec) {
    this.petAccessGuard = petAccessGuard;
    this.weightLogRepository = weightLogRepository;
    this.cursorCodec = cursorCodec;
  }

  @Transactional
  public WeightResponse create(UUID userId, UUID petId, WeightCreateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    WeightLog saved =
        weightLogRepository.save(
            WeightLog.of(pet.id(), request.weightG(), request.measuredAt(), request.memo()));
    return toResponse(saved, findPrevious(saved));
  }

  /**
   * 목록 (최신순). 검증 계약 REQ-10-10 · 11 · 18 ~ 23.
   *
   * <p>파생 필드의 "직전"은 정렬상 바로 다음 항목이다 (D3). 페이지 마지막 항목의 직전은 페이지 밖에 있으므로 — {@code has_next} 면 이미 읽어 둔
   * {@code limit + 1} 번째 행, 아니면 1건을 더 조회한다.
   */
  @Transactional(readOnly = true)
  public CursorPage<WeightResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<WeightLog> rows;
    if (request.hasCursor()) {
      WeightCursor cursor = cursorCodec.decode(request.cursor(), WeightCursor.class);
      rows =
          weightLogRepository.findPageAfter(pet.id(), cursor.measuredAt(), cursor.id(), pageable);
    } else {
      rows = weightLogRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<WeightLog> page = hasNext ? rows.subList(0, limit) : rows;
    WeightLog beyond = null;
    if (hasNext) {
      beyond = rows.get(limit);
    } else if (!page.isEmpty()) {
      beyond = findPrevious(page.get(page.size() - 1));
    }

    List<WeightResponse> items = new ArrayList<>(page.size());
    for (int i = 0; i < page.size(); i++) {
      WeightLog previous = i + 1 < page.size() ? page.get(i + 1) : beyond;
      items.add(toResponse(page.get(i), previous));
    }

    String nextCursor = null;
    if (hasNext) {
      WeightLog last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new WeightCursor(last.getMeasuredAt(), last.getId()));
    }
    return CursorPage.of(items, nextCursor, hasNext);
  }

  /**
   * 수정. <b>보낸 필드만 반영한다</b> (D10) — 병합은 여기서 한다. {@link WeightLog#update} 는 받은 값을 그대로 쓴다. 검증 계약
   * REQ-10-16.
   */
  @Transactional
  public WeightResponse update(UUID userId, UUID petId, UUID logId, WeightUpdateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    WeightLog entry = findOwnedLog(pet.id(), logId);

    entry.update(
        request.weightG() != null ? request.weightG() : entry.getWeightG(),
        request.measuredAt() != null ? request.measuredAt() : entry.getMeasuredAt(),
        request.memo() != null ? request.memo() : entry.getMemo());

    return toResponse(entry, findPrevious(entry));
  }

  /** 삭제 (행 삭제, D7). 검증 계약 REQ-10-17. */
  @Transactional
  public void delete(UUID userId, UUID petId, UUID logId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    WeightLog entry = findOwnedLog(pet.id(), logId);
    weightLogRepository.delete(entry);
    log.info("WeightLog deleted. petId={}, logId={}", petId, logId);
  }

  /** 기록 ↔ 펫 귀속 (D6). 검증 계약 REQ-10-08. */
  private WeightLog findOwnedLog(UUID petId, UUID logId) {
    return weightLogRepository
        .findByIdAndPetId(logId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /** 직전 기록 = {@code (measured_at, id)} 정렬에서 바로 다음 1건 (D3). 없으면 {@code null}. */
  private WeightLog findPrevious(WeightLog entry) {
    List<WeightLog> previous =
        weightLogRepository.findPageAfter(
            entry.getPetId(), entry.getMeasuredAt(), entry.getId(), ONE);
    return previous.isEmpty() ? null : previous.get(0);
  }

  /** 파생 필드 계산 (D3). 소수 1자리 반올림 · {@code |변화율| >= 20} 경고(20 정확히 포함). */
  private static WeightResponse toResponse(WeightLog entry, WeightLog previous) {
    Double changeRate = null;
    boolean warning = false;
    if (previous != null) {
      int prev = previous.getWeightG();
      changeRate = Math.round((entry.getWeightG() - prev) * 1000.0 / prev) / 10.0;
      warning = Math.abs(changeRate) >= WARNING_THRESHOLD_PERCENT;
    }
    return new WeightResponse(
        entry.getId(),
        entry.getPetId(),
        entry.getWeightG(),
        entry.getMeasuredAt(),
        entry.getMemo(),
        changeRate,
        warning,
        entry.getCreatedAt());
  }
}
