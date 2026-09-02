package com.petkok.business.shed.service;

import java.time.LocalDate;
import java.util.UUID;

/** 탈피 목록 keyset 커서 페이로드 {@code (shed_date, id)} (D8). 위치 이유는 {@code WeightCursor} 와 같다. */
public record ShedCursor(LocalDate shedDate, UUID id) {}
