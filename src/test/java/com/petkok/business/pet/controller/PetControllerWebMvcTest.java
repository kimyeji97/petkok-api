package com.petkok.business.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.pet.service.PetService;
import com.petkok.data.pet.dto.PetResponse;
import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * pets 의 <b>HTTP 계약</b>. 검증 계약 REQ-09-03 · 04 · 15 · 16 (PLAN-REQ-09 § 검증 계약).
 *
 * <p>설정은 AGENTS §6 관례를 따른다 — {@code @Import({SecurityConfig, JacksonConfig})} 를 빼면 테스트가 통과하면서 틀린
 * 계약을 고정한다. 이 슬라이스에는 {@code UserService} 가 없으므로 {@link UserStatusChecker} 를 <b>따로 목으로 둔다</b>(필터가 자동
 * 포함되기 때문).
 *
 * <p>⚠️ <b>{@code message} 를 단언하지 않는다</b> — 로케일을 탄다(REQ-15 실측). {@code status} 와 {@code error.code}
 * 만 본다.
 */
@WebMvcTest(PetController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class PetControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private MockMvc mockMvc;
  @MockBean private PetService petService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static String createBody(String species, String gender) {
    return "{\"name\":\"두부\",\"species\":\"" + species + "\",\"gender\":\"" + gender + "\"}";
  }

  @Test
  @DisplayName("[REQ-09-03] POST /pets 는 201 을 반환한다")
  void req_09_03_createReturnsCreated() throws Exception {
    when(petService.create(any(), any()))
        .thenReturn(
            new PetResponse(
                PET_ID,
                "두부",
                Species.CRESTED_GECKO,
                null,
                Gender.MALE,
                LocalDate.of(2023, 3, 15),
                null,
                null,
                LocalDateTime.now()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/pets")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("CRESTED_GECKO", "MALE")))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-09-04] DELETE /pets/{id} 는 204 다")
  void req_09_04_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/pets/" + PET_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-09-04] DELETE /pets/{id} 는 본문이 비어 있다")
  void req_09_04_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/pets/" + PET_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-09-15] 정의되지 않은 species 값은 400 이다")
  void req_09_15_unknownSpeciesIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/pets")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("HAMSTER", "MALE")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-09-16] 정의되지 않은 gender 값은 400 이다")
  void req_09_16_unknownGenderIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/pets")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("DOG", "NEUTER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-09-01] GET /pets/{id} 응답 키가 snake_case 다")
  void req_09_01_responseUsesSnakeCase() throws Exception {
    when(petService.findOne(any(), any()))
        .thenReturn(
            new PetResponse(
                PET_ID,
                "두부",
                Species.CRESTED_GECKO,
                null,
                Gender.MALE,
                LocalDate.of(2023, 3, 15),
                LocalDate.of(2023, 5, 1),
                "https://img.example.com/a.png",
                LocalDateTime.now()));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/pets/" + PET_ID).with(asUser()))
        .andExpect(jsonPath("$.data.adoption_date").exists());
  }

  @Test
  @DisplayName("[REQ-09-02] GET /pets/{id} 응답에 updated_at 이 없다")
  void req_09_02_responseHasNoUpdatedAt() throws Exception {
    when(petService.findOne(any(), any()))
        .thenReturn(
            new PetResponse(
                PET_ID, "두부", Species.DOG, null, null, null, null, null, LocalDateTime.now()));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/pets/" + PET_ID).with(asUser()))
        .andExpect(jsonPath("$.data.updated_at").doesNotExist());
  }
}
