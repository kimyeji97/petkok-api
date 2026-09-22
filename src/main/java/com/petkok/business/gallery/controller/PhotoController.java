package com.petkok.business.gallery.controller;

import com.petkok.business.gallery.service.PhotoService;
import com.petkok.data.gallery.dto.GrowthAlbumResponse;
import com.petkok.data.gallery.dto.PhotoCreateRequest;
import com.petkok.data.gallery.dto.PhotoPresignedUrlRequest;
import com.petkok.data.gallery.dto.PhotoPresignedUrlResponse;
import com.petkok.data.gallery.dto.PhotoResponse;
import com.petkok.data.gallery.dto.PhotoUpdateRequest;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 갤러리 엔드포인트 (Notion {@code API I/F} Photos 행). {@code presigned-url} 은 펫 경로 밖이라 클래스 레벨
 * {@code @RequestMapping} 을 두지 않고 메서드마다 전체 경로를 명시한다. 검증 계약 REQ-11-01 · 05 · 06 · 11 · 18, REQ-22-22
 * ~ 25.
 */
@RestController
public class PhotoController {

  private final PhotoService photoService;

  public PhotoController(PhotoService photoService) {
    this.photoService = photoService;
  }

  /** presigned 업로드 URL 발급. 인증만 필요하다 — pet 소유권은 검증하지 않는다(계획서 결정). */
  @PostMapping("/api/v1/photos/presigned-url")
  public ApiResponse<PhotoPresignedUrlResponse> presignedUrl(
      @Valid @RequestBody PhotoPresignedUrlRequest request) {
    return ApiResponse.success(photoService.createPresignedUrl(request));
  }

  @PostMapping("/api/v1/pets/{petId}/photos")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PhotoResponse> create(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @Valid @RequestBody PhotoCreateRequest request) {
    return ApiResponse.success(photoService.create(principal.userId(), petId, request));
  }

  /**
   * 목록 (최신순, 커서). {@code limit} 미지정·0 이하는 {@link CursorRequest} 가 기본 20 으로 보정한다. {@code tag}(선택,
   * REQ-22) — 특정 태그가 달린 사진만 필터.
   */
  @GetMapping("/api/v1/pets/{petId}/photos")
  public ApiResponse<CursorPage<PhotoResponse>> list(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "0") int limit,
      @RequestParam(required = false) String tag) {
    CursorRequest request = new CursorRequest(cursor, limit);
    CursorPage<PhotoResponse> page =
        tag == null
            ? photoService.list(principal.userId(), petId, request)
            : photoService.list(principal.userId(), petId, request, tag);
    return ApiResponse.success(page);
  }

  /**
   * 수정(REQ-22, 이전엔 이 경로 자체가 없었다). {@code caption}·{@code taken_date}·{@code tags}·{@code
   * is_representative} 부분 수정.
   */
  @PatchMapping("/api/v1/pets/{petId}/photos/{photoId}")
  public ApiResponse<PhotoResponse> update(
      @CurrentUser AuthPrincipal principal,
      @PathVariable UUID petId,
      @PathVariable UUID photoId,
      @Valid @RequestBody PhotoUpdateRequest request) {
    return ApiResponse.success(photoService.update(principal.userId(), petId, photoId, request));
  }

  /** 삭제. 204 는 본문이 없으므로 공통 래퍼를 쓰지 않는다 ({@code WeightController.delete} 와 같은 형태). */
  @DeleteMapping("/api/v1/pets/{petId}/photos/{photoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId, @PathVariable UUID photoId) {
    photoService.delete(principal.userId(), petId, photoId);
  }

  /** 🦎 성장 앨범(REQ-22) — 월별 대표 사진, 시간순. */
  @GetMapping("/api/v1/pets/{petId}/photos/growth-album")
  public ApiResponse<GrowthAlbumResponse> growthAlbum(
      @CurrentUser AuthPrincipal principal, @PathVariable UUID petId) {
    return ApiResponse.success(photoService.getGrowthAlbum(principal.userId(), petId));
  }
}
