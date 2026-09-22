package com.petkok.business.environment.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 환경 기록 목록 keyset 커서 페이로드 {@code (measured_at, id)} — {@code FeedingCursor} 와 같은 이유로 위치한다. */
public record EnvironmentCursor(OffsetDateTime measuredAt, UUID id) {}
