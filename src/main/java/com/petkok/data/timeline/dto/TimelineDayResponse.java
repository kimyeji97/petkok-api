package com.petkok.data.timeline.dto;

import java.time.LocalDate;
import java.util.List;

/** 타임라인 하루치 집계 — {@code markers}(그날 존재하는 기록 유형, 캘린더 도트) + {@code events}(시간순 상세). */
public record TimelineDayResponse(
    LocalDate date, List<String> markers, List<TimelineEventResponse> events) {}
