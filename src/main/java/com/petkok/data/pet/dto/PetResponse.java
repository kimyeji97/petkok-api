package com.petkok.data.pet.dto;

import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * pets 응답. {@code POST} · {@code GET}(목록·상세) · {@code PATCH} 가 모두 이 형태를 쓴다.
 *
 * <p><b>필드는 정확히 9개다.</b> Notion {@code API I/F} → 반려동물 목록 응답이 이 9개만 규정한다 — 엔티티는 {@code updated_at}
 * 을 갖고 있어 무심코 넣기 쉬우나 <b>원본에 없으므로 넣지 않는다</b>. 검증 계약 REQ-09-01 · 02.
 *
 * <p>응답 필드는 전역 snake_case 로 나간다({@code adoption_date}, {@code profile_image_url}, {@code
 * created_at}).
 */
public record PetResponse(
    UUID id,
    String name,
    Species species,
    String breed,
    Gender gender,
    LocalDate birthday,
    LocalDate adoptionDate,
    String profileImageUrl,
    LocalDateTime createdAt) {}
