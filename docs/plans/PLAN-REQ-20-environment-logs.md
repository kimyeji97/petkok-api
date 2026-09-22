# PLAN-REQ-20 · 게코 사육 환경(온습도) 기록

> 출처: 현재 세션 대화(Notion 요구사항 25건 전수 대조 중 `docs/TODO.md`에 등록된 갭) · 작성: 2026-09-22 · 상태: ✅ 완료 (미결 0건 — 3건 전부 대화로 확정, Phase 0·1 전부 완료 · 검증 계약 15건 전부 `/testrun` 통과)

## 배경

Notion 요구사항 DB의 `FR-FEED-05`(🔴 Must Have)와 `US-FEED-03`이 "게코로서, 오늘 온도와 습도를 기록하고 일간 평균을 확인하고 싶다"를 요구하는데, 백엔드에 관련 테이블·코드가 **전혀 없다**(2026-09-21 `docs/TODO.md` 작성 시 grep으로 확인 — `temperature`·`humidity` 매치 0건).

다른 record 도메인(급여·체중·활동·탈피)과 달리 이 기능은 **Notion `API I/F`에 엔드포인트 정의 자체가 없다** — `요구사항`·`유저 스토리` 두 곳 다 제목 한 줄뿐이고 API 계약·DB 설계가 원본에 없는 상태다. 이번이 그 설계를 처음 하는 자리다.

## 범위

**포함**

- 게코 전용 온습도 기록 — `POST`(기록) · `GET`(목록, 커서) · `PATCH`(수정) · `DELETE`(삭제). 다른 4개 record 도메인(급여·체중·활동·탈피)과 동일한 CRUD 형태(개별 기록 로그 방식)
- **일간 평균 조회** — `GET /pets/{pet_id}/environment/daily-summary?date=YYYY-MM-DD` 전용 엔드포인트. `AnorexiaStreakCalculator`·`ShedPredictionCalculator`와 같은 패턴(I/O 없는 순수 계산기 클래스, `EnvironmentSummaryCalculator` 가칭)으로 그날 기록을 평균 낸다
- Notion `API I/F`에 이 5개 엔드포인트 신규 행 추가 — **구현 착수 전에 먼저** 한다(대화 확정)
- 게코 외 종 → `FEATURE_NOT_SUPPORTED_SPECIES`(기존 코드 재사용, 거식 스트릭·탈피 예측과 동일 패턴)
- V6 마이그레이션 — 신규 테이블 1개

**제외**

- **적정 범위 이탈 경고 표시** — `FR-FEED-05`·`US-FEED-03` 원문 어디에도 "적정 범위를 벗어나면 경고"라는 문구가 없다. "적정 사육 환경이 유지되고 있는지 확인하기 위해"는 동기일 뿐 기능 요구가 아니다. 근거 없이 만들지 않는다 — 필요하면 별도로 Notion에 먼저 확인

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 저장 방식 | 개별 기록 로그(하루 여러 건 가능) | 대화 확정 — "오늘 기록하고 일간 평균을 확인"이 여러 번의 기록을 암시, `feeding_logs`와 같은 패턴이 기존 record 도메인과 일관적 | 하루 1건 요약 입력 — "평균"이라는 표현과 어긋남(평균낼 대상이 하나뿐이면 평균이 아니다) |
| 시각 컬럼 타입 | `timestamptz`(`measured_at`) | 하루 여러 번 기록해 시각까지 구분해야 하므로 REQ-17/`CLAUDE.md` 계약(`_at`=시각·`_date`=날짜)에 따라 `_at` | `date`(날짜만) — weight_logs처럼 하루 1건이면 충분했겠지만 이번엔 여러 건이라 시각이 필요 |
| 종 제약 | 게코 전용, `FEATURE_NOT_SUPPORTED_SPECIES` 재사용 | `ShedService.validateGecko`와 동일 코드·패턴(거식 스트릭·탈피 예측 선례) | — |
| 도메인 이름 | `environment`(`environment_logs`, `EnvironmentController` 등) | 온도+습도를 아우르는 이름이 필요하고 "사육 환경"을 직역 | `terrarium`(테라리움) — 크레스티드 게코 사육장 명칭으로 더 정확할 수 있으나 다른 종(향후 확장 시) 사육장을 가리키는 일반 용어는 아니라 보류 |
| 일간 평균 API 모양 | 별도 요약 엔드포인트(`GET .../environment/daily-summary?date=`) | 기존 파생값 엔드포인트(거식 스트릭·탈피 예측)와 같은 패턴 — 순수 계산기 클래스 + 전용 GET. 목록 응답에 얹으면 같은 날 여러 기록마다 값이 중복 노출되는 문제도 있음(대화 확정, 2026-09-22) | 목록 응답에 `daily_average` 필드 얹기 — 이 레포에 선례 없음, 중복 노출 문제 |
| 온도·습도 단위·정밀도 | 섭씨(°C)·%(백분율), 둘 다 소수점 1자리(`decimal(4,1)`) | 한국 앱이라 섭씨 외 대안이 사실상 없음. 소수점은 스마트 온습도계 연동을 고려(대화 확정, 2026-09-22) | 정수만 — 스마트 기기 값이 잘려나감 |
| Notion 역반영 시점 | 구현 착수 전에 먼저 — Phase 0 | AGENTS §0 원칙(API 계약 원본은 Notion) 유지. 이 세션에서 "구현 후 역반영"이 몇 번 지연 문제로 드러난 전례(REQ-11·REQ-17)를 반복하지 않음(대화 확정, 2026-09-22) | 구현·검증 끝난 뒤 역반영 — 위 지연 전례와 같은 패턴 반복 |

## 미결 질문

없음 — 2026-09-22 대화로 3건 전부 확정.

## 작업 단계

- [x] **Phase 0 — Notion API I/F 설계 선반영**
      완료 기준: Notion `API I/F` 데이터베이스에 신규 행 5개 추가 — `POST /pets/{pet_id}/environment`(기록) · `GET /pets/{pet_id}/environment`(목록) · `PATCH /pets/{pet_id}/environment/{log_id}`(수정) · `DELETE /pets/{pet_id}/environment/{log_id}`(삭제) · `GET /pets/{pet_id}/environment/daily-summary`(🦎 일간 평균). 각 행에 요청·응답 필드(`temperature`·`humidity`·`measured_at`·`memo`, 응답은 `avg_temperature`·`avg_humidity`·`record_count`) 명시. `fetch` 재조회로 반영 확인
      → 2026-09-22 완료. 행 5개 생성, `fetch` 재조회로 콜아웃·코드블록 정상 렌더 확인. 부수 작업 — `도메인` multi_select에 `Environment` 옵션이 없어 `notion-update-data-source`로 스키마에 먼저 추가(기존 9개 옵션엔 없었음)

- [x] **Phase 1 — 게코 사육 환경 기록 CRUD + 일간 평균**
      완료 기준: `V6__environment_logs.sql` 적용(신규 테이블) · CRUD 4개 엔드포인트가 원본 상태코드(201/200/200/204)대로 동작 · 게코 외 종 → `FEATURE_NOT_SUPPORTED_SPECIES`(400) · `daily-summary`가 지정 날짜의 `avg_temperature`·`avg_humidity`·`record_count`를 반환(기록 0건이면 `record_count: 0`이고 평균은 `null`) · 컨트롤러 테스트(AGENTS §6 관례) 통과
      → 2026-09-22 완료. `ShedService`/`FeedingLogRepository` 패턴 그대로 재사용(신규 로직 최소화). `/testrun REQ-20`에서 검증 계약 15건 전부 1차 통과 확인, 커밋 `cef14f7` · PR #57(스쿼시)로 `main` 병합 완료(`6b72dc1`)

## 검증 계약

> 작성: 2026-09-22 · 대상: Phase 1 착수 직전 · 검증: `/testrun REQ-20`
> Phase 0(Notion API I/F 선반영)은 코드가 아니라 문서 작업이라 케이스로 옮기지 않았다 — 완료 기준 자체("신규 행 5개 추가, `fetch` 재조회로 확인")가 검증 방법이다.
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-20-01]` 형태로 박는다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-20-01 | `V6__environment_logs.sql` | `environment_logs` 테이블을 온도·습도·측정시각 컬럼과 함께 생성한다 | 회귀(구조) | Phase 1 완료 기준 — "`V6__environment_logs.sql` 적용(신규 테이블)" | 1 | ✅ |
| REQ-20-02 | `EnvironmentService.create` | 개 펫이 기록을 시도하면 `FEATURE_NOT_SUPPORTED_SPECIES`다 | 예외 | Phase 1 완료 기준 — "게코 외 종 → `FEATURE_NOT_SUPPORTED_SPECIES`(400)" | 1 | ✅ |
| REQ-20-03 | `EnvironmentService.getDailySummary` | 개 펫이 일간 평균을 조회하면 `FEATURE_NOT_SUPPORTED_SPECIES`다 | 예외 | 〃 | 1 | ✅ |
| REQ-20-04 | `POST /environment` | 201을 반환한다 | 정상 | Phase 1 완료 기준 — "CRUD 4개 엔드포인트가 원본 상태코드(201/200/200/204)대로 동작" | 1 | ✅ |
| REQ-20-05 | `GET /environment` | 200을 반환한다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-06 | `PATCH /environment/{id}` | 200을 반환한다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-07 | `DELETE /environment/{id}` | 204를 반환한다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-08 | `DELETE /environment/{id}` | 본문이 비어 있다 | 정상 | 〃(204는 본문 없음 — 다른 도메인 동일 관례) | 1 | ✅ |
| REQ-20-09 | `EnvironmentSummaryCalculator` | 기록 0건이면 `avg_temperature`·`avg_humidity`가 `null`, `record_count`는 0이다 | 불변식 | Phase 1 완료 기준 — "기록 0건이면 `record_count: 0`이고 평균은 `null`" | 1 | ✅ |
| REQ-20-10 | 〃 | 기록 1건이면 `avg_temperature`가 그 기록의 값과 같다 | 정상 | Phase 1 완료 기준 — "지정 날짜의 `avg_temperature`... 반환" | 1 | ✅ |
| REQ-20-11 | 〃 | 기록 여러 건이면 `avg_temperature`가 산술 평균이다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-12 | 〃 | 기록 여러 건이면 `avg_humidity`가 산술 평균이다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-13 | 〃 | `record_count`가 입력 건수와 같다 | 불변식 | 〃 | 1 | ✅ |
| REQ-20-14 | `GET /environment/daily-summary` | 200을 반환한다 | 정상 | 〃 | 1 | ✅ |
| REQ-20-15 | 〃 | 응답에 `avg_temperature`·`avg_humidity`·`record_count` 키가 있다 | 정상 | 〃 | 1 | ✅ |

## 제약·함정

- **Phase 0(Notion 선반영)이 Phase 1의 전제다.** AGENTS §0 원칙대로 API 계약이 코드보다 먼저 Notion에 있어야 한다 — 이번엔 원본을 옮겨 적는 게 아니라 이 계획서가 원본을 새로 만드는 자리라는 점이 다른 record 도메인과 다르다
- `FR-FEED-05`·`US-FEED-03` 둘 다 본문이 없고 제목 한 줄뿐이다(2026-09-22 `fetch` 확인) — Phase 0에서 Notion에 API 계약을 쓸 때 이 계획서의 `## 결정` 표가 사실상의 1차 설계안이 된다
- 일간 평균 계산은 저장하지 않고 조회 시 계산한다(다른 파생 필드·계산기와 같은 원칙 — weight의 `weight_change_rate`, feeding의 거식 스트릭과 동일)
