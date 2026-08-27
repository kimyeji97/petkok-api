package com.petkok.business.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.entity.Pet;
import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.pet.repository.PetRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 소유권 앵커의 <b>반환 형태</b>. 검증 계약 REQ-09-09 · 10 (PLAN-REQ-09 § 검증 계약, D3 · D4).
 *
 * <p>403 · 404 는 여기서 보지 않는다 — Phase 3 완료 기준이 "<b>HTTP 왕복으로</b> 검증됨"이라 {@code
 * PetControllerWebMvcTest} 의 REQ-09-12 · 13 이 맡는다.
 */
class PetAccessGuardTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private final PetRepository petRepository = mock(PetRepository.class);
  private final PetAccessGuard guard = new PetAccessGuard(petRepository);

  private void owned(Species species) {
    Pet pet = Pet.builder().userId(OWNER).name("두부").species(species).gender(Gender.MALE).build();
    when(petRepository.findByIdAndDeletedAtIsNull(PET_ID)).thenReturn(Optional.of(pet));
  }

  @Test
  @DisplayName("[REQ-09-09] getOwnedPet 의 선언 반환 타입이 Pet 엔티티가 아니다")
  void req_09_09_declaredReturnTypeIsNotEntity() throws NoSuchMethodException {
    Method method = PetAccessGuard.class.getMethod("getOwnedPet", UUID.class, UUID.class);

    assertThat(method.getReturnType()).isNotEqualTo(Pet.class);
  }

  @Test
  @DisplayName("[REQ-09-09] getOwnedPet 이 실제로 돌려주는 객체도 Pet 엔티티가 아니다")
  void req_09_09_returnedObjectIsNotEntity() {
    owned(Species.DOG);

    Object result = guard.getOwnedPet(PET_ID, OWNER);

    assertThat(result).isNotInstanceOf(Pet.class);
  }

  @Test
  @DisplayName("[REQ-09-10] 가드 반환 DTO 에 펫의 species 가 실린다")
  void req_09_10_returnedDtoCarriesSpecies() {
    owned(Species.CRESTED_GECKO);

    OwnedPetResponse result = guard.getOwnedPet(PET_ID, OWNER);

    assertThat(result.species()).isEqualTo(Species.CRESTED_GECKO);
  }
}
