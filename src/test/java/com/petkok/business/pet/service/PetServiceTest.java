package com.petkok.business.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petkok.data.pet.dto.PetUpdateRequest;
import com.petkok.data.pet.entity.Pet;
import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.pet.repository.PetRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 반려동물 CRUD 의 소유권·부분 반영. 검증 계약 REQ-09-05 · 07 · 08 · 17 · 19 (PLAN-REQ-09 § 검증 계약).
 *
 * <p>{@link Pet} 은 <b>실물을 쓴다</b> — 병합 결과가 실제로 엔티티에 반영되는지가 검증 대상이라 목으로 바꾸면 볼 것이 남지 않는다.
 */
class PetServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-20T03:00:00Z"), ZoneId.of("Asia/Seoul"));

  private final PetRepository petRepository = mock(PetRepository.class);
  private final PetService petService = new PetService(petRepository, CLOCK);

  private Pet owned() {
    Pet pet =
        Pet.builder()
            .userId(OWNER)
            .name("두부")
            .species(Species.CRESTED_GECKO)
            .breed("Dalmatian")
            .gender(Gender.MALE)
            .birthday(LocalDate.of(2023, 3, 15))
            .adoptionDate(LocalDate.of(2023, 5, 1))
            .profileImageUrl("https://img.example.com/a.png")
            .build();
    when(petRepository.findByIdAndDeletedAtIsNull(PET_ID)).thenReturn(Optional.of(pet));
    return pet;
  }

  @Test
  @DisplayName("[REQ-09-05] 이름만 보내면 나머지 필드가 유지된다")
  void req_09_05_nameOnlyPatchKeepsOtherFields() {
    Pet pet = owned();

    petService.update(OWNER, PET_ID, new PetUpdateRequest("모찌", null, null, null, null, null));

    assertThat(pet.getBreed()).isEqualTo("Dalmatian");
  }

  @Test
  @DisplayName("[REQ-09-05] 이름만 보내면 성별도 유지된다")
  void req_09_05_nameOnlyPatchKeepsGender() {
    Pet pet = owned();

    petService.update(OWNER, PET_ID, new PetUpdateRequest("모찌", null, null, null, null, null));

    assertThat(pet.getGender()).isEqualTo(Gender.MALE);
  }

  @Test
  @DisplayName("[REQ-09-17] 수정해도 species 는 바뀌지 않는다")
  void req_09_17_speciesNeverChangesOnUpdate() {
    Pet pet = owned();

    petService.update(
        OWNER, PET_ID, new PetUpdateRequest("모찌", "Harlequin", null, null, null, null));

    assertThat(pet.getSpecies()).isEqualTo(Species.CRESTED_GECKO);
  }

  @Test
  @DisplayName("[REQ-09-07] 남의 펫에 접근하면 PET_FORBIDDEN")
  void req_09_07_strangerGetsForbidden() {
    owned();

    assertThatThrownBy(() -> petService.findOne(STRANGER, PET_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-09-08] 없는 펫은 PET_NOT_FOUND")
  void req_09_08_missingPetIsNotFound() {
    when(petRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> petService.findOne(OWNER, PET_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-09-19] 삭제된 펫은 조회되지 않는다 (수동 필터가 실제로 건다)")
  void req_09_19_softDeletedPetIsFilteredOut() {
    // findByIdAndDeletedAtIsNull 이 빈 값을 주는 것이 곧 "필터가 걸렸다"는 뜻이다.
    when(petRepository.findByIdAndDeletedAtIsNull(PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> petService.findOne(OWNER, PET_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-09-19] 삭제는 deleted_at 을 찍는다")
  void req_09_19_deleteSetsDeletedAt() {
    Pet pet = owned();

    petService.delete(OWNER, PET_ID);

    assertThat(pet.isDeleted()).isTrue();
  }
}
