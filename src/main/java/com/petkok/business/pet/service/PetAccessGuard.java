package com.petkok.business.pet.service;

import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.entity.Pet;
import com.petkok.data.pet.repository.PetRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소유권 앵커. {@code /pets/{petId}/...} 하위 도메인(diary · feeding · activity · weight · shed · gallery)은
 * 진입 시 <b>반드시</b> 이 가드를 통과한다 (Notion 「소스 구조」 §3 · PLAN-REQ-09 D1 · D3 · D4). 검증 계약 REQ-09-09 ~ 13.
 *
 * <p><b>돌려주는 것은 {@link OwnedPetResponse} 이지 {@link Pet} 이 아니다</b> (D3). 하위 도메인에게 pet 은 남의 도메인이고,
 * AGENTS §5 "Entity 는 Service 밖으로 나가지 않는다"가 그대로 적용된다. 하위 도메인이 열어 볼 수 있는 패키지는 {@code data.pet.dto} ·
 * {@code data.pet.enums} · 이 패키지 셋뿐이며, {@code entity} · {@code repository} 는 ArchUnit({@code
 * DomainBoundaryTest})이 닫아 둔다 — 그래서 <b>{@code PetRepository} 를 직접 주입하는 우회가 규칙에 걸린다</b>.
 *
 * <p><b>종(species) 검증은 여기서 하지 않는다</b> (D4). 이 시그니처에는 "호출한 엔드포인트가 기대하는 종"이 들어오지 않으므로 할 수도 없다. 가드는
 * 소유권만 판정하고 {@code species} 를 DTO 에 실어 넘기며, 규칙은 각 하위 Service 가 진입 시 적용한다.
 *
 * <p>없는 것(404)과 남의 것(403)은 구분한다 — {@link PetService#findOwned} 와 같은 규칙이다. 삭제된 펫은 {@code
 * findByIdAndDeletedAtIsNull} 이 걸러 404 가 된다(D6).
 */
@Component
public class PetAccessGuard {

  private final PetRepository petRepository;

  public PetAccessGuard(PetRepository petRepository) {
    this.petRepository = petRepository;
  }

  /**
   * 활성 펫이 {@code userId} 의 것인지 확인하고 읽기 전용 표현을 돌려준다.
   *
   * @throws BusinessException {@code PET_NOT_FOUND}(없거나 삭제됨) · {@code PET_FORBIDDEN}(남의 것)
   */
  @Transactional(readOnly = true)
  public OwnedPetResponse getOwnedPet(UUID petId, UUID userId) {
    Pet pet =
        petRepository
            .findByIdAndDeletedAtIsNull(petId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    if (!pet.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.PET_FORBIDDEN);
    }
    return new OwnedPetResponse(pet.getId(), pet.getSpecies());
  }
}
