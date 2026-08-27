package com.petkok.data.pet.dto;

import com.petkok.data.pet.enums.Species;
import java.util.UUID;

/**
 * {@code PetAccessGuard} 가 하위 도메인(diary · feeding · activity · weight · shed · gallery)에 돌려주는 <b>읽기
 * 전용</b> 표현. 검증 계약 REQ-09-09 · 10 · 11 (PLAN-REQ-09 D3 · D4).
 *
 * <p><b>{@code Pet} 엔티티를 돌려주지 않는 이유가 이 record 의 존재 이유다</b> (D3). 엔티티를 넘기면 {@code data.pet} 을 통째로
 * ArchUnit 예외로 열어야 하고, 그러면 하위 도메인이 {@code PetRepository} 를 직접 주입해 <b>가드를 우회해도 규칙이 잡지 못한다</b>. 이 DTO
 * 와 {@code enums} 만 열어 두면 우회(repository)·엔티티 누출(entity)이 둘 다 잡힌다 — 2026-08-10 실측.
 *
 * <p>{@code species} 를 싣는 이유 — 종별 규칙(shed 는 게코만, activity 는 종별 허용값)은 <b>각 하위 Service 가 진입 시
 * 검증</b>한다(D4, Notion 「소스 구조」 §8). 가드는 소유권만 판정하고, 검증에 필요한 정보를 여기에 실어 보낸다.
 *
 * <p>⚠️ 여기에 필드를 더할 때는 "하위 도메인이 pet 의 무엇을 알아야 하는가"를 먼저 묻는다. 필요 이상을 실으면 응답 DTO({@code PetResponse})와
 * 역할이 겹치기 시작한다.
 */
public record OwnedPetResponse(UUID id, Species species) {}
