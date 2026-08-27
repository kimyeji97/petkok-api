package com.petkok.data.user.dto;

import jakarta.validation.constraints.Size;

/**
 * {@code PATCH /users/me} 요청. <b>보낸 필드만 반영된다.</b>
 *
 * <p><b>누락과 {@code null} 은 둘 다 "변경 없음"이다</b> (PLAN-REQ-08 D3). Notion 원본이 "변경할 필드만 포함"만 규정하고 {@code
 * null} 의 의미를 정의하지 않으므로, 원본에 없는 규약을 만들지 않는다. AGENTS.md §5 가 PATCH 를 고른 이유(누락과 {@code null} 구분)와는
 * 어긋나는 <b>의도적 예외</b>다 — 따라서 <b>지금은 프로필 이미지를 제거할 수단이 없다</b>(계획서 미결).
 *
 * <p>⚠️ <b>{@code @NotBlank} 를 붙이면 안 된다.</b> {@code null} 을 거부하므로 "닉네임을 안 보내는" 정상 경로가 통째로 400 이 된다.
 * {@code users.nickname} 의 {@code NOT NULL} 은 <b>엔티티의 불변식이지 이 요청 DTO 의 불변식이 아니다.</b>
 *
 * <p>{@code @Size} 는 {@code null} 을 통과시킨다 — 그래서 D3 과 충돌하지 않는다. 상한 100 은 {@code varchar(100)} 이
 * 스키마에서 확정된 사실이기 때문이고, 이게 없으면 101자 요청이 {@code DataIntegrityViolationException} 으로 올라와 <b>400 이 아니라
 * 500</b> 이 된다 (D7). 하한 1 은 D9(2026-08-27, Notion {@code API I/F} 에 먼저 명시) — {@code ""} 를 여기서 거른다.
 *
 * <p>⚠️ <b>{@code @Size(min = 1)} 은 공백만인 값({@code " "})을 통과시킨다.</b> 트림은 여기가 아니라 {@code UserService}
 * 가 하며, 트림 후 빈 값은 서비스가 {@code INVALID_INPUT} 으로 거부한다. 두 층이 나뉘어 있는 이유는 D3 — 애노테이션으로 "트림 후 검사"를 표현하려면
 * {@code null} 처리까지 얽혀 규약이 늘어난다.
 *
 * <p>검증 계약 REQ-08-06 · 07 · 26 · 27.
 */
public record UserUpdateRequest(
    @Size(min = 1, max = 100) String nickname, @Size(max = 500) String profileImageUrl) {}
