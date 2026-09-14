# PLAN-REQ-12 · timeline (월간 캘린더 + 이벤트 집계)

> 출처: 2026-09-10~11 세션(`/workplan req-12`, Notion API I/F 원본 재대조) · 작성: 2026-09-11 · 상태: 🟡 진행 (미결 전부 확정 · 착수 전제였던 REQ-17 `main` 병합 완료 · `/testgen` Phase 1 검증 계약 33건 작성 완료, 구현 전)

## 배경

REQ-12는 개발 순서상 마지막 도메인(auth → user → pet → 기록 도메인 → timeline, 소스 구조 §9)이고, 5개 원천 도메인(diary·feeding·activity·weight·shed)이 REQ-10에서 이미 전부 구현·검증돼 있어 착수 조건은 갖춰져 있었다.

**그런데 레포 파생 문서(`docs/specs/api-list.md §10`)가 실제 계약과 근본적으로 달랐다.** 이전 판은 "커서 기반 통합 시간순 목록"으로 적고 있었지만, 2026-09-11 Notion `API I/F`「통합 타임라인」행을 직접 열어보니 실제 계약은 **월 단위 캘린더 집계**다 — `year_month`+`type` 쿼리 파라미터를 받고 `cursor`/`limit`은 아예 없다. 응답도 평평한 병합 리스트가 아니라 `days[].{date, markers, events}`로 날짜별로 묶여 있다. 이 세션에서 `api-list.md §10`을 원본 그대로 다시 썼다(정정 완료).

**"캘린더 도트"로 알려졌던 것도 별도 미정의 기능이 아니었다.** 이전 판은 "API I/F에 엔드포인트가 정의되지 않아 Notion에 먼저 추가가 필요하다"고 적었는데, 실제로는 이 API 응답의 `markers` 필드가 정확히 그 기능이었다 — 이미 원본에 있었다.

## 범위

**포함**
- `GET /api/v1/pets/{pet_id}/timeline?year_month=YYYY-MM&type=all|feeding|activity|weight|shed|diary` 단일 엔드포인트 (원본 그대로)
- 5개 도메인(diary·feeding·activity·weight·shed) 리포지토리의 월간 조회를 조합해 날짜별로 묶는다 — **앱 레벨 병합(옵션 A)**, Notion 추천대로
- 응답 `{ data: { days: [ { date, markers: [...], events: [...] } ] } }` (Notion 원본 예시 그대로 — `api-list.md §10` 참고)
- `PetAccessGuard`로 소유권 검증 (다른 도메인과 동일 패턴, D5 재사용)
- `data/timeline/dto` 신설(응답 DTO만) — 소스 구조 문서가 이미 이 자리를 예정해 둠("DTO 필요해지면 REQ-12에서 추가")
- `business/timeline`만 사용 — 엔티티·리포지토리 없음. `DomainBoundaryTest`의 기존 `business.timeline` 예외를 재사용(REQ-09 Phase 0 이후 공허하게 존재해 오던 것이 이번에 처음 실제로 쓰인다)

**제외**
- **"펫 필터 전체"(다중 펫 통합 조회)** — Notion 원본이 스스로 "엔드포인트 스코프 미확정"이라고 명시했다(`api-list.md §10` 참고). URI 자체가 `/pets/{pet_id}/timeline`(단일 펫 경로)라 구조적으로도 안 맞는다 — 여러 펫을 묶으려면 다른 모양의 엔드포인트가 필요하다. 별도 REQ로 미룬다
- **gallery(photos) 통합** — `type` enum(`all|feeding|activity|weight|shed|diary`)에 `photos`가 없다(원본 확인). REQ-11과 무관하게 이 집계엔 안 들어간다
- **옵션 B(네이티브 `UNION ALL`/QueryDSL)** — 소스 구조 §9가 "무한스크롤 대량 조회에서 병목이 확인되면 도입"으로 못박아 둔 대안이다. 지금 도입 안 함(REQ-10 D8류 패턴과 동일 — 단순하게 시작, 필요해지면 최적화)
- **커서 페이지네이션(`CursorRequest`/`CursorPage`)** — 원본에 없다. 다른 9개 도메인이 전부 이 패턴을 쓰다 보니 습관적으로 넣기 쉬운데, 이 엔드포인트는 월 단위 조회라 커서 개념 자체가 없다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 조회 방식 | 월 단위(`year_month`) 집계, 커서 없음 | Notion `API I/F`「통합 타임라인」행 원본 확인(2026-09-11) — `cursor`/`limit` 파라미터가 아예 없고 `year_month`·`type`만 있다 | 레포 파생 문서(`api-list.md` 이전 판)의 "커서 기반 통합 시간순" — 원본 재대조로 기각, 이번에 문서 정정 |
| 캘린더 도트 기능 | 별도 엔드포인트 아님 — 이 API 응답의 `markers` 필드가 그 기능 | 원본 응답 예시가 `days[].markers`를 명시(그날 존재하는 기록 유형) | 별도 엔드포인트 신설(이전 판이 "Notion에 추가 필요"로 적어둠) — 이미 있었으므로 불필요 |
| 병합 방식 | 앱 레벨 병합(옵션 A) | 소스 구조 §9 "Notion 추천" | 네이티브 `UNION ALL`(옵션 B) — 병목 실측 전 도입 안 함 |
| gallery 포함 여부 | 미포함 | `type` enum에 `photos`/`gallery` 없음(원본 확인) | 포함 — 원본에 근거 없음 |
| 패키지 구조 | `business/timeline`(로직) + `data/timeline/dto`(응답 DTO만, entity·repository 없음) | 소스 구조 문서(AGENTS.md §3 이관 근거와 동일 계통)가 이미 이 형태로 예정 — "DTO 필요해지면 REQ-12에서 `data/timeline/dto` 추가" | `data/timeline` 자체를 계속 안 만듦 — 응답 DTO를 `business` 쪽에 두면 §3 "DTO 패키지는 `data/{도메인}/dto`" 원칙과 어긋난다 |
| 날짜 전용 도메인(diary·weight·shed)의 `occurred_at` | **방향 확정(2026-09-11) — KST 자정(`00:00:00+09:00`)으로 고정.** feeding·activity는 실제 `timestamptz`를 그대로 쓴다 | 이 세 도메인은 "하루에 여러 번, 순서가 의미 있는 사건"이 아니라 "그날의 관찰·측정 1건"이라 시각을 지어내면 없는 정밀도를 있는 것처럼 보이게 한다. 반대로 feeding·activity는 하루 여러 번 기록되고 순서 자체가 정보라 실제 시각이 필요 — 현재 스키마의 시각 유무 구분은 설계 의도이지 누락이 아니다 | `created_at`(감사 시각) 대체 — 기록을 "입력한" 시각이지 "발생한" 시각이 아니라 더 오해를 만든다 / 시각 없이 `date`만 응답 — 응답 타입을 일관되게 유지하기 어려움 |
| "펫 필터 전체" 범위 | **제외 유지(확정, 2026-09-11)** — 이번 REQ-12 범위 밖, 별도 REQ로 미룬다 | Notion 원본이 스스로 "엔드포인트 스코프 미확정"이라 명시했고, URI(`/pets/{pet_id}/timeline`)가 구조적으로 단일 펫 경로라 안 맞는다 | 이번에 포함 — 엔드포인트 모양(URI·쿼리) 재설계가 필요해 범위·일정이 커진다는 이유로 기각 |
| `events[]` 타입별 부가 필드 스키마 | **도메인별 선택 필드 허용(확정, 2026-09-11)** — 공통(`type`·`ref_id`·`occurred_at`·`summary`) + 도메인 고유 선택 필드: diary→`condition_tag`, feeding→`is_refused`, activity→`activity_type`, shed→`is_complete`·`is_assisted`, weight→없음(summary만) | Notion 원본 예시가 이미 diary에 `condition_tag`, feeding에 `is_refused`를 얹어 보여줬다 — 공통 필드만 쓰는 안은 원본과 어긋난다 | 공통 필드만 — 원본 예시와 불일치해 기각 |
| `summary` 텍스트 생성 규칙 | **확정(2026-09-11)** — diary: `title`(없으면 `content` 앞 20자, 둘 다 없으면 "일지 기록") · feeding: `"{foodType}({foodSize}) {amount}{amountUnit}"`(누락 필드는 생략, 원본 예시 "귀뚜라미(M) 5마리"와 동일 패턴) · activity: `"{activityType} {durationMinutes}분"`(`durationMinutes` 없으면 타입명만) · weight: `"{weightG}g"`(원본 예시 "62g") · shed: `isComplete`·`isAssisted` 조합 — "탈피 완료"/"탈피 도와줌"/"탈피 진행 중" | 원본에 조합 규칙이 없어 도메인 필드를 직접 확인(`FeedingLog`·`ActivityLog`·`WeightLog`·`ShedRecord`)한 뒤 원본 예시 패턴을 다른 도메인에도 일관 적용 | — |
| `type` 필터의 `markers` 적용 여부 | **markers도 type으로 필터링(확정, 2026-09-11)** | 사용자 결정 — 필터 의미를 더 강하게 가져간다 | markers는 항상 그날 전체 기록 유형(캘린더 도트 본래 목적 우선) — 기각 |

## 미결 질문

- [x] **`occurred_at`의 날짜 전용 도메인 처리 — 확정.** diary·weight·shed는 `occurred_at`을 KST 자정(`00:00:00+09:00`)으로 고정한다(2026-09-11). 별건으로 진행되던 컬럼명 조사가 **"이름만 변경"(`_at`인데 `date`형인 컬럼의 이름을 바꾼다 — 예: `measured_at` → `measured_date`)으로 결론 났다**(타입 변경 아님) — 그래서 이 결정은 그대로 유효하다.
      > ⚠️ **구현 시 주의 — 컬럼·필드명이 바뀐 뒤에 착수한다.** 사용자가 별도로 이 이름 변경 작업(PLAN-REQ-17)을 진행 중이고 **Notion 역반영은 이미 완료(2026-09-11)**했다. REQ-12 Phase 1은 REQ-17 **Phase 2(DB 마이그레이션 + 코드 리네임)가 끝난 뒤**, 바뀐 이름(`measured_date`·`taken_date`)을 기준으로 구현해야 한다 — 지금 `measured_at` 기준으로 코드를 쓰면 곧 다시 고쳐야 한다. 착수 전 `PLAN-REQ-17-at-date-column-naming.md` Phase 2가 `main`에 들어왔는지 확인할 것
      > ✅ **충족(2026-09-14) — PR #53으로 REQ-17 Phase 2가 `main`에 병합됐다.** 이제 `measured_date`·`taken_date` 기준으로 구현하면 된다.
- [x] **"펫 필터 전체" 범위 재확인 — 확정(2026-09-11).** 제외 유지, 별도 REQ로 미룬다(근거는 `## 결정` 표)
- [x] **`events[]` 항목의 타입별 부가 필드 스키마 — 확정(2026-09-11).** 도메인별 선택 필드 허용(근거는 `## 결정` 표)
- [x] **`summary` 텍스트 생성 규칙 — 확정(2026-09-11).** 도메인별 조합 규칙(근거는 `## 결정` 표)
- [x] **`type` 필터가 걸렸을 때 `markers`도 필터링되는지 — 확정(2026-09-11).** markers도 type으로 필터링(근거는 `## 결정` 표)
- [ ] **feeding summary의 "필드 누락 시 정확한 생략 형태" — 미확정(2026-09-14, `/testgen` 발견).** 원본 예시(`"귀뚜라미(M) 5마리"`)는 전 필드가 있는 경우뿐이라 `foodSize`·`amount`·`amountUnit` 중 일부가 없을 때 `()`·공백을 정확히 어떻게 생략하는지 근거가 없다. `/testgen`은 전 필드 케이스(REQ-12-13)만 쓰고 부분 누락 조합은 테스트하지 않았다 — 구현 시 확정 필요, 확정되면 검증 계약에 케이스 추가

## 작업 단계

- [ ] **Phase 1** — `GET /pets/{pet_id}/timeline` 구현 (월별 집계, 앱 레벨 병합)
      완료 기준: `year_month`+`type` 파라미터로 5개 도메인 조회 후 날짜별 `{date, markers, events}`로 병합 · `PetAccessGuard`로 403/404 검증 · `type` 필터 시 해당 도메인만 `events`에 포함하고 `markers`도 같은 타입으로 필터링 · diary·weight·shed의 `occurred_at`은 KST 자정 고정, feeding·activity는 실제 `timestamptz` · `events[]` 부가 필드는 도메인별 선택 필드(위 `## 결정` 표) · `summary`는 확정된 도메인별 조합 규칙 적용 · gallery 미포함 · "펫 필터 전체" 미구현 · `business.timeline` ArchUnit 예외가 실제로 작동함을 프로브로 확인(예외를 걷어낸 원본 규칙에서 정상 사용이 FAIL인지)
      > ⚠️ **착수 전제 — 미결 5건 전부 확정(2026-09-11)됐지만, 착수는 REQ-17 Phase 2(컬럼 리네임 `measured_date`·`taken_date`)가 `main`에 들어온 뒤로 미룬다.** 지금 `measured_at` 기준으로 짜면 곧 다시 고쳐야 한다 — 위 occurred_at 행의 구현 시 주의 참고.
      > ✅ **전제 충족(2026-09-14) — PR #53 머지로 `main`에 새 컬럼명이 들어왔다. Phase 1 착수 가능.**

## 검증 계약

> 작성: 2026-09-14 · 스펙: `docs/specs/api-list.md` §10 · 이 계획서(별도 스펙 문서 없음) · 검증: `/testrun REQ-12`

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-12-01 | `TimelineMerger` | 여러 도메인 기록이 같은 날 있으면 markers 에 그 타입이 모두 모인다 / 다른 날짜 기록은 서로 다른 day 로 분리된다 | 정상 | api-list.md §10 예시 — `"markers": ["diary","feeding","activity","weight"]` | 1 | — |
| REQ-12-02 | `TimelineMerger` | `type=weight` 면 `events` 엔 weight 만 남는다 | 정상 | PLAN §작업 단계 Phase 1 완료 기준 — "`type` 필터 시 해당 도메인만 `events`에 포함" | 1 | — |
| REQ-12-03 | `TimelineMerger` | `type=weight` 면 `markers` 도 weight 만 남는다 | 정상 | PLAN §결정 — "markers도 type으로 필터링(확정)" | 1 | — |
| REQ-12-04 | `TimelineMerger` | 하루 안 `events` 는 `occurred_at` 시간순 정렬 | 정상 | api-list.md §10 — "events — 선택일 상세 타임라인용(시간순 정렬)" | 1 | — |
| REQ-12-05 | `TimelineMerger` | diary `occurred_at` 은 KST 자정 고정 | 정상 | PLAN §결정 — "diary·weight·shed ... KST 자정(00:00:00+09:00)으로 고정" | 1 | — |
| REQ-12-06 | `TimelineMerger` | weight `occurred_at` 도 KST 자정 고정 | 정상 | 상동 | 1 | — |
| REQ-12-07 | `TimelineMerger` | shed `occurred_at` 도 KST 자정 고정 | 정상 | 상동 | 1 | — |
| REQ-12-08 | `TimelineMerger` | feeding `occurred_at` 은 `fed_at` 그대로 | 정상 | PLAN §결정 — "feeding·activity는 실제 timestamptz를 그대로 쓴다" | 1 | — |
| REQ-12-09 | `TimelineMerger` | activity `occurred_at` 은 `logged_at` 그대로 | 정상 | 상동 | 1 | — |
| REQ-12-10 | `TimelineMerger` | diary summary — title 있으면 그대로 | 정상 | PLAN §결정 — "title(없으면 content 앞 20자, 둘 다 없으면 '일지 기록')" | 1 | — |
| REQ-12-11 | `TimelineMerger` | diary summary — title 없고 content 있으면 앞 20자 | 경계 | 상동 | 1 | — |
| REQ-12-12 | `TimelineMerger` | diary summary — 둘 다 없으면 "일지 기록" | 경계 | 상동 | 1 | — |
| REQ-12-13 | `TimelineMerger` | feeding summary — 원본 예시("귀뚜라미(M) 5마리") 재현 | 정상 | api-list.md §10 예시 | 1 | — |
| REQ-12-14 | `TimelineMerger` | weight summary — 원본 예시("62g") 재현 | 정상 | api-list.md §10 예시 | 1 | — |
| REQ-12-15 | `TimelineMerger` | activity summary — durationMinutes 있으면 "{타입} {분}분" | 정상 | PLAN §결정 — `"{activityType} {durationMinutes}분"` | 1 | — |
| REQ-12-16 | `TimelineMerger` | activity summary — durationMinutes 없으면 타입명만 | 경계 | PLAN §결정 — "durationMinutes 없으면 타입명만" | 1 | — |
| REQ-12-17 | `TimelineMerger` | shed summary — complete·not-assisted → "탈피 완료" | 정상 | PLAN §결정 — "'탈피 완료'/'탈피 도와줌'/'탈피 진행 중'" | 1 | — |
| REQ-12-18 | `TimelineMerger` | shed summary — not-complete·assisted → "탈피 도와줌" | 정상 | 상동 | 1 | — |
| REQ-12-19 | `TimelineMerger` | shed summary — not-complete·not-assisted → "탈피 진행 중" | 정상 | 상동 | 1 | — |
| REQ-12-20 | `TimelineMerger` | shed summary — complete·assisted(둘 다 true) → "탈피 도와줌"(is_assisted 우선) | 경계 | `docs/PROGRESS.md` 2026-09-03 — "`shed_records.is_assisted` 자체가 이미 '탈피도와줌' 상태의 단일 출처" (ADR-0001) | 1 | — |
| REQ-12-21 | `TimelineMerger` | diary 이벤트에 `condition_tag` 포함 | 정상 | PLAN §결정 — "diary→condition_tag" | 1 | — |
| REQ-12-22 | `TimelineMerger` | feeding 이벤트에 `is_refused` 포함 | 정상 | PLAN §결정 — "feeding→is_refused" | 1 | — |
| REQ-12-23 | `TimelineMerger` | weight 이벤트엔 도메인 선택 필드 없음(summary만) | 정상 | PLAN §결정 — "weight→없음(summary만)" | 1 | — |
| REQ-12-24 | `TimelineService` | 남의 펫이면 가드의 PET_FORBIDDEN 그대로 전파 | 예외 | PLAN §작업 단계 Phase 1 완료 기준 — "PetAccessGuard로 403/404 검증" | 1 | — |
| REQ-12-25 | `TimelineService` | 없는 펫이면 가드의 PET_NOT_FOUND 그대로 전파 | 예외 | 상동 | 1 | — |
| REQ-12-26 | `TimelineService` | feeding·activity 월 조회는 KST 월 경계로 변환(반열린 구간) | 경계 | CLAUDE.md 시각 처리 절 — "계산 = Asia/Seoul"(ADR-0002) | 1 | — |
| REQ-12-27 | `TimelineController` | `GET /timeline?year_month=...` 200 + `data.days` 배열 | 정상 | PLAN §범위 — 엔드포인트·응답 형태 | 1 | — |
| REQ-12-28 | `TimelineController` | `type` 생략 시 서비스에 `ALL` 전달 | 정상 | api-list.md §10 — "type ...(기본 all)" | 1 | — |
| REQ-12-29 | `TimelineController` | `type=weight`(소문자) → 서비스에 `WEIGHT` 전달 | 정상 | api-list.md §10 — "all\|feeding\|activity\|weight\|shed\|diary" | 1 | — |
| REQ-12-30 | `TimelineController` | 남의 펫 → 403·`error.code=PET_FORBIDDEN` | 예외 | PLAN Phase1 완료 기준(REQ-09-12 동형) | 1 | — |
| REQ-12-31 | `TimelineController` | 없는 펫 → 404·`error.code=PET_NOT_FOUND` | 예외 | 상동 | 1 | — |
| REQ-12-32 | `TimelineController` | 응답 이벤트 키가 snake_case(`ref_id`,`occurred_at` 등) | 정상 | api-list.md §10 응답 예시 | 1 | — |
| REQ-12-33 | `DomainBoundaryTest` | `business.timeline` cross-domain 예외가 실제로 작동 — 예외를 걷어낸 원본 규칙에서는 FAIL | **프로브(수동)** | PLAN §작업 단계 Phase 1 완료 기준 — "`business.timeline` ArchUnit 예외가 실제로 작동함을 프로브로 확인" | 1 | — |

**REQ-12-33은 코드로 안 쓴다** — REQ-10-01~03과 같은 방식(`docs/plans/PLAN-REQ-10-record-domains.md` 참고)으로, `/implement` 단계에서 실제 timeline 코드가 cross-domain 참조를 하게 된 뒤 `git stash`로 `DomainBoundaryTest`의 `ignoreDependency(resideInAPackage("com.petkok.business.timeline.."), alwaysTrue())` 줄을 걷어내고 같은 프로브가 FAIL 하는지 수동 확인한다. `결과` 열엔 확인 후 "✅ 수동"을 적는다(`/checkpoint`).

## 제약·함정

- **이 엔드포인트는 `CursorRequest`/`CursorPage`를 쓰지 않는다.** 다른 9개 도메인이 전부 이 패턴이라 습관적으로 커서 파라미터를 넣기 쉽다 — 실제로 레포 파생 문서(`api-list.md` 이전 판)가 이 실수를 이미 한 번 했다(2026-09-11 발견·정정)
- **날짜 전용 컬럼과 타임스탬프 컬럼이 섞여 있다.** `fed_at`·`logged_at`(feeding·activity)은 `timestamptz`지만 `entry_date`·`measured_date`·`shed_date`(diary·weight·shed)는 `date`다 — 5개 도메인을 하나의 `occurred_at` 필드로 통일하려는 순간 이 차이가 정면으로 걸린다(미결 1)
- **`business.timeline`의 `DomainBoundaryTest` 예외는 지금까지 공허했다.** 대상 코드가 0개라 "예외가 실제로 작동하는지" 한 번도 검증된 적이 없다(REQ-09 Phase 0 이후 계속 공허 상태로 알고 남겨 둔 것 — `DomainBoundaryTest.java` 주석 참고). 이번이 처음 실제로 쓰이는 시점이라, 다른 새 ArchUnit 예외를 추가할 때처럼 프로브로 확인해야 한다
- **gallery(photos)를 "포함하는 게 자연스러워 보여서" 넣지 않는다.** `type` enum에 없다는 게 원본 확인 사항이다 — REQ-11이 방금 끝나 있어서 timeline에도 사진을 얹고 싶어질 수 있지만 원본 범위 밖이다
