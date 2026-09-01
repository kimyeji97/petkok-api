package com.petkok.business.feeding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.feeding.dto.FeedingCreateRequest;
import com.petkok.data.feeding.dto.FeedingResponse;
import com.petkok.data.feeding.dto.FeedingUpdateRequest;
import com.petkok.data.feeding.entity.FeedingLog;
import com.petkok.data.feeding.enums.FoodSize;
import com.petkok.data.feeding.repository.FeedingLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 급여 기록의 <b>가드 위임 · D6 · 커서 · 병합 · fed_at 미래 거부 · food_size 종 무관 저장</b>. 검증 계약 REQ-10-45 ~ 47 · 50
 * · 51 · 54 ~ 57 (PLAN-REQ-10 § 검증 계약). {@code WeightServiceTest} · {@code ActivityServiceTest} 와
 * 같은 구성.
 *
 * <p>⚠️ <b>이 파일은 {@code FeedingService} 등 Phase 3 대상 클래스가 아직 없어 컴파일되지 않는다.</b> {@code /testgen} 은
 * 케이스를 코드로 고정하는 역할까지만 하고, 대상 클래스는 {@code /implement REQ-10 3} 이 만든다(REQ-08·09·10 Phase 1·2 와 같은 순서
 * — PLAN-REQ-10 § 작업 단계 안내 참고).
 *
 * <p>가정한 계약 — {@code FeedingService(PetAccessGuard, FeedingLogRepository, CursorCodec, Clock)}.
 * {@code fed_at} 이 미래면 {@code ErrorCode.INVALID_INPUT}(REQ-10-54) — 계획서·원본 어디에도 전용 코드가 없어 기존 범용 코드를
 * 그대로 썼다. {@code food_size} 는 종을 검증하지 않고 그대로 저장한다(D13 과 같은 결).
 */
class FeedingServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_A = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
  private static final UUID LOG_B = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
  private static final UUID LOG_C = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

  // 고정 시각 — 2026-07-07T12:00:00+09:00 과 같은 순간이지만 Z 표기로 둔다. 이 파일의 CursorCodec 은
  // 앱의 JacksonConfig(timeZone=KST) 를 안 거친 무설정 ObjectMapper 라 인코드 시 오프셋을 UTC 로
  // 정규화한다(Jackson jsr310 기본값) — +09:00 리터럴을 쓰면 REQ-10-51 의 레코드 동등성 비교에서만
  // 어긋난다(순간은 같다). WeightServiceTest·ActivityServiceTest 는 애초에 UTC 리터럴이라 드러나지 않았다.
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-07T03:00:00Z");
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-07T03:00:00Z"), java.time.ZoneId.of("Asia/Seoul"));

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final FeedingLogRepository repository = mock(FeedingLogRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final FeedingService service = new FeedingService(guard, repository, codec, CLOCK);

  private static FeedingLog log(UUID id, boolean isRefused, OffsetDateTime fedAt) {
    FeedingLog log = FeedingLog.builder().petId(PET_ID).isRefused(isRefused).fedAt(fedAt).build();
    ReflectionTestUtils.setField(log, "id", id);
    return log;
  }

  private static FeedingCreateRequest create(OffsetDateTime fedAt) {
    return new FeedingCreateRequest(null, null, null, null, false, fedAt, null);
  }

  private static FeedingCreateRequest createWithFoodSize(FoodSize foodSize) {
    return new FeedingCreateRequest(
        "귀뚜라미", foodSize, new BigDecimal("5.00"), "마리", false, NOW.minusHours(1), null);
  }

  private void owned(Species species) {
    when(guard.getOwnedPet(PET_ID, OWNER)).thenReturn(new OwnedPetResponse(PET_ID, species));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── 가드 위임 ────────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-45] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_10_45_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.create(STRANGER, PET_ID, create(NOW.minusHours(1))))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-10-46] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다")
  void req_10_46_deletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest(null, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 기록 ↔ 펫 귀속 (D6) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-47] 다른 펫에 속한 기록 id 는 RESOURCE_NOT_FOUND 다")
  void req_10_47_recordOfAnotherPetIsNotFound() {
    owned(Species.DOG);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(
                    OWNER,
                    PET_ID,
                    LOG_A,
                    new FeedingUpdateRequest(null, null, null, null, null, null, "m")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // ── PATCH 병합 (D10) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-50] memo 만 보내면 amount 가 유지된다")
  void req_10_50_memoOnlyPatchKeepsAmount() {
    owned(Species.DOG);
    FeedingLog entry =
        FeedingLog.builder()
            .petId(PET_ID)
            .amount(new BigDecimal("5.00"))
            .isRefused(false)
            .fedAt(NOW.minusHours(1))
            .build();
    ReflectionTestUtils.setField(entry, "id", LOG_A);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.of(entry));

    service.update(
        OWNER, PET_ID, LOG_A, new FeedingUpdateRequest(null, null, null, null, null, null, "아침"));

    assertThat(entry.getAmount()).isEqualByComparingTo("5.00");
  }

  // ── keyset 커서 (D8) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-51] next_cursor 페이로드에 마지막 항목의 id 가 실린다")
  void req_10_51_nextCursorCarriesLastItemId() {
    owned(Species.DOG);
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(
                log(LOG_A, false, NOW.minusHours(1)),
                log(LOG_B, false, NOW.minusHours(1)),
                log(LOG_C, false, NOW.minusHours(1))));

    CursorPage<FeedingResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    assertThat(codec.decode(page.nextCursor(), FeedingCursor.class))
        .isEqualTo(new FeedingCursor(NOW.minusHours(1), LOG_B));
  }

  @Test
  @DisplayName("[REQ-10-51] 다음 페이지 조회는 fed_at 과 id 를 둘 다 저장소에 넘긴다")
  void req_10_51_nextPagePassesBothKeysToRepository() {
    owned(Species.DOG);
    when(repository.findPageAfter(any(), any(), any(), any())).thenReturn(List.of());

    service.list(
        OWNER,
        PET_ID,
        new CursorRequest(codec.encode(new FeedingCursor(NOW.minusHours(1), LOG_B)), 2));

    verify(repository).findPageAfter(eq(PET_ID), eq(NOW.minusHours(1)), eq(LOG_B), any());
  }

  // ── fed_at 미래 거부 (미결 질문 Phase 3) ────────────────────

  @Test
  @DisplayName("[REQ-10-54] fed_at 이 서버 현재 시각보다 미래면 거부된다")
  void req_10_54_futureFedAtIsRejected() {
    owned(Species.DOG);

    assertThatThrownBy(() -> service.create(OWNER, PET_ID, create(NOW.plusMinutes(1))))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  @DisplayName("[REQ-10-55] fed_at 이 어제(과거)면 정상 저장된다")
  void req_10_55_pastFedAtIsAccepted() {
    owned(Species.DOG);

    assertThatCode(() -> service.create(OWNER, PET_ID, create(NOW.minusDays(1))))
        .doesNotThrowAnyException();
  }

  // ── food_size 종 무관 저장 (미결 질문 Phase 3) ──────────────

  @Test
  @DisplayName("[REQ-10-56] food_size 없이 요청하면 정상 저장되고 null 이다")
  void req_10_56_missingFoodSizeIsStoredAsNull() {
    owned(Species.CRESTED_GECKO);

    FeedingResponse response = service.create(OWNER, PET_ID, create(NOW.minusHours(1)));

    assertThat(response.foodSize()).isNull();
  }

  @Test
  @DisplayName("[REQ-10-57] 개 펫이 food_size 를 보내도 그대로 저장되고 예외가 없다")
  void req_10_57_dogFoodSizeIsStoredAsIs() {
    owned(Species.DOG);

    FeedingResponse response = service.create(OWNER, PET_ID, createWithFoodSize(FoodSize.M));

    assertThat(response.foodSize()).isEqualTo(FoodSize.M);
  }
}
