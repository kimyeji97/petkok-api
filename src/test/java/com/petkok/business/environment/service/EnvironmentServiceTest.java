package com.petkok.business.environment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.environment.dto.EnvironmentCreateRequest;
import com.petkok.data.environment.repository.EnvironmentLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 게코 사육 환경(온습도) 기록의 <b>종별 제한</b>. 검증 계약 REQ-20-02 · 03 (PLAN-REQ-20 § 검증 계약). {@code
 * ShedServiceTest} 와 같은 구성 — 종별 가드가 {@code create}·{@code getDailySummary} 를 포함해 전 메서드 진입 시 적용된다고
 * 가정한다.
 *
 * <p>⚠️ 이 파일은 {@code EnvironmentService} 등 Phase 1 대상 클래스가 아직 없어 컴파일되지 않는다 — {@code /implement
 * REQ-20 1} 이 만든다.
 *
 * <p>가정한 계약 — {@code EnvironmentService(PetAccessGuard, EnvironmentLogRepository, CursorCodec)}
 * ({@code ShedService}와 동일하게 목록 커서 페이지네이션에 필요). {@code ShedService.validateGecko} 와 동일하게 진입 시 종을
 * 검증하고 게코 외 종은 {@code ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES} 를 던진다(PLAN-REQ-20 §결정 — "게코 전용,
 * `FEATURE_NOT_SUPPORTED_SPECIES` 재사용").
 */
class EnvironmentServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final EnvironmentLogRepository repository = mock(EnvironmentLogRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final EnvironmentService service = new EnvironmentService(guard, repository, codec);

  private void owned(Species species) {
    when(guard.getOwnedPet(PET_ID, OWNER)).thenReturn(new OwnedPetResponse(PET_ID, species));
  }

  private static EnvironmentCreateRequest create() {
    return new EnvironmentCreateRequest(
        new BigDecimal("24.5"), new BigDecimal("65.0"), OffsetDateTime.now(), null);
  }

  @Test
  @DisplayName("[REQ-20-02] 개 펫이 기록을 시도하면 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_20_02_dogCreateIsRejected() {
    owned(Species.DOG);

    assertThatThrownBy(() -> service.create(OWNER, PET_ID, create()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
  }

  @Test
  @DisplayName("[REQ-20-03] 개 펫이 일간 평균을 조회하면 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_20_03_dogDailySummaryIsRejected() {
    owned(Species.DOG);

    assertThatThrownBy(() -> service.getDailySummary(OWNER, PET_ID, TODAY))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
  }
}
