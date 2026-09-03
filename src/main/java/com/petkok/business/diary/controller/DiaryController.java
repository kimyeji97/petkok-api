package com.petkok.business.diary.controller;

import com.petkok.business.diary.service.DiaryService;
import com.petkok.data.diary.dto.DiaryCreateRequest;
import com.petkok.data.diary.dto.DiaryResponse;
import com.petkok.data.diary.dto.DiaryUpdateRequest;
import com.petkok.data.diary.enums.ConditionTag;
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
 * 다이어리 엔드포인트 (Notion {@code API I/F} Diary 5행). <b>전부 인증이 필요하다.</b> 종 제한 없음. 상태코드 — {@code POST}
 * 201 · {@code GET} 200 · {@code PATCH} 200 · {@code DELETE} 204. 검증 계약 REQ-10-94 · 95 · 99 · 103 ·
 * 104 · 108 ~ 110 · 113 · 114.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/diary")
public class DiaryController {

  private final DiaryService diaryService;

  public DiaryController(DiaryService diaryService) {
    this.diaryService = diaryService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<DiaryResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody DiaryCreateRequest request) {
    return ApiResponse.success(diaryService.create(principal.userId(), petId, request));
  }

  /**
   * 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정하고, 서비스가 최대 50 으로
   * 클램프한다(Phase 5 미결 확정).
   */
  @GetMapping
  public ApiResponse<CursorPage<DiaryResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit,
      @RequestParam(name = "condition_tag", required = false) ConditionTag conditionTag) {
    return ApiResponse.success(
        diaryService.list(
            principal.userId(), petId, new CursorRequest(cursor, limit), conditionTag));
  }

  @PatchMapping("/{entryId}")
  public ApiResponse<DiaryResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID entryId,
      @Valid @RequestBody DiaryUpdateRequest request) {
    return ApiResponse.success(diaryService.update(principal.userId(), petId, entryId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code ShedController.delete} 와 같은 형태). */
  @DeleteMapping("/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID entryId) {
    diaryService.delete(principal.userId(), petId, entryId);
  }
}
