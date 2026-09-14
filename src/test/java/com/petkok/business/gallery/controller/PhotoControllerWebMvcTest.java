package com.petkok.business.gallery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.gallery.service.PhotoService;
import com.petkok.data.gallery.dto.PhotoPresignedUrlResponse;
import com.petkok.data.gallery.dto.PhotoResponse;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
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
 * gallery 도메인의 <b>HTTP 계약</b>. 검증 계약 REQ-11-01 · 05 · 06 · 11 · 18 (PLAN-REQ-11 § 검증 계약).
 *
 * <p>설정은 AGENTS §6 관례 — {@code @Import({SecurityConfig, JacksonConfig})} · {@link
 * UserStatusChecker} 는 이 슬라이스에 {@code UserService} 가 없으므로 따로 목. {@code message} 는 단언하지 않는다.
 *
 * <p>⚠️ presigned 응답의 정확한 필드명(예: {@code upload_url}/{@code image_url})은 계획서·스펙 어디에도 근거가 없어 <b>미결로
 * 남겼다</b> — 여기서는 상태 코드만 확인하고 응답 바디 필드는 단언하지 않는다.
 */
@WebMvcTest(PhotoController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class PhotoControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID PHOTO_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/photos";

  @Autowired private MockMvc mockMvc;
  @MockBean private PhotoService photoService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static PhotoResponse sample() {
    return new PhotoResponse(
        PHOTO_ID,
        PET_ID,
        null,
        "https://img.petkok.com/photos/a.jpg",
        null,
        null,
        OffsetDateTime.now());
  }

  @Test
  @DisplayName("[REQ-11-01] POST /photos/presigned-url 은 허용 타입·크기 이내 요청에 200 을 반환한다")
  void req_11_01_presignedUrlReturnsOk() throws Exception {
    when(photoService.createPresignedUrl(any()))
        .thenReturn(
            new PhotoPresignedUrlResponse(
                "https://r2.example.com/upload", "https://img.petkok.com/a.jpg"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/photos/presigned-url")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content_type\":\"image/jpeg\",\"content_length\":1000000}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("[REQ-11-05] image_url 없이 POST /photos 를 보내면 400 이다")
  void req_11_05_missingImageUrlIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-11-06] caption·taken_date·diary_entry_id 없이도 201 이다")
  void req_11_06_optionalFieldsOmittedStillCreates() throws Exception {
    when(photoService.create(any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"image_url\":\"https://img.petkok.com/photos/a.jpg\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-11-11] GET /photos 응답에 items · next_cursor · has_next 키가 있다")
  void req_11_11_listResponseHasCursorPageKeys() throws Exception {
    when(photoService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.next_cursor").hasJsonPath())
        .andExpect(jsonPath("$.data.has_next").value(false));
  }

  @Test
  @DisplayName("[REQ-11-18] DELETE /photos/{photo_id} 는 204 이고 본문이 비어 있다")
  void req_11_18_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + PHOTO_ID).with(asUser()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
  }
}
