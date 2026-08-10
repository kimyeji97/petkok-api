package com.petkok.data.pet.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** pets DTO 의 형태 계약. 검증 계약 REQ-09-01 · 02 · 06 · 14 (PLAN-REQ-09 § 검증 계약). */
class PetDtoContractTest {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private final Validator validator = FACTORY.getValidator();

  @Test
  @DisplayName("[REQ-09-01] 응답 DTO 필드는 정확히 9개다")
  void req_09_01_responseHasExactlyNineComponents() {
    String[] names =
        Arrays.stream(PetResponse.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);

    assertThat(names)
        .containsExactly(
            "id",
            "name",
            "species",
            "breed",
            "gender",
            "birthday",
            "adoptionDate",
            "profileImageUrl",
            "createdAt");
  }

  @Test
  @DisplayName("[REQ-09-02] 응답 DTO 에 updatedAt 이 없다")
  void req_09_02_responseHasNoUpdatedAt() {
    boolean hasUpdatedAt =
        Arrays.stream(PetResponse.class.getRecordComponents())
            .anyMatch(c -> c.getName().equals("updatedAt"));

    assertThat(hasUpdatedAt).isFalse();
  }

  @Test
  @DisplayName("[REQ-09-06] 수정 요청 DTO 에 @NotBlank·@NotNull 이 없다")
  void req_09_06_updateRequestHasNoNotBlankOrNotNull() {
    boolean hasRejectingNull =
        Arrays.stream(PetUpdateRequest.class.getRecordComponents())
            .anyMatch(
                c -> c.isAnnotationPresent(NotBlank.class) || c.isAnnotationPresent(NotNull.class));

    assertThat(hasRejectingNull).isFalse();
  }

  @Test
  @DisplayName("[REQ-09-17] 수정 요청 DTO 에 species 필드가 없다")
  void req_09_17_updateRequestHasNoSpecies() {
    boolean hasSpecies =
        Arrays.stream(PetUpdateRequest.class.getRecordComponents())
            .anyMatch(c -> c.getName().equals("species"));

    assertThat(hasSpecies).isFalse();
  }

  @Test
  @DisplayName("[REQ-09-14] 등록 요청에 name 이 없으면 검증에서 걸린다")
  void req_09_14_createRequestRequiresName() {
    var violations =
        validator.validate(
            new PetCreateRequest(
                null, com.petkok.data.pet.enums.Species.DOG, null, null, null, null, null));

    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("[REQ-09-14] 등록 요청에 species 가 없으면 검증에서 걸린다")
  void req_09_14_createRequestRequiresSpecies() {
    var violations =
        validator.validate(new PetCreateRequest("두부", null, null, null, null, null, null));

    assertThat(violations).isNotEmpty();
  }
}
