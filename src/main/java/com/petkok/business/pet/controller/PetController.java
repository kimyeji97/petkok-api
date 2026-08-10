package com.petkok.business.pet.controller;

import com.petkok.business.pet.service.PetService;
import com.petkok.data.pet.dto.PetCreateRequest;
import com.petkok.data.pet.dto.PetListResponse;
import com.petkok.data.pet.dto.PetResponse;
import com.petkok.data.pet.dto.PetUpdateRequest;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반려동물 엔드포인트. <b>전부 인증이 필요하다</b> — {@code /pets/**} 는 {@code SecurityConfig.PUBLIC_PATHS} 에 없다.
 *
 * <p>상태코드는 Notion {@code API I/F} 5행 그대로다 (PLAN-REQ-09 D2) — {@code POST} 201 · {@code GET} 200 ·
 * {@code PATCH} 200 · {@code DELETE} 204.
 */
@RestController
@RequestMapping("/api/v1/pets")
public class PetController {

  private final PetService petService;

  public PetController(PetService petService) {
    this.petService = petService;
  }

  /** 등록. 검증 계약 REQ-09-03. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PetResponse> create(
      @CurrentUser AuthPrincipal principal, @Valid @RequestBody PetCreateRequest request) {
    return ApiResponse.success(petService.create(principal.userId(), request));
  }

  /** 내 반려동물 목록. 원본 응답이 {@code {"data":{"items":[...]}}} 라 {@code items} 로 감싼다. */
  @GetMapping
  public ApiResponse<PetListResponse> findMyPets(@CurrentUser AuthPrincipal principal) {
    return ApiResponse.success(new PetListResponse(petService.findMyPets(principal.userId())));
  }

  /** 상세. 남의 펫이면 {@code PET_FORBIDDEN}, 없거나 삭제됐으면 {@code PET_NOT_FOUND}. */
  @GetMapping("/{petId}")
  public ApiResponse<PetResponse> findOne(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId) {
    return ApiResponse.success(petService.findOne(principal.userId(), petId));
  }

  /** 수정. <b>{@code species} 는 보내도 무시된다</b> (D5). */
  @PatchMapping("/{petId}")
  public ApiResponse<PetResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody PetUpdateRequest request) {
    return ApiResponse.success(petService.update(principal.userId(), petId, request));
  }

  /**
   * 삭제 (소프트 딜리트). 검증 계약 REQ-09-04.
   *
   * <p><b>응답 본문이 없다.</b> 204 는 정의상 본문을 갖지 않으므로 공통 래퍼를 쓰지 않는다 — {@code AuthController.logout}·{@code
   * UserController.withdraw} 와 같은 형태다.
   */
  @DeleteMapping("/{petId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@CurrentUser AuthPrincipal principal, @PathVariable UUID petId) {
    petService.delete(principal.userId(), petId);
  }
}
