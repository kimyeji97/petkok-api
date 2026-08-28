package com.petkok.business.weight.controller;

import com.petkok.business.weight.service.WeightService;
import com.petkok.data.weight.dto.WeightCreateRequest;
import com.petkok.data.weight.dto.WeightResponse;
import com.petkok.data.weight.dto.WeightUpdateRequest;
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
 * 체중 기록 엔드포인트 (Notion {@code API I/F} Weight 4행). <b>전부 인증이 필요하다.</b> 상태코드는 원본 그대로 — {@code POST}
 * 201 · {@code GET} 200 · {@code PATCH} 200 · {@code DELETE} 204. 검증 계약 REQ-10-04 · 05 · 11 ~ 14 ·
 * 22.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/weight")
public class WeightController {

  private final WeightService weightService;

  public WeightController(WeightService weightService) {
    this.weightService = weightService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<WeightResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody WeightCreateRequest request) {
    return ApiResponse.success(weightService.create(principal.userId(), petId, request));
  }

  /** 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정한다. */
  @GetMapping
  public ApiResponse<CursorPage<WeightResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit) {
    return ApiResponse.success(
        weightService.list(principal.userId(), petId, new CursorRequest(cursor, limit)));
  }

  @PatchMapping("/{logId}")
  public ApiResponse<WeightResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID logId,
      @Valid @RequestBody WeightUpdateRequest request) {
    return ApiResponse.success(weightService.update(principal.userId(), petId, logId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code PetController.delete} 와 같은 형태). */
  @DeleteMapping("/{logId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID logId) {
    weightService.delete(principal.userId(), petId, logId);
  }
}
