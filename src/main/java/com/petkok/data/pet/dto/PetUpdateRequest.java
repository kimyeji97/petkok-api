package com.petkok.data.pet.dto;

import com.petkok.data.pet.enums.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code PATCH /pets/{pet_id}} 요청. <b>보낸 필드만 반영된다.</b>
 *
 * <p>⚠️ <b>{@code species} 필드가 없는 것이 계약이다</b> (PLAN-REQ-09 D5). 원본이 "**species 변경 불가**"만 규정하고 거부
 * 방식을 정하지 않았으므로, 원본에 없는 규약(400)을 만들지 않고 <b>무시</b>한다. 알 수 없는 필드는 Spring Boot 기본값 ({@code
 * FAIL_ON_UNKNOWN_PROPERTIES=false})으로 조용히 버려진다 — 2026-08-10 실측으로 확인한 앱 전체의 동작이다. 클라이언트는 응답의 {@code
 * species} 로 바뀌지 않았음을 확인할 수 있다. 검증 계약 REQ-09-17.
 *
 * <p>⚠️ <b>{@code @NotBlank}·{@code @NotNull} 을 붙이지 않는다</b> (AGENTS §5). {@code null} 은 "변경 없음"이라
 * 필드를 안 보내는 것이 정상 경로다 — 붙이면 부분 수정이 통째로 400 이 된다. 길이 제약은 {@code @Size} 로만 표현한다.
 */
public record PetUpdateRequest(
    @Size(max = 100) String name,
    @Size(max = 100) String breed,
    Gender gender,
    LocalDate birthday,
    LocalDate adoptionDate,
    @Size(max = 500) String profileImageUrl) {}
