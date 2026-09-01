# PLAN-REQ-10 · 기록 도메인 5종 (weight · activity · feeding · shed · diary)

> 출처: 2026-08-27 세션 (REQ-09 완료 · 미결 12건 처리 직후) · 작성: 2026-08-27 · 상태: 🟡 진행 (Phase 0~3 완료 2026-09-01 · 다음은 Phase 4 shed)

## 배경

`/pets/{pet_id}/...` 아래에 매달리는 기록 API 23행(Notion `API I/F`)이 전부 `시작 전`이다. 이 REQ 가 프로젝트의 차별점 — **게코 특화 로직**(거식 스트릭 · 탈피 예측 · 종별 분기)이 전부 여기 있다.

REQ-09 가 이 다섯 도메인이 **그대로 복제할 형태**를 확정해 뒀다: 진입 시 `PetAccessGuard.getOwnedPet(petId, userId)` → `OwnedPetResponse(id, species)` · 종 검증은 각 Service · 삭제된 펫의 하위 기록은 API 로 닿지 않는다(가드 404). 이 REQ 는 그 형태가 **실제로 다섯 번 쓰일 수 있는지**를 증명하는 자리이기도 하다.

**원본을 직접 읽어야 했던 이유** — 2026-08-27 에 23행을 전부 읽었고, 파생 요약(`api-list §4~8`)·테이블 정의서와 **어긋나는 지점 4건**이 나왔다(아래 결정 D1~D4). REQ-09 에서 본 것과 같다 — 파생 요약은 양방향으로 배신한다.

## 범위

**포함**

- **엔드포인트 22행** (Notion `API I/F` · Diary 5 · Feeding 5 · Activity 4 · Weight 4 · Shed 5). 전부 🔒 인증 필요
- `business/{도메인}/{controller,service}` + `data/{도메인}/{entity,repository,dto,enums}` × 5
- **파생 계산기 2 + 파생 필드 1** — `AnorexiaStreakCalculator`(feeding) · `ShedPredictionCalculator`(shed) · 체중 변화율(weight, D3). 전부 **저장하지 않고 조회 시 계산**, 계산기는 I/O 없는 순수 클래스(Notion 「소스 구조」 §1-4 · §8)
- **종별 분기** — shed 는 `CRESTED_GECKO` 만(`SHED_NOT_SUPPORTED_SPECIES`) · activity 는 게코 `HANDLING` 만, 개/고양이 `WALK | PLAY | GROOMING | TRAINING`(`INVALID_SPECIES_ACTIVITY`) · 거식 스트릭은 게코 전용
- **커서 페이지네이션** — 목록 5종 전부 원본 응답이 `{items, next_cursor, has_next}` 다(= `CursorPage`). `framework/pagination` 을 첫 소비
- **V3 마이그레이션 1건** — `feeding_logs.food_size` (D2)
- `DomainBoundaryTest` 예외 3건 추가 (Phase 0 — REQ-09 이관)
- 컨트롤러 테스트 — AGENTS §6 관례

**원본 Validation (Notion `API I/F` 에서 옮겨 적음, 2026-08-27 — `/testrun` 인용 검사가 파일만 보기 때문)**

- 다이어리 작성: "`entry_date` 필수 / `condition_tag`: `정상 | 활발 | 거꾸리 | 구토` (거식·탈피는 급여·탈피 기록에서 파생, 단일 출처)" · callout "미래 날짜 불가"
- 급여 기록: "`fed_at` 필수 / `is_refused` 필수 / `food_size` 선택: `S | M | L` (🦎 게코 곤충 사이즈, 개/고양이 미사용)" · callout "당일 시간만 허용"
- 활동 기록: "`activity_type` 필수: `WALK | PLAY | GROOMING | TRAINING | HANDLING` (🦎 게코=`HANDLING`만, 개/고양이=`WALK | PLAY | GROOMING | TRAINING`) / `distance_km` 선택(게코 미사용) / `logged_at` 필수"
- 체중 기록: "`weight_g` 필수 (양의 정수) / `measured_at` 필수" · callout "직전 대비 20% 이상 증감 시 경고 표시"
- 탈피 기록: "`shed_date` 필수" · callout "완료일은 시작일 이후여야 함"(⚠️ 컬럼이 없다 — 미결)
- 거식 스트릭: "level 값: `NONE` | `CAUTION` (3일+) | `DANGER` (7일+)"
- 탈피 예측: "confidence 값: `LOW` (기록 1개) | `MEDIUM` (기록 2개) | `HIGH` (기록 3개+)" · callout "기록 3회 이상 시 실제 간격 평균으로 자동 보정"
- 다이어리 목록: "`cursor` / `limit` (기본 20, 최대 50) / `condition_tag` (상태 태그 필터)" · 급여 목록: "`cursor` / `limit` (기본 20)"

**제외**

- **`GET /pets/{pet_id}/timeline`** — `API I/F` 에 Diary 도메인 행으로 있지만 5개 테이블 union 집계라 **REQ-12** 다(「소스 구조」 §9). 원본 자신이 "펫 필터 전체 — 엔드포인트 스코프 미확정"이라 적고 있다
- **다이어리 ↔ 사진 연결** (`photo_ids` · `photos[]` · `photo_count`) — **REQ-11(gallery) 로 이관**(D4, 2026-08-27). 이번 diary 는 텍스트만이고 **응답 필드가 원본보다 적다** — 상세의 `photos`, 목록의 `photo_count` 를 넣지 않는다
- **`condition_tag` 7종** (거식·탈피도와줌·탈피완료) — D1 로 4종 확정. 테이블 정의서 §·「소스 구조」 §8 ·`api-list §4` 의 7종 문구는 **역반영 대상**
- **거꾸리 경고 파생** — 「소스 구조」 §8 이 `DiaryService` 에 "condition_tag='거꾸리' 기간 집계 → 경고"를 두지만 **`API I/F` 어느 행에도 그 응답이 없다.** 원본 없이 만들지 않는다 → 미결
- **`GET /weights/chart`** — 이전 판 api-list 에 있던 것, 원본에 없음(이미 제거됨)
- **커서 `limit` 상한 재조정** — 원본 다이어리 목록만 "최대 50", `CursorRequest.MAX_LIMIT` 은 100. 이번엔 framework 를 건드리지 않는다 → 미결
- **탈퇴 시 하위 기록 처리** — REQ-09 결정("탈퇴 시 pets 그대로") 과 같은 이유로 손대지 않는다

## 결정

> **D1~D4 는 2026-08-27 `AskUserQuestion` 으로 확정.** D5 이후는 REQ-09 확정 사항·프로젝트 규약에서 도출한 것이고, 근거가 원본에 없는 것은 미결로 뺐다.

| 항목 | 결정 | 근거 | 기각한 안 |
|---|---|---|---|
| **D1** `condition_tag` 허용값 | **4종 — `정상 · 활발 · 거꾸리 · 구토`** (`API I/F` 다이어리 작성 행). `ConditionTag` enum 4개. `V1__init.sql` 주석(4종)과 일치해 V1 을 건드릴 일이 없다 | → [ADR-0001](../adr/ADR-0001-derived-state-single-source.md) | ↑ (7종 문구 3곳은 역반영 대상) |
| **D2** `food_size` (S/M/L) | **`feeding_logs` 에 컬럼 추가 — `V4__feeding_food_size.sql`** (`food_size varchar(1) null`) | `API I/F` 급여 기록·목록이 **응답 계약에 `food_size` 를 포함**한다. 스키마가 계약보다 늦은 것이지 계약이 틀린 게 아니다. 게코 곤충 사이즈는 `amount`(마리 수)·`amount_unit` 과 **다른 축**이라 매핑으로 갚을 수 없다. 테이블 정의서 역반영 대상 | **`amount`/`amount_unit` 매핑**(`api-list §5` "엔티티는 그대로") — 응답에서 `food_size` 가 사라져 계약 위반. **필드 제거 + Notion 수정** — 게코 사용자에게 곤충 사이즈는 실사용 정보 |
| **D3** 체중 20% 경고 | **응답에 파생 필드** — 저장하지 않고 조회 시 직전 기록과 비교해 계산. **2026-08-28 확정(Notion 「체중 목록」 행에 먼저 명시)**: `weight_change_rate`(직전 대비 %, 소수 1자리, 첫 기록 `null`) · `is_weight_warning`(`\|변화율\| >= 20` → `true`, 첫 기록 `false`) · 직전 = `measured_at` desc, `id` desc 정렬에서 바로 다음 1건 | 원본 callout 이 목록·기록 두 행에 명시. 「소스 구조」 §1-4 "파생 상태는 저장하지 않고 계산" | **클라이언트 계산** — 서버 응답에 없으면 원본 문구가 공허해지고, 목록 API 소비자마다 다르게 구현한다 |
| **D4** 다이어리 ↔ 사진 | **REQ-11 로 이관.** 이번 diary 는 `photo_ids` 무시 · `photos` · `photo_count` 미포함 | `photos` 테이블·업로드가 REQ-11 이고, diary 가 photos 엔티티를 참조하면 **도메인 간 참조(diary→gallery)** 가 생겨 ArchUnit 예외 또는 포트 설계가 필요하다. 그 설계는 gallery 가 있어야 할 수 있다 | **REQ-10 에 포함** — 범위가 두 도메인으로 번진다. diary 를 마지막 Phase 로 두는 이유이기도 하다(D9) |
| **D5** 가드 소비 형태 | `XxxService` 가 `PetAccessGuard` 주입 → 진입 시 `getOwnedPet(petId, userId)` → `species` 로 분기. **`PetRepository`·`Pet` 은 참조하지 않는다** | REQ-09 D3·D4. ArchUnit 예외 3건(`business.pet.service` · `data.pet.dto` · `data.pet.enums`)이 정확히 이 형태만 허용한다 | (REQ-09 에서 기각한 A·B2′·C 안 — 재론하지 않는다) |
| **D6** 하위 기록의 소유 확인 | 기록 조회는 **`findByIdAndPetId(id, petId)`** — 없으면 `RESOURCE_NOT_FOUND`(404). 펫 소유권은 가드가, 기록↔펫 귀속은 이 조회가 | 남의 펫의 기록 id 를 내 펫 경로로 부르면 가드는 통과한다(내 펫이니까). 기록 쪽에서 `pet_id` 를 함께 걸지 않으면 **남의 기록이 보인다.** 원본에 없어 규약이지만, 없으면 보안 결함이라 결정으로 둔다. 403 이 아니라 404 인 이유 — "그 펫에 그런 기록은 없다"가 사실이고, 존재 여부를 흘리지 않는다 | **`findById` 후 `pet_id` 비교 → 403** — 남의 기록이 존재한다는 정보가 샌다 |
| **D7** 삭제 = 하드 삭제 | 5개 테이블 전부 `deleted_at` 없음 → `delete` 는 행 삭제, 204 | `V1__init.sql`(하위 테이블에 `deleted_at` 없음) · 원본 "204 · 응답 바디 없음" · 「소스 구조」 §5 "users·pets 만 소프트 딜리트" | **소프트 딜리트로 통일** — 스키마 변경 + 원본 근거 없음 |
| **D8** 목록 = 커서(keyset) | 정렬 키 = 원본 "최신순" 의 날짜 컬럼 desc + `id` desc 타이브레이크. 커서 페이로드 = `(정렬키, id)`, `CursorCodec` 으로 opaque. `limit` 은 `CursorRequest` 보정(기본 20) | 원본 5행 응답이 전부 `{items, next_cursor, has_next}` = `CursorPage`. 인덱스가 이미 `(pet_id, 날짜 desc)` 로 잡혀 있다(`V1__init.sql`) | **offset** — 「소스 구조」 §6 "커서 페이지네이션" 규약 위반. **날짜만 커서** — 같은 날짜 여러 건에서 누락·중복 |
| **D9** Phase 순서 | **weight → activity → feeding → shed → diary** | weight 가 가장 단순(파생 필드 1, 종 분기 없음)해서 **가드 소비 패턴·커서·D6 을 여기서 확정**하고 나머지가 복제한다. activity 는 종 분기만, feeding 은 +V3+계산기, shed 는 게코 전용+계산기, diary 는 D4 로 REQ-11 순서와 얽혀 마지막 | **diary 먼저**(원본 "Diary" 가 도메인 목록 첫째) — 사진 연결 미결과 얽혀 첫 Phase 가 가장 불확실해진다 |
| **D10** PATCH 의미론 | REQ-08 D3 그대로 — 누락·`null` = "변경 없음", DTO 에 `@NotNull`/`@NotBlank` 금지, 병합은 서비스 | 원본 5행 전부 "변경할 필드만 포함". AGENTS §5 | (REQ-08 에서 기각한 `JsonNullable`·`""` 안) |
| **D11** 엔티티 베이스 | diary = `BaseTimeEntity`(`updated_at` 있음), 나머지 4 = `BaseCreatedEntity` | `V1__init.sql` 컬럼 · 「소스 구조」 §5. ⚠️ **diary 응답에는 `updated_at` 이 들어간다** — 원본 목록·상세 예시에 있다. pet·user 와 다르다 | — |
| **D13** `distance_km` 처리 | **거부하지 않는다 — 종·활동 유형과 무관하게 보낸 값을 그대로 저장, 안 보내면 `null`** (2026-08-28 확정, Notion 「활동 기록」 행에 먼저 명시) | 원본 "선택(게코 미사용)"·테이블 정의서 "실내 활동·게코는 사용하지 않음"은 **입력 UI 가 숨긴다**는 뜻이지 서버 거부 규약이 아니다. 원본에 없는 거부 규약을 만들지 않는다(REQ-09 D5 와 같은 결). 서버가 거부하는 것은 종별 `activity_type` 뿐 | **400 `INVALID_INPUT`** — 명시적이지만 원본에 없는 규약이고, 개/고양이의 실내 활동(`PLAY` 등)에 거리가 올 때까지 규칙을 늘려야 한다 |
| **D12** 시각 타입 | `fed_at`·`logged_at` = `LocalDateTime`(기존 규약, `hibernate.jdbc.time_zone: UTC`). 요청·응답은 ISO-8601 `Z` | 원본 예시 `"2026-06-30T18:00:00Z"`. REQ-07 실측(PROGRESS 07-30) — 앱은 UTC 로 저장하고 DB `now()` 는 KST 라 **SQL 로 심은 픽스처는 9시간 어긋난다** | `OffsetDateTime`/`Instant` 전환 — 기존 엔티티 전부와 갈라진다 |

## 미결 질문

> ⚠️ 원본에 답이 없거나 원본끼리 어긋나는 것들. 추측으로 채우지 않았다. **각 Phase 착수 전에 그 Phase 의 미결을 닫는다** — 안 닫히면 그 부분은 구현하지 않는다.

**Phase 1 (weight) 전**
- [x] **체중 경고의 응답 형태 — 2026-08-28 확정, D3 에 반영.** Notion 「체중 목록」 행에 파생 필드 절 · 「체중 기록」 행에 201 응답 형태 명시. (원문:) D3 은 "파생 필드"까지만. 필드명(`change_rate`? `is_warning`?) · 직전 기록의 정의(`measured_at` 직전 1건? 같은 날 2건이면?) · 첫 기록(직전 없음)일 때 값 · 20% 가 "이상"인지 "초과"인지. **Notion 체중 목록·기록 행에 먼저 명시**한 뒤 옮겨 적는다
- [x] **목록 API 의 `cursor`/`limit` — 2026-08-28 역반영 완료.** 활동·체중·탈피 목록 행에 `Query Parameters: cursor / limit (기본 20)` + 정렬키(`logged_at`/`measured_at`/`shed_date` desc, `id` desc) 추가. (원문:) activity·weight·shed 목록은 응답에 `next_cursor` 만 있고 Query Parameters 절이 없다. 같은 규약으로 간주하되 **원본 3행에 파라미터 절을 추가**하는 역반영이 필요하다

**Phase 2 (activity) 전**
- [x] **게코가 `distance_km` 를 보내면 — 거부하지 않고 그대로 저장 (D13 확정, 2026-08-28).** Notion 「활동 기록」 행에 먼저 명시했다. (원문:) 원본 "선택(게코 미사용)". 무시하고 `null` 저장 vs 400. REQ-09 D5(`PATCH` 의 `species` 무시)와 같은 결로 "무시"가 자연스럽지만 원본이 말하지 않는다

**Phase 3 (feeding) 전**
- [x] **`fed_at` — 미래 시각만 거부 (2026-08-28 확정 · 2026-09-01 Notion 역반영 완료 — 「급여 기록」 callout·Validation).** 서버 현재 시각 이전이면 과거 소급을 허용한다. **순간 비교라 타임존 논쟁이 사라진다** — "오늘 날짜만" 안은 자정 직후 입력과 "어제 급여를 오늘 입력"을 막아 기각. (원문:) **`fed_at` "당일 시간만 허용"의 뜻.** ① 오늘 날짜만(어제 기록 불가?) ② 미래만 불가 ③ 서버 현재 시각 이전만. "당일"의 기준 타임존(KST? 클라이언트?)도 없다
- [x] **거식 스트릭 = 일수 기준 (2026-08-28 확정 · 2026-09-01 Notion 역반영 완료 — 「🦎 거식 스트릭 조회」 응답 예시의 `level` 을 `DANGER`(자기모순)에서 `CAUTION` 으로 정정).** 마지막 `is_refused = false` 급여(`last_eaten_at`)부터 기준 시각까지의 **KST 달력 일수**. `>= 7` DANGER · `>= 3` CAUTION · 나머지 NONE(원본 "3일+"은 3 포함). 기록 0건이면 `{0, NONE, null}`. **연속 거식 건수 안은 기각** — 게코는 며칠에 한 번 먹는 것이 정상이라 급여 시도 횟수에 의존하면 "7일"과 맞지 않는다. (원문:) **거식 스트릭 계산 규칙.** `current_streak_days` 의 정의 — 마지막 `is_refused = false` 급여(`last_eaten_at`) 이후 **일수**인가, 연속 `is_refused = true` 기록 **건수**인가. 기준 시각(요청 시각 KST? UTC?). 기록이 0건일 때 응답. `CAUTION (3일+)` 가 `>= 3` 인지. **계산기의 검증 계약은 이 답이 있어야 쓸 수 있다**
- [x] **게코 외 종의 스트릭 호출 → `FEATURE_NOT_SUPPORTED_SPECIES` 신설 (2026-08-28 확정 · 2026-09-01 Notion 역반영 완료 — 「소스 구조」 §10 ErrorCode 예시에 `400 FEATURE_NOT_SUPPORTED_SPECIES` 추가).** 게코 전용 기능의 **공통** 코드다. (원문:) **거식 스트릭을 게코 외 종이 부르면** — 원본 "🦎 게코 전용"인데 **ErrorCode 가 없다**. 새 코드를 만들지, `SHED_…` 를 일반화할지
      ⚠️ **"제거" 대상이 애초에 Notion에 없었다.** `SHED_NOT_SUPPORTED_SPECIES` 는 `docs/specs/api-list.md`·이 계획서에만 있던 **레포 자체 명명**이고, API I/F 의 탈피 기록/목록/예측 행 어디에도 이 문자열이 적힌 적이 없다(2026-09-01 `notion-search` 로 확인 — 원문은 전부 "🦎 게코 전용"만 적혀 있고 ErrorCode 이름이 없었다). **"제거"가 아니라 처음부터 "추가"뿐이었다** — 역반영 대상이라고 적은 항목의 전제 자체가 계획서 안에서만 존재했던 경우로, 앞서 REQ-16 에서 반복된 "열거가 실제 원본과 어긋난다" 패턴과 같은 결이다. `api-list.md:144` 도 `FEATURE_NOT_SUPPORTED_SPECIES` 로 맞췄다
- [x] **`food_size` 는 종과 무관하게 그대로 저장 (2026-08-28 확정 · 2026-09-01 Notion 역반영 완료 — 「급여 기록」 Validation 문구 정정).** D13(`distance_km`)과 같은 결 — "개/고양이 미사용"은 입력 UI 규약이지 서버 거부 규약이 아니다. 거부하는 것은 enum 밖의 값뿐(400). (원문:) **`food_size` 를 개/고양이가 보내면** — "개/고양이 미사용". 무시 vs 400 (activity `distance_km` 와 같은 질문)

**Phase 4 (shed) 전**
- [ ] **"완료일은 시작일 이후여야 함"** — `shed_records` 에는 `shed_date` 하나뿐이다. 낡은 문구로 보이며 **원본 행에서 지우는 역반영**이 필요하다. 지우지 않으면 검증 계약이 존재하지 않는 컬럼을 가리킨다
- [ ] **탈피 예측의 기록 부족 시 동작.** `confidence LOW(1개)` 일 때 `average_cycle_days` 는 무엇으로 계산하는가(간격이 없다) — 기본 주기 상수? "3회 이상 시 실제 간격 평균으로 **자동 보정**"의 "보정 전" 값이 원본에 없다. 0건일 때 응답(빈 객체? 404?). `is_complete = false` 기록을 주기 계산에 넣는가
- [ ] **`is_assisted ↔ condition_tag '탈피도와줌'` 연계** (테이블 정의서) — D1 로 다이어리에 그 태그가 없어졌다. 연계 규칙 자체를 폐기하고 테이블 정의서에서 지울지, timeline(REQ-12) 이벤트로만 표기할지

**Phase 5 (diary) 전**
- [ ] **"미래 날짜 불가"의 기준 타임존** — `entry_date` 가 `date` 라 "오늘"이 KST 냐 UTC 냐에 따라 자정 전후 9시간이 갈린다
- [ ] **`limit` 최대 50 vs `CursorRequest.MAX_LIMIT = 100`** — diary 만 50 으로 따로 보정할지, framework 상한을 50 으로 내릴지(다른 목록 원본은 상한 미명시)
- [ ] **거꾸리 경고** — 「소스 구조」 §8 에만 있고 `API I/F` 응답에 없다. 어느 엔드포인트에 어떤 형태로 실을지 원본에 먼저 적히기 전엔 만들지 않는다

**전체**
- [x] **응답 시각의 `Z` 표기 → REQ-16 으로 이관 (2026-08-28).** framework 전역 결정으로 번져 별도 REQ 가 됐다 — 저장을 `timestamptz` 로 바꾸고 응답은 `+09:00`, 달력 판정은 KST 고정([ADR-0002](../adr/ADR-0002-time-handling-timestamptz.md) · [PLAN-REQ-16](PLAN-REQ-16-time-handling-timestamptz.md)). **REQ-10 Phase 3 이후는 REQ-16 완료 뒤에 진행한다** — "당일"·스트릭 일수가 달력 기준을 요구하기 때문이다. (원문:) **응답 시각의 `Z` 표기** (2026-08-28 등록) — D12 는 "요청·응답은 ISO-8601 `Z`"인데 Jackson 에 시각 포맷 설정이 없어 응답 `logged_at`·`created_at` 이 `2026-06-30T09:00:00`(Z 없음)으로 나간다. 기존 엔티티 전부 같은 상태라 framework 전역 결정이다 — `JacksonConfig` 에 `LocalDateTime` 직렬화 포맷을 두거나 D12 문구를 현실에 맞추거나. Phase 3 전에 정한다
- [ ] **역반영 목록** (Phase 진행하며 사람이 Notion 에서) — ⓐ 테이블 정의서·「소스 구조」§8·ERD 의 `condition_tag` 7종 → 4종 ⓑ 테이블 정의서 `feeding_logs` 에 `food_size` ~~ⓒ 활동·체중·탈피 목록 행에 `cursor`/`limit` 절~~ (2026-08-28 완료) ⓓ 탈피 기록 "완료일은 시작일 이후" 삭제 ~~ⓔ 체중 경고 필드 명시~~ (2026-08-28 완료) ~~ⓕ 「소스 구조」 §13 ArchUnit 스케치에 예외 3건~~ (2026-08-28 완료 — callout + 스케치 ① 갱신, `fetch` 로 저장 확인) ~~ⓖ 급여 기록 callout "당일 시간만 허용" → "미래 시각 불가" ⓗ 거식 스트릭 응답 예시의 자기모순 ⓘ 「소스 구조」 §10 에 `FEATURE_NOT_SUPPORTED_SPECIES` 추가 ⓙ 급여 기록 행에 `food_size` 처리 절~~ (2026-09-01 완료 — `fetch` 재조회로 4곳 전부 확인. ⓘ의 "`SHED_NOT_SUPPORTED_SPECIES` 제거"는 대상이 애초에 Notion에 없어 추가만 했다, 위 미결 참고) — Phase 3 착수 전 원본 정리 끝. (시각 예시 `…Z` → `+09:00` 은 REQ-16 Phase 4 소관, 완료됨)

## 작업 단계

> Phase 1 개 = 커밋 1 개 (`/implement` 규칙). 각 Phase 의 검증 계약은 `/testgen` 이 Phase 착수 직전에 채운다(REQ-09 실측 — 표를 먼저 채워 두면 `/implement` 게이트에 걸린다).

- [x] **Phase 0 — ArchUnit 예외 3건 + 프로브** — 완료 2026-08-28 (`016c692` · PR #36 · 「소스 구조」 §13 역반영 같은 날)
      `DomainBoundaryTest` 에 `ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.business.pet.service.."))` · `data.pet.dto..` · `data.pet.enums..` 추가. REQ-09 프로브에서 검증된 형태.
      완료 기준: 가짜 `business/weight/service` 가 가드를 주입해 통과 · `PetRepository` 직접 주입은 **여전히 FAIL** · `Pet` 엔티티 직접 참조는 **여전히 FAIL** (셋 다 프로브 후 삭제) · ArchUnit 8건 통과 · 「소스 구조」 §13 역반영

- [ ] **Phase 1 — weight (4행)** — 코드·케이스 20건 완료 (2026-08-28, `acde9ab` · `feat/req10-phase1-weight`). **체크 보류: 로컬 DB 확인 1건 남음** — ~~① `bootRun` 으로 `@Query` 기동 검증~~ **2026-08-28 닫힘** (Docker Postgres 17 구성 후 기동 성공. `w.measuredAt` → `w.measuredAtXX` 로 일부러 깨자 `UnknownPathException` 으로 기동이 막히는 것까지 확인해 "초록 기동 = 실제 파싱 통과"를 성립시켰다) · ② 같은 `measured_at` 3건 · `limit=2` 로 페이지 경계 누락·중복 실측 — **인증 토큰·펫 생성이 선행이라 아직 남음**
      `WeightLog` 엔티티(`BaseCreatedEntity`) · `WeightLogRepository`(`findByIdAndPetId` · keyset 목록) · `WeightService`(가드 소비, D6, 파생 필드 D3) · `WeightController`. **여기서 확정되는 것**: 가드 소비 코드 모양 · 커서 페이로드 형태 · D6 404 · 목록 응답 = `CursorPage`.
      완료 기준: 4행이 원본 상태코드(201/200/200/204)대로 · 남의 펫 403 · 삭제된 펫 404 · 남의 기록 id 404(D6) · 목록 `has_next`/`next_cursor` 가 keyset 으로 동작(같은 `measured_at` 여러 건에서 누락·중복 없음) · `weight_g` 0 이하 400 · 체중 경고 필드가 미결 답대로 · ArchUnit 통과

- [ ] **Phase 2 — activity (4행)** — 코드·케이스 19건 완료 (2026-08-28, `47cf630`). **체크 보류: Phase 1 과 같은 로컬 DB 확인 1건**(keyset 경계) — `@Query` 기동은 2026-08-28 닫혔다(Phase 1 참조) · keyset 경계는 Phase 1 확인 시 같이 본다
      Phase 1 형태 복제 + `ActivityType` enum + 종별 검증(`INVALID_SPECIES_ACTIVITY`).
      완료 기준: 게코가 `WALK` → 400 `INVALID_SPECIES_ACTIVITY` · 개가 `HANDLING` → 400 · 개가 `WALK` → 201 · 게코가 `HANDLING` → 201 · PATCH 로 `activity_type` 을 바꿔도 종 검증이 다시 걸린다 · 나머지는 Phase 1 과 동일 기준

- [x] **Phase 3 — feeding (5행) + V4** — 코드·케이스 25건 완료, PR #45 머지(2026-09-01, `e54b4d9`). **로컬 DB 확인도 닫혔다** — `docker start petkok-pg` + `bootRun` 으로 돌려보니 `ddl-auto: validate` 가 실제로 걸렸다: `food_size` 컬럼이 `@Enumerated(STRING)` + `@Column(length = 1)` 조합 때문에 Hibernate 가 `CHAR(1)` 로 추론해 `V4` 의 `varchar(1)` 과 충돌 — `length = 1` 을 빼서 고치고 PR #46(`ee57090`)으로 머지, 재기동으로 `Started PetKokApplication` 확인. ⚠️ **선행이었던 REQ-16 은 완료됨**(2026-09-01) · `V3` 는 REQ-16 이 가져갔다
      `V4__feeding_food_size.sql` · `FeedingLog` · `FoodSize` enum(S/M/L) · CRUD 4행 · `AnorexiaStreakCalculator`(순수) + `GET /feeding/anorexia-streak`.
      완료 기준: V4 가 로컬 DB 에 적용되고 `ddl-auto: validate` 통과 ✅(PR #46 으로 실제 결함 잡고 확인) · `is_refused` 누락 400 ✅ · 계산기 단위 테스트가 미결 답(스트릭 정의·경계·0건)을 케이스로 고정 ✅ · `level` 경계값(2일/3일/6일/7일) ✅ · 게코 외 종의 스트릭 호출이 미결 답대로 ✅ · 나머지는 Phase 1 기준 ✅

- [ ] **Phase 4 — shed (5행)**
      `ShedRecord` · CRUD 4행(게코 외 종은 **네 행 전부** `SHED_NOT_SUPPORTED_SPECIES`) · `ShedPredictionCalculator`(순수) + `GET /shed/prediction`.
      완료 기준: 개 펫의 `POST /shed` → 400 `SHED_NOT_SUPPORTED_SPECIES` · `GET /shed` 도 400(목록도 게코 전용) · 예측 `confidence` 가 기록 1/2/3+ 건에서 LOW/MEDIUM/HIGH · `average_cycle_days` 가 최근 3개 간격 평균 · 기록 부족 시 동작이 미결 답대로 · 나머지는 Phase 1 기준

- [ ] **Phase 5 — diary (5행, 텍스트만)**
      `DiaryEntry`(`BaseTimeEntity`) · `ConditionTag` enum 4종(D1) · CRUD 5행 · 목록 `condition_tag` 필터.
      완료 기준: `condition_tag: "거식"` → 400 · `entry_date` 미래 → 400(기준 타임존은 미결 답) · 응답에 `updated_at` 있음(D11) · 응답에 `photos`·`photo_count` **없음**(D4) · `photo_ids` 를 보내도 무시되고 201 · 필터가 걸린 목록도 keyset 유지 · 나머지는 Phase 1 기준

## 검증 계약

> 작성: 2026-08-27 · 근거: 이 계획서 (원본은 Notion `API I/F` · 「소스 구조」) · 검증: `/testrun REQ-10`
> **결과 갱신: 2026-08-28 — 01~03 `✅ 수동` (Phase 0 프로브).** 가짜 `business/weight/service/WeightService` 를 3형태로 심어 `DomainBoundaryTest` 실행(XML 로 8건 실행 확인, 프로브 후 삭제). 01 은 예외 없는 원본 규칙에서 FAIL 인 것까지 대조해 공허하지 않음을 확인. 커밋 `016c692` 본문에 결과를 남겼다. `/testrun` 에는 잡히지 않는다.
> **결과 갱신: 2026-08-28 — 04~23 전부 `✅` (Phase 1).** `/testrun REQ-10` 32 메서드 실행 · 실패 0 · 표 20행 ↔ 코드 20 ID 일치 · 근거 인용 전건 원문 존재. **Phase 1 완료 기준 중 "keyset 누락·중복 없음"의 실제 DB 경계 동작과 `@Query` JPQL 기동 검증은 이 세션 머신에 DB 가 없어 미확인** — Phase 1 체크는 그 확인 후 켠다.
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-10-01]` 형태로 박는다.
> **Phase 0 의 01~03 은 프로브다** — 가짜 클래스를 심었다 지우는 확인이라 영구 테스트로 남지 않는다. `/implement REQ-10 0` 이 실행하고 결과를 커밋 본문에 남기며, `결과` 열은 `REQ-08-11` 처럼 `✅ 수동` 으로 채운다.
> **테스트 코드는 Phase 별로 들어온다** (Java 는 대상 클래스가 없으면 테스트 소스가 컴파일되지 않는다 — REQ-08·09 실측). Phase 1 코드는 `/implement REQ-10 1` 직전 `/testgen` 재호출로 쓴다. Phase 2~5 행은 각 Phase 착수 전 미결을 닫은 뒤 추가한다.
> **`message` 를 단언하지 않는다** — `status` 와 `error.code` 만 본다(AGENTS §6). 인용문에 `|` 가 들어가면 표 셀이 갈라지므로 **`|` 앞에서 끊는다**.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-10-01 | `DomainBoundaryTest` | 하위 Service 가 `PetAccessGuard` · `OwnedPetResponse` · `Species` 만 주입 → 통과 | 프로브 | Phase 0 완료 기준 — "가짜 `business/weight/service` 가 가드를 주입해 통과" | 0 | ✅ 수동 |
| REQ-10-02 | 〃 | 하위 Service 가 `PetRepository` 직접 주입 → 규칙 FAIL | 프로브 | Phase 0 완료 기준 — "`PetRepository` 직접 주입은 **여전히 FAIL**" | 0 | ✅ 수동 |
| REQ-10-03 | 〃 | 하위 Service 가 `Pet` 엔티티 참조 → 규칙 FAIL | 프로브 | Phase 0 완료 기준 — "`Pet` 엔티티 직접 참조는 **여전히 FAIL**" | 0 | ✅ 수동 |
| REQ-10-04 | `POST /weight` | **HTTP 왕복** 201 | 정상 | Phase 1 완료 기준 — "4행이 원본 상태코드(201/200/200/204)대로" | 1 | ✅ |
| REQ-10-05 | `DELETE /weight/{log_id}` | **HTTP 왕복** 204 · 본문 없음 | 정상 | 〃 | 1 | ✅ |
| REQ-10-06 | `WeightService` 진입 | 남의 펫 → `PET_FORBIDDEN` (가드 위임) | 예외 | Phase 1 완료 기준 — "남의 펫 403" | 1 | ✅ |
| REQ-10-07 | 〃 | 삭제된 펫 → `PET_NOT_FOUND` | 예외 | Phase 1 완료 기준 — "삭제된 펫 404" | 1 | ✅ |
| REQ-10-08 | 기록 조회 | 다른 펫에 속한 기록 id → `RESOURCE_NOT_FOUND` | 예외 | D6 — "`findByIdAndPetId(id, petId)`" · 제약·함정 — "기록 조회에 `pet_id` 를 함께 걸지 않으면 남의 기록이 보인다" | 1 | ✅ |
| REQ-10-09 | `WeightService` | `PetRepository` · `Pet` 을 참조하지 않는다 | 불변식 | D5 — "`PetRepository`·`Pet` 은 참조하지 않는다" | 1 | ✅ |
| REQ-10-10 | 목록 | 같은 `measured_at` 여러 건이 페이지 경계에 걸려도 누락·중복 없음 | 회귀 | D8 — "`id` desc 타이브레이크" · 제약·함정 — "같은 날짜 여러 건에서 누락·중복" | 1 | ✅ |
| REQ-10-11 | 목록 응답 | `items` · `next_cursor` · `has_next` 키 (snake_case) | 불변식 | 범위—포함 — "`{items, next_cursor, has_next}` 다(= `CursorPage`)" | 1 | ✅ |
| REQ-10-12 | `POST /weight` | `weight_g` 0 → 400 | 경계 | 원본 Validation — "`weight_g` 필수 (양의 정수)" | 1 | ✅ |
| REQ-10-13 | 〃 | `weight_g` 누락 → 400 | 경계 | 〃 | 1 | ✅ |
| REQ-10-14 | 〃 | `measured_at` 누락 → 400 | 경계 | 원본 Validation — "`measured_at` 필수" | 1 | ✅ |
| REQ-10-15 | PATCH 요청 DTO | `@NotNull` · `@NotBlank` 가 없다 | 회귀 | 제약·함정 — "PATCH DTO 에 `@NotNull`·`@NotBlank` 금지" | 1 | ✅ |
| REQ-10-16 | `PATCH /weight/{log_id}` | `memo` 만 보내면 `weight_g` 가 유지된다 | 회귀 | D10 — "병합은 서비스" | 1 | ✅ |
| REQ-10-17 | 삭제 | 삭제 후 같은 id 조회 → `RESOURCE_NOT_FOUND` (하드 삭제) | 정상 | D7 — "`delete` 는 행 삭제, 204" | 1 | ✅ |
| REQ-10-18 | 체중 경고 | 첫 기록(직전 없음) → `weight_change_rate` `null` · `is_weight_warning` `false` | 경계 | D3 — "첫 기록 `null`" · "첫 기록 `false`" | 1 | ✅ |
| REQ-10-19 | 〃 | 직전 50g → 60g (정확히 +20%) → `20.0` · `true` | 경계 | D3 — "`\|변화율\| >= 20` → `true`" | 1 | ✅ |
| REQ-10-20 | 〃 | 직전 50g → 59g (+18%) → `18.0` · `false` | 경계 | 〃 | 1 | ✅ |
| REQ-10-21 | 〃 | 직전 = `measured_at` desc, `id` desc 정렬의 바로 다음 1건 (그 다음 건이 아니다) | 정상 | D3 — "직전 = `measured_at` desc, `id` desc 정렬에서 바로 다음 1건" | 1 | ✅ |
| REQ-10-22 | `GET /weight` | `limit` 미지정 → 서비스가 받는 `CursorRequest.limit()` 이 20 | 경계 | 미결 질문(Phase 1) — "`Query Parameters: cursor / limit (기본 20)`" | 1 | ✅ |
| REQ-10-23 | `GET /weight` | 해석 불가한 `cursor` → `INVALID_CURSOR` | 예외 | D8 — "`CursorCodec` 으로 opaque" · `CursorCodec.decode` 가 `INVALID_CURSOR` 를 던진다 | 1 | ✅ |

| REQ-10-24 | `ActivityService` | 게코가 `WALK` → `INVALID_SPECIES_ACTIVITY` | 예외 | Phase 2 완료 기준 — "게코가 `WALK` → 400 `INVALID_SPECIES_ACTIVITY`" | 2 | ✅ |
| REQ-10-25 | 〃 | 개가 `HANDLING` → `INVALID_SPECIES_ACTIVITY` | 예외 | Phase 2 완료 기준 — "개가 `HANDLING` → 400" | 2 | ✅ |
| REQ-10-26 | 〃 | 고양이가 `HANDLING` → `INVALID_SPECIES_ACTIVITY` | 예외 | 원본 Validation — "🦎 게코=`HANDLING`만" (개/고양이는 나머지 넷) | 2 | ✅ |
| REQ-10-27 | 〃 | 개가 `WALK` → 저장 | 정상 | Phase 2 완료 기준 — "개가 `WALK` → 201" | 2 | ✅ |
| REQ-10-28 | 〃 | 게코가 `HANDLING` → 저장 | 정상 | Phase 2 완료 기준 — "게코가 `HANDLING` → 201" | 2 | ✅ |
| REQ-10-29 | 〃 | PATCH 로 게코 기록의 `activity_type` 을 `WALK` 로 → `INVALID_SPECIES_ACTIVITY` | 회귀 | Phase 2 완료 기준 — "PATCH 로 `activity_type` 을 바꿔도 종 검증이 다시 걸린다" | 2 | ✅ |
| REQ-10-30 | 〃 | PATCH 에 `activity_type` 이 없으면 종 검증 없이 통과 | 정상 | D10 — "누락·`null` = "변경 없음"" | 2 | ✅ |
| REQ-10-31 | 〃 | 게코가 `distance_km` 를 보내면 그대로 저장되고 400 이 아니다 | 정상 | D13 — "보낸 값을 그대로 저장" | 2 | ✅ |
| REQ-10-32 | `POST /activity` | **HTTP 왕복** 201 | 정상 | Phase 2 완료 기준 — "나머지는 Phase 1 과 동일 기준" · Phase 1 완료 기준 — "4행이 원본 상태코드(201/200/200/204)대로" | 2 | ✅ |
| REQ-10-33 | `DELETE /activity/{log_id}` | **HTTP 왕복** 204 · 본문 없음 | 정상 | 〃 | 2 | ✅ |
| REQ-10-34 | `POST /activity` | 종 위반은 **HTTP 왕복** 400 · `error.code` = `INVALID_SPECIES_ACTIVITY` | 예외 | Phase 2 완료 기준 — "게코가 `WALK` → 400 `INVALID_SPECIES_ACTIVITY`" | 2 | ✅ |
| REQ-10-35 | `POST /activity` | 정의되지 않은 `activity_type` → 400 | 경계 | 제약·함정 — "정의되지 않은 enum 값은 400" | 2 | ✅ |
| REQ-10-36 | `POST /activity` | `activity_type` 누락 → 400 | 경계 | 원본 Validation — "`activity_type` 필수" | 2 | ✅ |
| REQ-10-37 | `POST /activity` | `logged_at` 누락 → 400 | 경계 | 원본 Validation — "`logged_at` 필수" | 2 | ✅ |
| REQ-10-38 | PATCH 요청 DTO | `@NotNull` · `@NotBlank` 가 없다 | 회귀 | 제약·함정 — "PATCH DTO 에 `@NotNull`·`@NotBlank` 금지" | 2 | ✅ |
| REQ-10-39 | `ActivityService` | `memo` 만 보내면 `duration_minutes` 가 유지된다 | 회귀 | D10 — "병합은 서비스" | 2 | ✅ |
| REQ-10-40 | 〃 | 남의 펫 → `PET_FORBIDDEN` (가드 위임) | 예외 | Phase 1 완료 기준 — "남의 펫 403" (Phase 2 "나머지는 Phase 1 과 동일 기준") | 2 | ✅ |
| REQ-10-41 | 〃 | 다른 펫에 속한 기록 id → `RESOURCE_NOT_FOUND` | 예외 | D6 — "`findByIdAndPetId(id, petId)`" | 2 | ✅ |
| REQ-10-42 | 목록 | `next_cursor` 에 마지막 항목 `id` · 다음 페이지 조회가 `logged_at`·`id` 둘 다 전달 | 회귀 | D8 — "`id` desc 타이브레이크" (REQ-10-10 과 같은 필요조건 검증) | 2 | ✅ |

> **결과 갱신: 2026-08-28 — 24~42 전부 `✅` (Phase 2).** `/testrun` 22 메서드 실행 · 실패 0 · 표 19행 ↔ 코드 19 ID 일치 · 인용 전건 원문 존재. Phase 1 과 같은 이유(로컬 DB 기동·keyset 경계 미확인)로 Phase 2 체크는 보류.
> **2026-08-28 추가 — 24~42 (Phase 2, activity).** D13 확정 후 추가. Phase 1 과 같은 형태의 케이스(가드 위임·D6·커서·PATCH 병합)는 도메인마다 한 번씩 다시 고정한다 — 복제 과정에서 빠뜨리는 것이 이 REQ 의 주 실패 모드다.
> **2026-08-28 추가 — 18~23.** Phase 1 미결 2건(체중 경고 형태 · `cursor`/`limit`)이 닫혀 행을 추가했다. **REQ-10-10 은 DB 없이 검증한다** — 이 레포에 DB 테스트 하네스(H2·Testcontainers)가 없어 "누락·중복 없음"을 실제 페이지 경계에서 재지 못한다. 대신 그 성질의 **필요조건 두 가지**를 고정한다: ⓐ `next_cursor` 페이로드에 마지막 항목의 `id` 가 실린다 ⓑ 다음 페이지 조회가 `measured_at` 과 `id` 를 **둘 다** 저장소에 넘긴다. 실제 경계 동작은 Phase 1 완료 시 로컬 DB 로 한 번 수동 확인한다.

| REQ-10-43 | `POST /feeding` | **HTTP 왕복** 201 | 정상 | Phase 3 완료 기준 — "나머지는 Phase 1 기준" · Phase 1 완료 기준 — "4행이 원본 상태코드(201/200/200/204)대로" | 3 | ✅ |
| REQ-10-44 | `DELETE /feeding/{log_id}` | **HTTP 왕복** 204 · 본문 없음 | 정상 | 〃 | 3 | ✅ |
| REQ-10-45 | `FeedingService` 진입 | 남의 펫 → `PET_FORBIDDEN` | 예외 | Phase 1 완료 기준 — "남의 펫 403" (Phase 3 "나머지는 Phase 1 기준") | 3 | ✅ |
| REQ-10-46 | 〃 | 삭제된 펫 → `PET_NOT_FOUND` | 예외 | Phase 1 완료 기준 — "삭제된 펫 404" | 3 | ✅ |
| REQ-10-47 | 기록 조회 | 다른 펫에 속한 기록 id → `RESOURCE_NOT_FOUND` | 예외 | D6 — "`findByIdAndPetId(id, petId)`" | 3 | ✅ |
| REQ-10-48 | 목록 응답 | `items`·`next_cursor`·`has_next` 키 | 불변식 | 범위—포함 — "`{items, next_cursor, has_next}` 다(= `CursorPage`)" | 3 | ✅ |
| REQ-10-49 | PATCH 요청 DTO | `@NotNull`·`@NotBlank` 가 없다 | 회귀 | 제약·함정 — "PATCH DTO 에 `@NotNull`·`@NotBlank` 금지" | 3 | ✅ |
| REQ-10-50 | `PATCH /feeding/{log_id}` | `memo` 만 보내면 `amount` 가 유지된다 | 회귀 | D10 — "병합은 서비스" | 3 | ✅ |
| REQ-10-51 | 목록 | `next_cursor` 에 마지막 항목 `id` · 다음 페이지 조회가 `fed_at`·`id` 둘 다 전달 | 회귀 | D8 — "`id` desc 타이브레이크" | 3 | ✅ |
| REQ-10-52 | `POST /feeding` | `is_refused` 누락 → 400 | 경계 | 원본 Validation — "`is_refused` 필수" | 3 | ✅ |
| REQ-10-53 | 〃 | `fed_at` 누락 → 400 | 경계 | 원본 Validation — "`fed_at` 필수" | 3 | ✅ |
| REQ-10-54 | 〃 | `fed_at` 이 서버 현재 시각보다 미래 → 400 | 예외 | 미결 질문(Phase 3) — "`fed_at` — 미래 시각만 거부" | 3 | ✅ |
| REQ-10-55 | 〃 | `fed_at` 이 어제(과거) → 201 정상 저장 | 정상 | 미결 질문(Phase 3) — "서버 현재 시각 이전이면 과거 소급을 허용한다" | 3 | ✅ |
| REQ-10-56 | 〃 | `food_size` 없이 요청 → 정상 저장(`null`) | 경계 | 원본 Validation — "`food_size` 선택" | 3 | ✅ |
| REQ-10-57 | 〃 | 개 펫이 `food_size` 를 보내도 그대로 저장, 400 아님 | 정상 | 미결 질문(Phase 3) — "`food_size` 는 종과 무관하게 그대로 저장" | 3 | ✅ |
| REQ-10-58 | 〃 | `food_size` 가 enum 밖 값(`"XL"`) → 400 | 경계 | 제약·함정 — "정의되지 않은 enum 값은 400" | 3 | ✅ |
| REQ-10-59 | `AnorexiaStreakCalculator` | 기록 0건 → `{current_streak_days:0, level:NONE, last_eaten_at:null}` | 경계 | 미결 질문(Phase 3) — "기록 0건이면 `{0, NONE, null}`" | 3 | ✅ |
| REQ-10-60 | 〃 | 마지막 정상급여 후 2일 경과 → `NONE` | 경계 | Phase 3 완료 기준 — "level 경계값(2일/3일/6일/7일)" | 3 | ✅ |
| REQ-10-61 | 〃 | 마지막 정상급여 후 3일 경과 → `CAUTION` | 경계 | 미결 질문(Phase 3) — "`>= 3` CAUTION" | 3 | ✅ |
| REQ-10-62 | 〃 | 마지막 정상급여 후 6일 경과 → `CAUTION` | 경계 | Phase 3 완료 기준 — "level 경계값(2일/3일/6일/7일)" | 3 | ✅ |
| REQ-10-63 | 〃 | 마지막 정상급여 후 7일 경과 → `DANGER` | 경계 | 미결 질문(Phase 3) — "`>= 7` DANGER" | 3 | ✅ |
| REQ-10-64 | 〃 | 마지막 급여 23:30(KST)·기준 시각 익일 00:30(KST) → `current_streak_days = 1` | 회귀 | 미결 질문(Phase 3) — "KST 달력 일수" | 3 | ✅ |
| REQ-10-65 | 〃 | 경과일 0일이면(건수 아님) `NONE` 유지 | 회귀 | 미결 질문(Phase 3) — "연속 거식 건수 안은 기각" | 3 | ✅ |
| REQ-10-66 | `GET /feeding/anorexia-streak` | 개 펫 호출 → **HTTP 왕복** 400 · `error.code` = `FEATURE_NOT_SUPPORTED_SPECIES` | 예외 | 미결 질문(Phase 3) — "게코 외 종의 스트릭 호출 → `FEATURE_NOT_SUPPORTED_SPECIES` 신설" | 3 | ✅ |
| REQ-10-67 | 〃 | 게코 펫 호출 → **HTTP 왕복** 200 | 정상 | 범위—포함 — "거식 스트릭은 게코 전용" | 3 | ✅ |

> **결과 갱신: 2026-09-01 — 43~67 전부 `✅` (Phase 3).** `/testrun REQ-10` 82 메서드 실행(weight·activity·feeding 전체) · 실패 0 · 표 25행 ↔ 코드 25 ID 일치 · 근거 인용 표본 검사 통과. `/testrun`이 자체 실행 1차에서 REQ-10-51 두 케이스를 (a) 테스트 결함으로 분류해 고쳤다 — `FeedingServiceTest`의 `NOW` 상수가 `+09:00`으로 선언돼 있었는데, 이 파일의 `CursorCodec`은 앱 `JacksonConfig`(KST 존)를 안 거친 무설정 `ObjectMapper`라 인코드 시 오프셋을 `Z`로 정규화한다(Jackson jsr310 기본값) — 순간은 같은데 레코드 동등성 비교만 깨졌다. `Weight`·`ActivityServiceTest`는 애초에 UTC 리터럴이라 드러나지 않았을 뿐이다. `NOW`를 `Z` 표기로 바꿔 해결(같은 순간, 표기만 변경). **계약 승격 제안 — 아래 참고**
> ✅ **Phase 3 체크 완료 (2026-09-01 오후).** "V4가 로컬 DB에 적용되고 `ddl-auto: validate` 통과"를 실제로 돌려 닫았다 — `docker start petkok-pg` + `bootRun`. **닫는 과정에서 실제 구현 결함을 잡았다**: `FeedingLog.foodSize`가 `@Enumerated(EnumType.STRING)` + `@Column(length = 1)` 조합이었는데, Hibernate 6가 이 조합을 `CHAR(1)`로 추론해 `V4__feeding_food_size.sql`의 `varchar(1)`과 충돌 — `Schema-validation: wrong column type ... found [varchar], but expecting [char(1)]`로 기동 자체가 막혔다. `length = 1`을 빼서 고치고(PR #46, `ee57090`) 재기동으로 `Started PetKokApplication` 확인, REQ-10 82케이스 재확인 통과.
> ⭐ **REQ-16 CLAUDE.md 계약("`ddl-auto: validate`는 컬럼 존재만 보고 타입은 안 본다")과 겹쳐 보이지만 다른 이야기다.** 그 계약은 `timestamp`/`timestamptz`처럼 **JDBC 타입 코드가 같게 취급되는 경우**의 사각지대였고, 이번 `VARCHAR`/`CHAR`는 **JDBC 타입 코드 자체가 다르다**(`Types#VARCHAR` vs `Types#CHAR`)— validate가 원래도 잡는 부류다. "타입을 안 본다"가 전체 규칙이 아니라 "코드가 같은 타입끼리는 못 가른다"로 정정해서 읽어야 한다.
> ✅ **계약 승격 완료 (2026-09-01, PR #47 · `40eec58`).** AGENTS.md §5에 반영. 반영 전 사용자가 "그럼 DDL이 틀린 거 아닌가 — CHAR가 더 올바른 거 아닌가?"를 물었다 — 타당한 질문이다. 답은 **"고립된 컬럼 하나만 보면 CHAR(1)이 교과서적으로 더 정확하지만, 이 프로젝트는 `species`·`gender`·`provider`·`activity_type`·`condition_tag` 전부를 varchar로 통일하는 "enum=varchar" 규칙을 이미 AGENTS §5에 못 박아 뒀고, `food_size` 하나만 CHAR로 새면 그 규칙이 깨진다"**다. 틀린 건 DDL이 아니라 엔티티의 `length = 1` 힌트라는 판단을 그대로 유지했다.
> `/testgen`이 승인된 표대로 테스트 코드까지 작성했다(`FeedingServiceTest`·`AnorexiaStreakCalculatorTest`·`FeedingControllerWebMvcTest`·`FeedingDtoContractTest`) — 대상 클래스가 아직 없어 **컴파일되지 않는 상태**로 커밋 전이었다(Phase 1·2 와 같은 순서, REQ-08·09 실측). `/implement`가 구현을 채워 커밋했다(`9e148df`, wip — 당시 REQ-10-51 2건 미해결이라 미푸시). 코드가 정한 계약 —
> - `FeedingService(PetAccessGuard, FeedingLogRepository, CursorCodec, Clock)`. `fed_at` 미래 거부는 전용 코드가 없어 `ErrorCode.INVALID_INPUT` 을 그대로 썼다(REQ-10-54)
> - `AnorexiaStreakCalculator.calculate(OffsetDateTime lastEatenAt, OffsetDateTime now)` — **거식 시도 List 가 아니라 "마지막 정상 급여 시각(nullable)"만 받는 순수 정적 메서드.** "일수냐 건수냐"(미결 질문) 질문이 시그니처 자체로 답해진다 — 건수는 파라미터에 없어 계산에 들어올 수가 없다. "기록은 있지만 전부 거식"인 경우도 `lastEatenAt = null` 로 합류해 "0건"과 같은 결과가 된다(별도 케이스 불필요)
> - `FeedingLog` 는 필드 8개(`petId`·`foodType`·`foodSize`·`amount`·`amountUnit`·`isRefused`·`fedAt`·`memo`) — `Pet` 과 같은 이유로 `@Builder`+`@AllArgsConstructor(PRIVATE)` 가 필요하다(AGENTS §5, Checkstyle `ParameterNumber` 최대 7). **`update()` 는 `petId` 를 뺀 7개라 일반 메서드로 된다**
> - `FeedingLogRepository.findFirstByPetIdAndIsRefusedFalseOrderByFedAtDesc(petId)` — 스트릭 계산의 "마지막 정상 급여" 조회
> ⚠️ **REQ-10-66/67 은 Controller 레벨(서비스 목)만 있고, `FeedingService.getAnorexiaStreak` 의 종 검증 자체를 서비스 레벨에서 잰 케이스가 이 표에 없다** — Activity 의 REQ-10-24~26(서비스 레벨)과 REQ-10-34(컨트롤러 레벨)가 둘 다 있던 것과 다르다. 승인된 표에 없어 임의로 추가하지 않았다 — 원하면 별도 케이스로 추가

## 제약·함정

- **가드 소비 형태는 D5 하나뿐이다.** `PetRepository` 주입·`Pet` 참조는 ArchUnit 이 잡는다(REQ-09 프로브 실측). 잡히면 예외를 늘리지 말고 코드를 고친다
- **기록 조회에 `pet_id` 를 함께 걸지 않으면 남의 기록이 보인다** (D6). 가드는 펫만 본다. `findById` 만 쓴 뒤 응답 200 이면 **보안 결함인데 테스트는 초록**이다 — 케이스로 반드시 고정
- **keyset 커서에 `id` 타이브레이크가 없으면 같은 날짜 여러 건에서 누락·중복**이 난다(D8). 날짜 컬럼이 `date` 인 weight·shed·diary 에서 특히 — 하루 여러 건이 정상 사용이다
- **`@Transactional` 안에서 예외를 던지면 쓰기가 사라진다** (AGENTS §5). 이번 REQ 에 "거절하면서 남겨야 하는 쓰기"는 없어 보이지만, 생기면 `noRollbackFor`
- **PATCH DTO 에 `@NotNull`·`@NotBlank` 금지** (AGENTS §5). 길이·범위는 `@Size`·`@Positive` 로만(`null` 통과)
- **정의되지 않은 enum 값은 400** — `GlobalExceptionHandler` 의 `HttpMessageNotReadableException` 핸들러가 REQ-09 에서 들어왔다. `ActivityType`·`FoodSize`·`ConditionTag` 전부 이 경로를 탄다. **한글 enum 값(`정상`)은 Jackson 이 `name()` 으로 매칭하므로 enum 상수명을 한글로 두거나 `@JsonValue` 가 필요하다** — 방식은 Phase 5 착수 시 정한다
- **`ddl-auto: validate` 가 이 REQ 의 마이그레이션(V4)을 실제로 검증한다** — 엔티티 컬럼과 마이그레이션이 어긋나면 기동 시점에 터진다(`V3` 는 REQ-16 이 가져갔다). ⚠️ **실측(Phase 3, 2026-09-01)** — `@Enumerated(STRING)` + `@Column(length = 1)` 조합을 Hibernate 6 가 `CHAR(1)` 로 추론해, 마이그레이션의 `varchar(1)` 과 충돌하며 기동이 막혔다(`FeedingLog.foodSize`, PR #46). **단일 문자 값의 문자열/enum 컬럼엔 `length = 1` 을 쓰지 않는다** — 길이를 굳이 제한하려면 `columnDefinition` 으로 명시한다. shed·diary 의 단일 문자 enum(있다면)에서 재발할 수 있다
- **적용된 마이그레이션은 주석 한 글자도 못 고친다** (CLAUDE.md) — `V1__init.sql` 의 `condition_tag` 주석이 D1 로 **우연히 맞게** 됐다. 고칠 일이 없다
- **SQL 로 심은 픽스처는 앱 기준 9시간 미래다** (D12, PROGRESS 07-30). 계산기 검증은 순수 단위 테스트로, DB 왕복 확인 시 `now() at time zone 'UTC'`
- **컨트롤러 테스트는 `@Import({SecurityConfig, JacksonConfig})`** (AGENTS §6). 슬라이스에 `UserService` 가 없으므로 `UserStatusChecker` 를 따로 `@MockBean`(`PetControllerWebMvcTest` 와 같음)
- **`--tests` 는 문자 클래스를 모르고, XML `testcase name` 은 `@DisplayName` 이다** (CLAUDE.md) — 클래스명으로 거르고 `[REQ-10-xx]` 로 센다
- **검증 계약 표 셀 안의 `|`** — `WALK | PLAY | …` 인용을 표에 넣으면 셀이 갈라져 자동 대조가 깨진다(REQ-09 실측). 인용을 `\|` 로 이스케이프하면 원문 grep 이 0건이 된다. **`/testgen` 은 인용을 `|` 앞에서 끊는다**
