package com.petkok.data.pet.dto;

import java.util.List;

/**
 * {@code GET /pets} 응답. 원본이 {@code {"data":{"items":[...]}}} 를 규정하므로 {@code items} 로 감싼다.
 *
 * <p><b>{@code next_cursor} 가 없다.</b> 원본 응답에 없어서다 — AGENTS §5 의 "페이지네이션은 커서 기반"과 어긋나 보이지만, 근거 없이
 * 만들지 않는다(PLAN-REQ-09 미결). 도입이 정해지면 이 record 에 필드가 붙는다.
 *
 * <p>⚠️ <b>컨트롤러 안에 중첩 record 로 두면 안 된다</b> — {@code ..controller..} 의 클래스는 {@code Controller} 로 끝나야
 * 한다는 ArchUnit 규칙에 걸린다(2026-08-10 실측). DTO 는 {@code data/{도메인}/dto} 가 제자리다.
 */
public record PetListResponse(List<PetResponse> items) {}
