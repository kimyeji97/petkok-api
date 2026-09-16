package com.petkok.business.timeline.controller;

import com.petkok.business.timeline.service.TimelineService;
import com.petkok.data.timeline.dto.TimelineResponse;
import com.petkok.data.timeline.enums.TimelineType;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * timeline 엔드포인트 (Notion {@code API I/F} 「통합 타임라인」행) — 월 단위 캘린더 집계, 커서 없음(PLAN-REQ-12 §제약). 검증 계약
 * REQ-12-27 ~ 32.
 *
 * <p>{@code year_month}·{@code type} 쿼리 문자열은 여기서 직접 {@link YearMonth}·{@link TimelineType}으로 변환해
 * 서비스에 넘긴다({@code WeightController}가 {@code cursor}·{@code limit}을 감싸는 것과 같은 위치의 책임) — 변환 자체는 이
 * 클래스의 테스트가, 필터링 로직은 {@code TimelineMergerTest}가 검증한다.
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/timeline")
public class TimelineController {

  private final TimelineService timelineService;

  public TimelineController(TimelineService timelineService) {
    this.timelineService = timelineService;
  }

  @GetMapping
  public ApiResponse<TimelineResponse> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam("year_month") String yearMonth,
      @RequestParam(value = "type", required = false) String type) {
    return ApiResponse.success(
        timelineService.list(
            principal.userId(), petId, YearMonth.parse(yearMonth), TimelineType.fromParam(type)));
  }
}
