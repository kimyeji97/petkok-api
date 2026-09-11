package com.petkok.business.gallery.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.gallery.dto.PhotoCreateRequest;
import com.petkok.data.gallery.dto.PhotoPresignedUrlRequest;
import com.petkok.data.gallery.dto.PhotoPresignedUrlResponse;
import com.petkok.data.gallery.dto.PhotoResponse;
import com.petkok.data.gallery.entity.Photo;
import com.petkok.data.gallery.repository.PhotoRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.framework.config.R2Properties;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import com.petkok.framework.port.PhotoLookup;
import com.petkok.framework.port.PhotoSummary;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 갤러리 사진 CRUD (Phase 1 — diary 통합 제외). 검증 계약 REQ-11-01 ~ 26 (PLAN-REQ-11 § 검증 계약).
 *
 * <ul>
 *   <li><b>진입 = {@link PetAccessGuard}</b> (D5, presigned 발급 제외). {@code PetRepository}·{@code Pet}
 *       은 참조하지 않는다
 *   <li><b>사진 ↔ 펫 귀속 = {@code findByIdAndPetId}</b> (REQ-10 D6 관례). 없으면 {@code
 *       RESOURCE_NOT_FOUND}(404)
 *   <li><b>목록 = keyset 커서</b>. {@code limit + 1} 건을 읽어 {@code has_next} 를 판정하고, 커서는 마지막 항목의 {@code
 *       (created_at, id)}
 *   <li><b>삭제 = R2 객체 삭제 성공 후에만 DB 행 삭제</b>(계획서 결정). R2 삭제가 실패하면 DB 는 그대로 두고 예외를 그대로 전파한다(500) — 고아
 *       DB 행(깨진 이미지 URL)이 고아 R2 파일보다 나쁜 실패 모드라는 판단
 * </ul>
 */
@Slf4j
@Service
public class PhotoService implements PhotoLookup {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
  private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

  private final PetAccessGuard petAccessGuard;
  private final PhotoRepository photoRepository;
  private final CursorCodec cursorCodec;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final R2Properties r2Properties;

  public PhotoService(
      PetAccessGuard petAccessGuard,
      PhotoRepository photoRepository,
      CursorCodec cursorCodec,
      S3Client s3Client,
      S3Presigner s3Presigner,
      R2Properties r2Properties) {
    this.petAccessGuard = petAccessGuard;
    this.photoRepository = photoRepository;
    this.cursorCodec = cursorCodec;
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.r2Properties = r2Properties;
  }

  /**
   * presigned 업로드 URL 발급. 펫 경로 밖이라 {@link PetAccessGuard} 를 타지 않는다 — 인증만으로 충분하다(계획서 결정). 검증 계약
   * REQ-11-01 ~ 04.
   */
  public PhotoPresignedUrlResponse createPresignedUrl(PhotoPresignedUrlRequest request) {
    if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }
    if (request.contentLength() > MAX_FILE_BYTES) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }

    String key = "photos/" + UUID.randomUUID();
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(r2Properties.bucket())
            .key(key)
            .contentType(request.contentType())
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGN_DURATION)
            .putObjectRequest(putObjectRequest)
            .build();
    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

    return new PhotoPresignedUrlResponse(
        presigned.url().toString(), r2Properties.publicBaseUrl() + "/" + key);
  }

  @Transactional
  public PhotoResponse create(UUID userId, UUID petId, PhotoCreateRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    Photo saved =
        photoRepository.save(
            Photo.of(
                pet.id(),
                request.diaryEntryId(),
                request.imageUrl(),
                request.caption(),
                request.takenDate()));
    return toResponse(saved);
  }

  /** 목록 (최신순, 커서). 검증 계약 REQ-11-11 · 14 ~ 17. */
  @Transactional(readOnly = true)
  public CursorPage<PhotoResponse> list(UUID userId, UUID petId, CursorRequest request) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    int limit = request.limit();
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<Photo> rows;
    if (request.hasCursor()) {
      PhotoCursor cursor = cursorCodec.decode(request.cursor(), PhotoCursor.class);
      rows = photoRepository.findPageAfter(pet.id(), cursor.createdAt(), cursor.id(), pageable);
    } else {
      rows = photoRepository.findFirstPage(pet.id(), pageable);
    }

    boolean hasNext = rows.size() > limit;
    List<Photo> page = hasNext ? rows.subList(0, limit) : rows;

    List<PhotoResponse> items = new ArrayList<>(page.size());
    for (Photo photo : page) {
      items.add(toResponse(photo));
    }

    String nextCursor = null;
    if (hasNext) {
      Photo last = page.get(page.size() - 1);
      nextCursor = cursorCodec.encode(new PhotoCursor(last.getCreatedAt(), last.getId()));
    }
    return CursorPage.of(items, nextCursor, hasNext);
  }

  /**
   * 삭제 — R2 객체 삭제가 성공해야 DB 행을 지운다(계획서 결정). R2 삭제 실패 시 여기서 예외가 그대로 전파돼 DB 는 손대지 않는다. 검증 계약 REQ-11-19
   * ~ 25.
   */
  @Transactional
  public void delete(UUID userId, UUID petId, UUID photoId) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);
    Photo photo = findOwnedPhoto(pet.id(), photoId);

    s3Client.deleteObject(
        DeleteObjectRequest.builder()
            .bucket(r2Properties.bucket())
            .key(extractKey(photo.getImageUrl()))
            .build());

    photoRepository.delete(photo);
    log.info("Photo deleted. petId={}, photoId={}", petId, photoId);
  }

  /** {@link PhotoLookup} 구현(REQ-11 Phase 2). 검증 계약 REQ-11-27 · 28. */
  @Override
  public int countByDiaryEntryId(UUID diaryEntryId) {
    return photoRepository.countByDiaryEntryId(diaryEntryId);
  }

  /** {@link PhotoLookup} 구현(REQ-11 Phase 2) — {@code Photo} 엔티티를 노출하지 않는다. 검증 계약 REQ-11-29. */
  @Override
  public List<PhotoSummary> findByDiaryEntryId(UUID diaryEntryId) {
    return photoRepository.findByDiaryEntryId(diaryEntryId).stream()
        .map(PhotoService::toSummary)
        .toList();
  }

  private static PhotoSummary toSummary(Photo photo) {
    return new PhotoSummary(
        photo.getId(), photo.getImageUrl(), photo.getCaption(), photo.getTakenDate());
  }

  /** 사진 ↔ 펫 귀속 (REQ-10 D6 관례). 검증 계약 REQ-11-21. */
  private Photo findOwnedPhoto(UUID petId, UUID photoId) {
    return photoRepository
        .findByIdAndPetId(photoId, petId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private String extractKey(String imageUrl) {
    String prefix = r2Properties.publicBaseUrl() + "/";
    return imageUrl.startsWith(prefix) ? imageUrl.substring(prefix.length()) : imageUrl;
  }

  private static PhotoResponse toResponse(Photo photo) {
    return new PhotoResponse(
        photo.getId(),
        photo.getPetId(),
        photo.getDiaryEntryId(),
        photo.getImageUrl(),
        photo.getCaption(),
        photo.getTakenDate(),
        photo.getCreatedAt());
  }
}
