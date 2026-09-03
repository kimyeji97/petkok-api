package com.petkok.business.diary.service;

import java.time.LocalDate;
import java.util.UUID;

/** 다이어리 keyset 커서 페이로드 (D8) — {@code entry_date}·{@code id}. */
public record DiaryCursor(LocalDate entryDate, UUID id) {}
