package com.petkok.business.feeding.controller;

import com.petkok.business.feeding.service.FeedingService;
import com.petkok.data.feeding.dto.AnorexiaStreakResponse;
import com.petkok.data.feeding.dto.FeedingCreateRequest;
import com.petkok.data.feeding.dto.FeedingResponse;
import com.petkok.data.feeding.dto.FeedingUpdateRequest;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 급여 기록 엔드포인트 (Notion {@code API I/F} Feeding 5행). <b>전부 인증이 필요하다.</b> 상태코드는 원본 그대로 — {@code POST}
 * 201 · {@code GET} 200 · {@code PATCH} 200 · {@code DELETE} 204. 검증 계약 REQ-10-43 · 44 · 48 · 52 ·
 * 53 · 58 · 66 · 67.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/feeding")
public class FeedingController {

  private final FeedingService feedingService;

  public FeedingController(FeedingService feedingService) {
    this.feedingService = feedingService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<FeedingResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody FeedingCreateRequest request) {
    return ApiResponse.success(feedingService.create(principal.userId(), petId, request));
  }

  /** 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정한다. */
  @GetMapping
  public ApiResponse<CursorPage<FeedingResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit) {
    return ApiResponse.success(
        feedingService.list(principal.userId(), petId, new CursorRequest(cursor, limit)));
  }

  @PatchMapping("/{logId}")
  public ApiResponse<FeedingResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID logId,
      @Valid @RequestBody FeedingUpdateRequest request) {
    return ApiResponse.success(feedingService.update(principal.userId(), petId, logId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code WeightController.delete} 와 같은 형태). */
  @DeleteMapping("/{logId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID logId) {
    feedingService.delete(principal.userId(), petId, logId);
  }

  /** 🦎 게코 전용 — 거식 스트릭 현황. 게코 외 종은 {@code FEATURE_NOT_SUPPORTED_SPECIES}(400). */
  @GetMapping("/anorexia-streak")
  public ApiResponse<AnorexiaStreakResponse> getAnorexiaStreak(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId) {
    return ApiResponse.success(feedingService.getAnorexiaStreak(principal.userId(), petId));
  }
}
