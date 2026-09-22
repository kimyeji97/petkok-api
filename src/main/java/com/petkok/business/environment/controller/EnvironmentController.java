package com.petkok.business.environment.controller;

import com.petkok.business.environment.service.EnvironmentService;
import com.petkok.data.environment.dto.EnvironmentCreateRequest;
import com.petkok.data.environment.dto.EnvironmentDailySummaryResponse;
import com.petkok.data.environment.dto.EnvironmentResponse;
import com.petkok.data.environment.dto.EnvironmentUpdateRequest;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
 * 게코 사육 환경(온습도) 기록 엔드포인트 (Notion {@code API I/F} Environment 5행, 🦎 게코 전용). <b>전부 인증이 필요하다.</b>
 * 상태코드는 원본 그대로 — {@code POST} 201 · {@code GET} 200 · {@code PATCH} 200 · {@code DELETE} 204. 검증 계약
 * REQ-20-04 ~ 08 · 14 · 15.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/environment")
public class EnvironmentController {

  private final EnvironmentService environmentService;

  public EnvironmentController(EnvironmentService environmentService) {
    this.environmentService = environmentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<EnvironmentResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody EnvironmentCreateRequest request) {
    return ApiResponse.success(environmentService.create(principal.userId(), petId, request));
  }

  /** 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정한다. */
  @GetMapping
  public ApiResponse<CursorPage<EnvironmentResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit) {
    return ApiResponse.success(
        environmentService.list(principal.userId(), petId, new CursorRequest(cursor, limit)));
  }

  @PatchMapping("/{logId}")
  public ApiResponse<EnvironmentResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID logId,
      @Valid @RequestBody EnvironmentUpdateRequest request) {
    return ApiResponse.success(
        environmentService.update(principal.userId(), petId, logId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code ShedController.delete} 와 같은 형태). */
  @DeleteMapping("/{logId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID logId) {
    environmentService.delete(principal.userId(), petId, logId);
  }

  /** 🦎 게코 전용 — 지정 날짜의 온습도 일간 평균. 게코 외 종은 {@code FEATURE_NOT_SUPPORTED_SPECIES}(400). */
  @GetMapping("/daily-summary")
  public ApiResponse<EnvironmentDailySummaryResponse> getDailySummary(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam LocalDate date) {
    return ApiResponse.success(environmentService.getDailySummary(principal.userId(), petId, date));
  }
}
