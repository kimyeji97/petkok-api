package com.petkok.business.shed.controller;

import com.petkok.business.shed.service.ShedService;
import com.petkok.data.shed.dto.ShedCreateRequest;
import com.petkok.data.shed.dto.ShedPredictionResponse;
import com.petkok.data.shed.dto.ShedResponse;
import com.petkok.data.shed.dto.ShedUpdateRequest;
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
 * 탈피 기록 엔드포인트 (Notion {@code API I/F} Shed 5행, 🦎 게코 전용). <b>전부 인증이 필요하다.</b> 상태코드는 원본 그대로 — {@code
 * POST} 201 · {@code GET} 200 · {@code PATCH} 200 · {@code DELETE} 204. 검증 계약 REQ-10-68 · 69 · 73 ·
 * 77 · 79 ~ 82 · 92 · 93.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/shed")
public class ShedController {

  private final ShedService shedService;

  public ShedController(ShedService shedService) {
    this.shedService = shedService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ShedResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody ShedCreateRequest request) {
    return ApiResponse.success(shedService.create(principal.userId(), petId, request));
  }

  /** 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정한다. */
  @GetMapping
  public ApiResponse<CursorPage<ShedResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit) {
    return ApiResponse.success(
        shedService.list(principal.userId(), petId, new CursorRequest(cursor, limit)));
  }

  @PatchMapping("/{recordId}")
  public ApiResponse<ShedResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID recordId,
      @Valid @RequestBody ShedUpdateRequest request) {
    return ApiResponse.success(shedService.update(principal.userId(), petId, recordId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code WeightController.delete} 와 같은 형태). */
  @DeleteMapping("/{recordId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID recordId) {
    shedService.delete(principal.userId(), petId, recordId);
  }

  /** 🦎 게코 전용 — 다음 탈피 예측. 게코 외 종은 {@code FEATURE_NOT_SUPPORTED_SPECIES}(400). */
  @GetMapping("/prediction")
  public ApiResponse<ShedPredictionResponse> getPrediction(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId) {
    return ApiResponse.success(shedService.getPrediction(principal.userId(), petId));
  }
}
