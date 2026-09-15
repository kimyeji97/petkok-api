package com.petkok.data.timeline.dto;

import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.diary.enums.ConditionTag;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 타임라인 하루 안의 이벤트 1건. 공통 필드(type·refId·occurredAt·summary) + 도메인별 선택 필드 — 해당 도메인이 아니면 {@code null}
 * (PLAN-REQ-12 §결정 "events[] 타입별 부가 필드 스키마").
 */
public record TimelineEventResponse(
    String type,
    UUID refId,
    OffsetDateTime occurredAt,
    String summary,
    ConditionTag conditionTag,
    Boolean isRefused,
    ActivityType activityType,
    Boolean isComplete,
    Boolean isAssisted) {}
