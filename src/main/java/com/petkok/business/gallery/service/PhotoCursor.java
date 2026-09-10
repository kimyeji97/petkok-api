package com.petkok.business.gallery.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 갤러리 목록 keyset 커서 페이로드 — {@code (created_at, id)}. {@code CursorCodec} 이 base64 opaque 문자열로 감싼다.
 *
 * <p>{@code data/gallery/dto} 가 아니라 여기 있는 이유 — DTO 패키지의 네이밍 규칙(ArchUnit {@code DTO_NAMING}: {@code
 * *Request}/{@code *Response})에 맞지 않고, 클라이언트에 노출되는 형태도 아니다(opaque). {@code WeightCursor} 와 같은
 * 이유(AGENTS §3).
 */
public record PhotoCursor(OffsetDateTime createdAt, UUID id) {}
