package com.petkok.data.user.dto;

import com.petkok.data.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code GET /users/me} · {@code PATCH /users/me} 응답.
 *
 * <p><b>필드는 정확히 5개다.</b> Notion {@code API I/F} 원본이 {@code id}·{@code nickname}·{@code
 * email}·{@code profile_image_url}·{@code created_at} 만 규정한다 — 엔티티는 {@code updated_at} 을 갖고 있어 무심코
 * 넣기 쉬우나 <b>원본에 없으므로 넣지 않는다</b> (PLAN-REQ-08 § 범위 — 제외). 검증 계약 REQ-08-01.
 *
 * <p>{@code email} 은 비어 있는 것이 정상이다 — Kakao 는 이메일을 내려주지 않는다({@link User} 참고).
 *
 * <p>응답 필드는 전역 snake_case 로 나간다({@code profile_image_url}, {@code created_at}).
 */
public record UserResponse(
    UUID id, String nickname, String email, String profileImageUrl, LocalDateTime createdAt) {}
