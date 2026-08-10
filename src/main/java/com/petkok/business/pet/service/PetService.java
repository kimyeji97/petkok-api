package com.petkok.business.pet.service;

import com.petkok.data.pet.dto.PetCreateRequest;
import com.petkok.data.pet.dto.PetResponse;
import com.petkok.data.pet.dto.PetUpdateRequest;
import com.petkok.data.pet.entity.Pet;
import com.petkok.data.pet.repository.PetRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 반려동물 CRUD. 검증 계약 REQ-09-01 ~ 08 · 14 ~ 19 (PLAN-REQ-09 § 검증 계약). */
@Slf4j
@Service
public class PetService {

  private final PetRepository petRepository;

  public PetService(PetRepository petRepository) {
    this.petRepository = petRepository;
  }

  @Transactional
  public PetResponse create(UUID userId, PetCreateRequest request) {
    Pet pet =
        petRepository.save(
            Pet.builder()
                .userId(userId)
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .gender(request.gender())
                .birthday(request.birthday())
                .adoptionDate(request.adoptionDate())
                .profileImageUrl(request.profileImageUrl())
                .build());
    return toResponse(pet);
  }

  @Transactional(readOnly = true)
  public List<PetResponse> findMyPets(UUID userId) {
    return petRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
        .map(PetService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public PetResponse findOne(UUID userId, UUID petId) {
    return toResponse(findOwned(userId, petId));
  }

  /**
   * 프로필 수정. <b>보낸 필드만 반영한다</b> (D5). 검증 계약 REQ-09-05 · 17.
   *
   * <p>⚠️ <b>병합을 여기서 하는 것이 핵심이다</b> (AGENTS §5) — {@link Pet#updateProfile} 은 받은 값을 그대로 쓰므로, 요청 값을
   * 그냥 넘기면 <b>안 보낸 필드가 지워지고 응답은 200 으로 정상</b>이다.
   *
   * <p>{@code species} 는 애초에 요청 DTO 에도, {@code updateProfile} 파라미터에도 없다 — 두 겹으로 막혀 있다.
   */
  @Transactional
  public PetResponse update(UUID userId, UUID petId, PetUpdateRequest request) {
    Pet pet = findOwned(userId, petId);

    pet.updateProfile(
        request.name() != null ? request.name() : pet.getName(),
        request.breed() != null ? request.breed() : pet.getBreed(),
        request.gender() != null ? request.gender() : pet.getGender(),
        request.birthday() != null ? request.birthday() : pet.getBirthday(),
        request.adoptionDate() != null ? request.adoptionDate() : pet.getAdoptionDate(),
        request.profileImageUrl() != null ? request.profileImageUrl() : pet.getProfileImageUrl());

    return toResponse(pet);
  }

  /**
   * 삭제 (소프트 딜리트). <b>연관 기록은 건드리지 않는다</b> — 원본이 "연관 기록(일지/식사/갤러리) 보존"을 규정한다. 하위 테이블에는 {@code
   * deleted_at} 이 없으므로 행은 그대로 남는다.
   */
  @Transactional
  public void delete(UUID userId, UUID petId) {
    findOwned(userId, petId).softDelete();
    log.info("Pet soft-deleted. petId={}", petId);
  }

  /**
   * 소유권까지 확인해 활성 펫을 찾는다. 검증 계약 REQ-09-07 · 08 · 19.
   *
   * <p><b>없는 것과 남의 것을 구분한다</b> — 조회에서 소유자까지 걸러 버리면 남의 펫도 404 가 되어 {@code PET_FORBIDDEN} 이 존재할 이유가
   * 없어진다. 삭제된 펫은 {@code findByIdAndDeletedAtIsNull} 이 걸러 <b>404</b> 가 된다(D6).
   */
  private Pet findOwned(UUID userId, UUID petId) {
    Pet pet =
        petRepository
            .findByIdAndDeletedAtIsNull(petId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    if (!pet.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.PET_FORBIDDEN);
    }
    return pet;
  }

  private static PetResponse toResponse(Pet pet) {
    return new PetResponse(
        pet.getId(),
        pet.getName(),
        pet.getSpecies(),
        pet.getBreed(),
        pet.getGender(),
        pet.getBirthday(),
        pet.getAdoptionDate(),
        pet.getProfileImageUrl(),
        pet.getCreatedAt());
  }
}
