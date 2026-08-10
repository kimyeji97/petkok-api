package com.petkok.data.pet.dto;

import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code POST /pets} 요청.
 *
 * <p>원본 Validation — <b>{@code name} 필수</b> / {@code species}: {@code CRESTED_GECKO | DOG | CAT} /
 * {@code gender}: {@code MALE | FEMALE | UNKNOWN}. 검증 계약 REQ-09-14 ~ 16.
 *
 * <p>⚠️ <b>여기는 PATCH 가 아니므로 {@code @NotBlank}·{@code @NotNull} 을 쓴다.</b> AGENTS §5 가 금지하는 것은
 * <b>PATCH 요청 DTO</b> 다 — 생성 요청에서는 {@code NOT NULL} 컬럼이 곧 요청의 불변식이다. 두 층의 제약이 여기서는 일치한다.
 *
 * <p>enum 값이 정의되지 않은 문자열이면 Jackson 역직렬화 단계에서 실패한다 — {@code GlobalExceptionHandler} 가 400 으로 변환한다.
 */
public record PetCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull Species species,
    @Size(max = 100) String breed,
    Gender gender,
    LocalDate birthday,
    LocalDate adoptionDate,
    @Size(max = 500) String profileImageUrl) {}
