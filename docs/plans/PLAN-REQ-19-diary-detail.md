# PLAN-REQ-19 · 다이어리 상세 엔드포인트 (GET /diary/{entry_id})

> 출처: 현재 세션 대화(Notion `API I/F` 개발상태 전수 대조 중 발견) · 작성: 2026-09-21 · 상태: ✅ 완료 (Phase 1(유일) 완료 · 검증 계약 8건 전부 통과 · 미결 0건 · 브랜치 `feat/req19-diary-detail` → origin 푸쉬 완료, PR 미생성)

## 배경

Notion `API I/F` 개발상태를 컨트롤러 실제 구현과 전수 대조하던 중, `GET /pets/{pet_id}/diary/{entry_id}`(다이어리 상세)만 유일하게 `시작 전`으로 남아 있는 것을 발견했다.

`PLAN-REQ-10`을 추적한 결과, **명시적 제외 결정이 없다**:
- `## 범위 — 포함`: "엔드포인트 22행 (Diary **5** · Feeding 5 · Activity 4 · Weight 4 · Shed 5)" — Diary 5개로 스코프에 있었다
- `## 범위 — 제외`: timeline 이관·사진 연결 이관·`condition_tag` 7→4종·거꾸리 경고 파생, 4건뿐. 상세 제외는 없다
- Phase 5 헤딩: "diary (**5행**, 텍스트만)... **CRUD 5행**" — 그런데 완료 기준 문장·검증 계약(REQ-10-94~114, 21건) 어디에도 상세 조회 케이스가 없다. "CRUD"는 원래 4개 동작인데 "5행"(Notion 원본 행 수)을 그대로 갖다 붙인 표현으로 보이고, 그 밑에서 실제로 구현·테스트된 건 4개(작성·목록·수정·삭제)뿐이다

`DiaryController`·`DiaryService` 어디에도 단건 조회 메서드가 없다 — 계획에 적어 두고 실제 작업에서 조용히 빠진 것으로 보인다(REQ-11처럼 명시적으로 다른 REQ로 이관된 것도 아니다).

## 범위

**포함**

- `GET /pets/{pet_id}/diary/{entry_id}` 엔드포인트 1개 — `DiaryController`·`DiaryService`에 추가
- 기존 소유권 검증·에러 처리 패턴 재사용 — `update`/`delete`가 이미 쓰는 `PetAccessGuard.getOwnedPet` → `findOwnedEntry`(private, `RESOURCE_NOT_FOUND`) 체인을 그대로 씀. 새 가드 로직 없음
- 응답은 기존 `toDetailResponse(DiaryEntry)`(private, `create`/`update`가 쓰는 것과 동일)를 재사용 — `photos` 배열 포함, `photoCount`는 비움(REQ-10-108 뒤집힘·REQ-11-30 불변식을 그대로 따름)
- 컨트롤러 테스트 — AGENTS §6 관례(`@WebMvcTest` + `SecurityConfig`·`JacksonConfig` `@Import`)

**제외**

- **다이어리 도메인의 다른 어떤 것도 건드리지 않는다** — 작성·목록·수정·삭제는 이미 REQ-10에서 완료, 이번엔 상세 조회 1개만
- **Notion `API I/F` "다이어리 상세" 행의 `개발상태`는 구현·검증까지 끝난 뒤 `/checkpoint`가 반영한다** — 계획 단계에서 미리 바꾸지 않는다

## 결정

특별한 대안 검토 없이 기존 패턴을 그대로 재사용하는 작업이라 결정 표에 올릴 항목이 없다. 응답 shape(`photos` 포함)는 REQ-10-108(REQ-11에서 뒤집힘)이 이미 확정해 둔 불변식을 그대로 따르는 것이지 이번에 새로 정하는 게 아니다.

## 미결 질문

없음 — 기존 `update`/`delete`와 동일한 가드·에러 패턴을 그대로 재사용하는 범위라 새로 답해야 할 질문이 나오지 않았다.

## 작업 단계

- [x] **Phase 1 — GET /diary/{entry_id} 구현**
      완료 기준: `GET /pets/{pet_id}/diary/{entry_id}`가 200과 `photos` 배열을 포함한 `DiaryResponse`를 반환한다 · 존재하지 않거나 다른 펫 소유인 `entry_id` → 404 `RESOURCE_NOT_FOUND` · 남의 펫 → 403 `PET_FORBIDDEN` · 삭제된 펫 → 404 `PET_NOT_FOUND`(전부 `PetAccessGuard` 공용 동작, REQ-10-96·97과 동일) · 미인증 → 401 · 컨트롤러 테스트(AGENTS §6 관례) 통과 — **2026-09-21 `/testrun REQ-19`로 확인, 8건 전부 충족**

## 검증 계약

> 작성: 2026-09-21 · 대상: Phase 1 착수 직전 · 검증: `/testrun REQ-19`
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-19-01]` 형태로 박는다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-19-01 | `DiaryService.get` | 남의 펫이면 가드의 `PET_FORBIDDEN`이 그대로 나간다 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "남의 펫 → 403 `PET_FORBIDDEN`" | 1 | ✅ |
| REQ-19-02 | `DiaryService.get` | 삭제된 펫이면 가드의 `PET_NOT_FOUND`가 그대로 나간다 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "삭제된 펫 → 404 `PET_NOT_FOUND`" | 1 | ✅ |
| REQ-19-03 | `DiaryService.get` | 다른 펫에 속한(또는 존재하지 않는) `entry_id`는 `RESOURCE_NOT_FOUND`다 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "존재하지 않거나 다른 펫 소유인 `entry_id` → 404 `RESOURCE_NOT_FOUND`" | 1 | ✅ |
| REQ-19-04 | `DiaryService.get` | 응답의 `photos`에 `PhotoLookup` 조회 결과가 그대로 담긴다 | 정상 | PLAN §범위 — 포함 — "`toDetailResponse`... 재사용 — `photos` 배열 포함" | 1 | ✅ |
| REQ-19-05 | `GET /diary/{entry_id}` | 200을 반환한다 | 정상 | PLAN §작업 단계 Phase 1 완료 기준 — "200과 ... `DiaryResponse`를 반환한다" | 1 | ✅ |
| REQ-19-06 | `GET /diary/{entry_id}` | 응답에 `photos` 배열이 있다 | 정상 | PLAN §작업 단계 Phase 1 완료 기준 — "200과 `photos` 배열을 포함한" | 1 | ✅ |
| REQ-19-07 | `GET /diary/{entry_id}` | 토큰 없는 요청은 401이다 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "미인증 → 401" | 1 | ✅ |
| REQ-19-08 | `GET /diary/{entry_id}` | 토큰 없는 요청의 에러 코드는 `UNAUTHORIZED`다 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "미인증 → 401"(AGENTS §6 — "`error.code`를 단언한다") | 1 | ✅ |

## 제약·함정

- **응답 DTO(`DiaryResponse`)·서비스 private 헬퍼(`toDetailResponse`, `findOwnedEntry`)를 그대로 재사용한다.** 새로 만들지 않는다 — 상세·생성·수정이 같은 shape를 쓰는 게 REQ-11에서 이미 확정된 불변식이다(REQ-10-108)
- `DiaryController`·`DiaryService` javadoc이 "Diary 5행"이라고 이미 적어 두고 있었다(구현은 4개뿐이었음) — 이번 작업으로 그 주석이 비로소 사실이 된다
