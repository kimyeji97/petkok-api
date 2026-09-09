package com.petkok.data.gallery.dto;

/**
 * {@code POST /photos/presigned-url} 응답. {@code uploadUrl} 로 클라이언트가 R2 에 직접 올리고, 업로드가 끝나면 {@code
 * imageUrl} 을 {@code POST /pets/{pet_id}/photos} 의 {@code image_url} 로 그대로 보낸다(2단계 업로드).
 *
 * <p>⚠️ 필드명은 확정이 아니다 — 계획서·Notion 원본 어디에도 정확한 응답 형태가 없어 미결로 남아 있다(PLAN-REQ-11 § 미결 질문).
 */
public record PhotoPresignedUrlResponse(String uploadUrl, String imageUrl) {}
