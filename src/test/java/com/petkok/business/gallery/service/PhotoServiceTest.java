package com.petkok.business.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.common.entity.BaseSoftDeleteEntity;
import com.petkok.data.gallery.dto.PhotoCreateRequest;
import com.petkok.data.gallery.dto.PhotoPresignedUrlRequest;
import com.petkok.data.gallery.dto.PhotoResponse;
import com.petkok.data.gallery.entity.Photo;
import com.petkok.data.gallery.repository.PhotoRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.config.R2Properties;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import com.petkok.framework.port.PhotoSummary;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * gallery 도메인 CRUD — presigned 발급 검증 · 가드 소비(D5) · 자원 귀속(D6) · keyset 커서 · R2 삭제 순서. 검증 계약
 * REQ-11-01 ~ 26 중 서비스 레이어 담당분 (PLAN-REQ-11 § 검증 계약).
 *
 * <p>{@link PetAccessGuard}는 목이다 — 403·404를 가드가 던지고 서비스는 그대로 흘리는 것(D5)이 검증 대상이라, 가드 안쪽(펫 저장소)은 이
 * 테스트의 관심이 아니다. {@link S3Client}도 목이다 — 실제 R2 왕복이 아니라 <b>삭제 성공/실패에 따른 순서(D 결정)</b>만 검증한다.
 *
 * <p>⚠️ REQ-11-14·15는 DB 없이 필요조건만 고정한다 — 실제 페이지 경계의 누락·중복은 REQ-10과 같은 관례로 Phase 1 완료 시 로컬 DB로 수동
 * 확인한다(계획서 검증 계약 절 참고).
 */
class PhotoServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID DIARY_ENTRY_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID PHOTO_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
  private static final UUID PHOTO_B = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
  private static final UUID PHOTO_C = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  // ⚠️ 리터럴은 Z(UTC) 오프셋으로 쓴다 — 무설정 CursorCodec 이 인코드 시 오프셋을 Z 로 정규화한다(AGENTS.md 로컬 검증).
  private static final OffsetDateTime JUN_30 = OffsetDateTime.parse("2026-06-30T10:00:00Z");
  private static final String IMAGE_URL = "https://img.petkok.com/photos/a.jpg";
  private static final long ONE_MB = 1024L * 1024L;

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final PhotoRepository repository = mock(PhotoRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final S3Client s3Client = mock(S3Client.class);
  private final S3Presigner s3Presigner = mock(S3Presigner.class);
  private final R2Properties r2Properties =
      new R2Properties(
          "account", "key", "secret", "bucket", "https://r2.example.com", "https://img.petkok.com");
  private final PhotoService service =
      new PhotoService(guard, repository, codec, s3Client, s3Presigner, r2Properties);

  private static Photo photo(UUID id, UUID diaryEntryId, OffsetDateTime createdAt) {
    Photo photo = Photo.of(PET_ID, diaryEntryId, IMAGE_URL, null, null);
    ReflectionTestUtils.setField(photo, "id", id);
    ReflectionTestUtils.setField(photo, "createdAt", createdAt);
    return photo;
  }

  private void ownedByMe() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenReturn(new OwnedPetResponse(PET_ID, Species.CRESTED_GECKO));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── presigned 발급 — 업로드 제한 ────────────────────────────────

  @Test
  @DisplayName("[REQ-11-01] 허용 타입(jpeg)·크기 이내 요청은 예외 없이 발급된다")
  void req_11_01_allowedTypeWithinSizeSucceeds() {
    PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
    when(presigned.url()).thenReturn(mock(URL.class));
    when(s3Presigner.presignPutObject((PutObjectPresignRequest) any())).thenReturn(presigned);

    assertThatCode(
            () ->
                service.createPresignedUrl(new PhotoPresignedUrlRequest("image/jpeg", 5 * ONE_MB)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("[REQ-11-02] content_type 이 HEIC 면 UNSUPPORTED_IMAGE_TYPE 이다")
  void req_11_02_heicIsRejected() {
    assertThatThrownBy(
            () -> service.createPresignedUrl(new PhotoPresignedUrlRequest("image/heic", ONE_MB)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
  }

  @Test
  @DisplayName("[REQ-11-03] 화이트리스트 밖 타입(pdf)도 UNSUPPORTED_IMAGE_TYPE 이다")
  void req_11_03_nonImageTypeIsRejected() {
    assertThatThrownBy(
            () ->
                service.createPresignedUrl(new PhotoPresignedUrlRequest("application/pdf", ONE_MB)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
  }

  @Test
  @DisplayName("[REQ-11-04] content_length 가 10MB 를 넘으면 FILE_TOO_LARGE 다")
  void req_11_04_overSizeIsRejected() {
    assertThatThrownBy(
            () ->
                service.createPresignedUrl(new PhotoPresignedUrlRequest("image/jpeg", 11 * ONE_MB)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FILE_TOO_LARGE);
  }

  // ── 가드 위임 (D5) — 생성 ───────────────────────────────────────

  @Test
  @DisplayName("[REQ-11-07] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다 (생성)")
  void req_11_07_createStrangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(
            () ->
                service.create(
                    STRANGER, PET_ID, new PhotoCreateRequest(IMAGE_URL, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-11-08] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다 (생성)")
  void req_11_08_createDeletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(
            () ->
                service.create(OWNER, PET_ID, new PhotoCreateRequest(IMAGE_URL, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 다이어리 연결 시점 (결정) ────────────────────────────────────

  @Test
  @DisplayName("[REQ-11-09] diary_entry_id 를 보내면 그대로 저장된다")
  void req_11_09_diaryEntryIdIsPersistedWhenSent() {
    ownedByMe();

    PhotoResponse response =
        service.create(
            OWNER, PET_ID, new PhotoCreateRequest(IMAGE_URL, null, null, DIARY_ENTRY_ID));

    assertThat(response.diaryEntryId()).isEqualTo(DIARY_ENTRY_ID);
  }

  @Test
  @DisplayName("[REQ-11-10] diary_entry_id 없이 보내면 null 로 저장된다 (단독 갤러리)")
  void req_11_10_diaryEntryIdIsNullWhenOmitted() {
    ownedByMe();

    PhotoResponse response =
        service.create(OWNER, PET_ID, new PhotoCreateRequest(IMAGE_URL, null, null, null));

    assertThat(response.diaryEntryId()).isNull();
  }

  // ── 가드 위임 (D5) — 목록·삭제 ──────────────────────────────────

  @Test
  @DisplayName("[REQ-11-12] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다 (목록)")
  void req_11_12_listStrangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.list(STRANGER, PET_ID, new CursorRequest(null, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-11-13] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다 (목록)")
  void req_11_13_listDeletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest(null, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-11-19] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다 (삭제)")
  void req_11_19_deleteStrangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.delete(STRANGER, PET_ID, PHOTO_A))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-11-20] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다 (삭제)")
  void req_11_20_deleteDeletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.delete(OWNER, PET_ID, PHOTO_A))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 자원 ↔ 펫 귀속 (D6 관례) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-11-21] 다른 펫에 속한 photo_id 는 RESOURCE_NOT_FOUND 다")
  void req_11_21_photoOfAnotherPetIsNotFound() {
    ownedByMe();
    when(repository.findByIdAndPetId(PHOTO_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(OWNER, PET_ID, PHOTO_A))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-11-22] 사진 조회에 pet_id 를 함께 건다 — findById 단독 호출이 없다")
  void req_11_22_neverLooksUpByIdAlone() {
    ownedByMe();
    when(repository.findByIdAndPetId(PHOTO_A, PET_ID))
        .thenReturn(Optional.of(photo(PHOTO_A, null, JUN_30)));
    when(s3Client.deleteObject((DeleteObjectRequest) any())).thenReturn(null);

    service.delete(OWNER, PET_ID, PHOTO_A);

    verify(repository, never()).findById(any());
  }

  // ── keyset 커서 ──────────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-11-14] next_cursor 페이로드에 마지막 항목의 id 가 실린다 (타이브레이크)")
  void req_11_14_nextCursorCarriesLastItemId() {
    ownedByMe();
    // limit 2 인데 같은 created_at 3건 — 저장소는 limit+1 을 돌려준다
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(
                photo(PHOTO_A, null, JUN_30),
                photo(PHOTO_B, null, JUN_30),
                photo(PHOTO_C, null, JUN_30)));

    CursorPage<PhotoResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    PhotoCursor cursor = codec.decode(page.nextCursor(), PhotoCursor.class);
    assertThat(cursor).isEqualTo(new PhotoCursor(JUN_30, PHOTO_B));
  }

  @Test
  @DisplayName("[REQ-11-15] 다음 페이지 조회는 created_at 과 id 를 둘 다 저장소에 넘긴다")
  void req_11_15_nextPagePassesBothKeysToRepository() {
    ownedByMe();
    String cursor = codec.encode(new PhotoCursor(JUN_30, PHOTO_B));

    service.list(OWNER, PET_ID, new CursorRequest(cursor, 2));

    verify(repository).findPageAfter(eq(PET_ID), eq(JUN_30), eq(PHOTO_B), any());
  }

  @Test
  @DisplayName("[REQ-11-16] limit 만큼만 돌려주고 has_next 가 켜진다")
  void req_11_16_returnsLimitItemsAndHasNext() {
    ownedByMe();
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(
                photo(PHOTO_A, null, JUN_30),
                photo(PHOTO_B, null, JUN_30),
                photo(PHOTO_C, null, JUN_30)));

    CursorPage<PhotoResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    assertThat(page.items()).hasSize(2);
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("[REQ-11-17] 해석 불가한 cursor 는 INVALID_CURSOR 다")
  void req_11_17_garbageCursorIsRejected() {
    ownedByMe();

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest("!!not-a-cursor!!", 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_CURSOR);
  }

  // ── 삭제 순서 (결정 — R2 성공 후에만 DB 삭제) ───────────────────

  @Test
  @DisplayName("[REQ-11-23] R2 삭제가 실패하면 저장소의 delete 가 호출되지 않는다")
  void req_11_23_r2FailurePreventsRepositoryDelete() {
    ownedByMe();
    Photo target = photo(PHOTO_A, null, JUN_30);
    when(repository.findByIdAndPetId(PHOTO_A, PET_ID)).thenReturn(Optional.of(target));
    when(s3Client.deleteObject((DeleteObjectRequest) any()))
        .thenThrow(new RuntimeException("R2 down"));

    assertThatThrownBy(() -> service.delete(OWNER, PET_ID, PHOTO_A));

    verify(repository, never()).delete(any());
  }

  @Test
  @DisplayName("[REQ-11-24] R2 삭제가 실패하면 예외가 그대로 전파된다")
  void req_11_24_r2FailurePropagatesException() {
    ownedByMe();
    Photo target = photo(PHOTO_A, null, JUN_30);
    when(repository.findByIdAndPetId(PHOTO_A, PET_ID)).thenReturn(Optional.of(target));
    when(s3Client.deleteObject((DeleteObjectRequest) any()))
        .thenThrow(new RuntimeException("R2 down"));

    assertThatThrownBy(() -> service.delete(OWNER, PET_ID, PHOTO_A))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("[REQ-11-25] R2 삭제가 성공하면 저장소의 delete 가 호출된다")
  void req_11_25_r2SuccessTriggersRepositoryDelete() {
    ownedByMe();
    Photo target = photo(PHOTO_A, null, JUN_30);
    when(repository.findByIdAndPetId(PHOTO_A, PET_ID)).thenReturn(Optional.of(target));
    when(s3Client.deleteObject((DeleteObjectRequest) any())).thenReturn(null);

    service.delete(OWNER, PET_ID, PHOTO_A);

    verify(repository).delete(target);
  }

  // ── 하드 삭제 엔티티 ──────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-11-26] Photo 는 소프트 딜리트 엔티티가 아니다")
  void req_11_26_entityHasNoSoftDelete() {
    assertThat(BaseSoftDeleteEntity.class.isAssignableFrom(Photo.class)).isFalse();
  }

  // ── PhotoLookup 구현 (Phase 2, diary 통합) ────────────────────────

  @Test
  @DisplayName("[REQ-11-27] 사진 3건이 연결된 다이어리 항목의 count 는 3이다")
  void req_11_27_countReturnsAttachedPhotoCount() {
    when(repository.countByDiaryEntryId(DIARY_ENTRY_ID)).thenReturn(3);

    int count = service.countByDiaryEntryId(DIARY_ENTRY_ID);

    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("[REQ-11-28] 연결된 사진이 없으면 count 는 0이다")
  void req_11_28_countReturnsZeroWhenNoPhotosAttached() {
    when(repository.countByDiaryEntryId(DIARY_ENTRY_ID)).thenReturn(0);

    int count = service.countByDiaryEntryId(DIARY_ENTRY_ID);

    assertThat(count).isZero();
  }

  @Test
  @DisplayName("[REQ-11-29] 사진 목록을 PhotoSummary 로 반환한다 — Photo 엔티티를 노출하지 않는다")
  void req_11_29_findReturnsPhotoSummaryNotEntity() {
    when(repository.findByDiaryEntryId(DIARY_ENTRY_ID))
        .thenReturn(List.of(photo(PHOTO_A, DIARY_ENTRY_ID, JUN_30)));

    List<PhotoSummary> summaries = service.findByDiaryEntryId(DIARY_ENTRY_ID);

    assertThat(summaries).containsExactly(new PhotoSummary(PHOTO_A, IMAGE_URL, null, null));
  }
}
