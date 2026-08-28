package com.petkok.business.weight.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 체중 목록 keyset 커서 페이로드 — {@code (measured_at, id)} (D8). {@code CursorCodec} 이 base64 opaque 문자열로
 * 감싼다.
 *
 * <p>{@code data/weight/dto} 가 아니라 여기 있는 이유 — DTO 패키지의 네이밍 규칙(ArchUnit {@code DTO_NAMING}: {@code
 * *Request}/{@code *Response})에 맞지 않고, 클라이언트에 노출되는 형태도 아니다(opaque).
 */
public record WeightCursor(LocalDate measuredAt, UUID id) {}
