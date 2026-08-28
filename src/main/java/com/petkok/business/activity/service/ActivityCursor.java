package com.petkok.business.activity.service;

import java.time.LocalDateTime;
import java.util.UUID;

/** 활동 목록 keyset 커서 페이로드 {@code (logged_at, id)} (D8). 위치 이유는 {@code WeightCursor} 와 같다. */
public record ActivityCursor(LocalDateTime loggedAt, UUID id) {}
