package com.petkok.data.timeline.dto;

import java.util.List;

/** {@code GET /pets/{pet_id}/timeline} 응답 — 월간 집계. */
public record TimelineResponse(List<TimelineDayResponse> days) {}
