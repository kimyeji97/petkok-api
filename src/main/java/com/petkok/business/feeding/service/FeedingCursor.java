package com.petkok.business.feeding.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 급여 목록 keyset 커서 페이로드 {@code (fed_at, id)} (D8). 위치 이유는 {@code ActivityCursor} 와 같다. */
public record FeedingCursor(OffsetDateTime fedAt, UUID id) {}
