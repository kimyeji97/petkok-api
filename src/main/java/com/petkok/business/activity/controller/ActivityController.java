package com.petkok.business.activity.controller;

import com.petkok.business.activity.service.ActivityService;
import com.petkok.data.activity.dto.ActivityCreateRequest;
import com.petkok.data.activity.dto.ActivityResponse;
import com.petkok.data.activity.dto.ActivityUpdateRequest;
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

/** 활동 기록 엔드포인트 (Notion {@code API I/F} Activity 4행). 전부 인증 필요. 201 · 200 · 200 · 204. */
@RestController
@RequestMapping("/api/v1/pets/{petId}/activity")
public class ActivityController {

  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ActivityResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody ActivityCreateRequest request) {
    return ApiResponse.success(activityService.create(principal.userId(), petId, request));
  }

  @GetMapping
  public ApiResponse<CursorPage<ActivityResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit) {
    return ApiResponse.success(
        activityService.list(principal.userId(), petId, new CursorRequest(cursor, limit)));
  }

  @PatchMapping("/{logId}")
  public ApiResponse<ActivityResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID logId,
      @Valid @RequestBody ActivityUpdateRequest request) {
    return ApiResponse.success(activityService.update(principal.userId(), petId, logId, request));
  }

  @DeleteMapping("/{logId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID logId) {
    activityService.delete(principal.userId(), petId, logId);
  }
}
