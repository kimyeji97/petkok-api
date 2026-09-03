# 진행 현황 (PROGRESS)

> 시간순 작업 로그. git이 말하지 못하는 **왜 / 함정 / 기각 이유**를 남긴다.
> 파일명·라인수처럼 `git show`로 볼 수 있는 건 적지 않는다.
> 깨면 회귀하는 **계약**은 이 파일이 아니라 CLAUDE.md/AGENTS.md에 둔다.
>
> 최종 갱신: 2026-09-03 (**REQ-10 완전 마감 — Notion 역반영 4건 전부 완료.** 「테이블 정의서」의 값 목록 셀 1곳만 API로 원문을 못 읽어 사람이 직접 고쳤고, 다시 입력하면서 저장 형태 자체가 정상화됐다. Notion 작업 이력(All Tasks·History) 계약이 새로 승격됐고 소급분까지 이미 채워져 있다 — 이 원본과 동기화 확인함. 남은 건 Phase 1·2 로컬 DB keyset 경계 실측뿐)

## 요구사항 인덱스

| REQ | 기능명 | 스펙 | 완료일 | 상태 |
|-----|--------|------|--------|:----:|
| REQ-01 | 스켈레톤 — global 공통 계층 + 베이스 엔티티 + Flyway `V1__init.sql` | [README](../README.md) | 2026-07-06 | ✅ |
| REQ-02 | 빌드·품질 도구 (Spotless / Checkstyle `-PciStrict` / JaCoCo) | [AGENTS §6](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-03 | 워크플로우·AI 진입점 (lefthook · CI · PR 템플릿 · AGENTS/CLAUDE) | [AGENTS §4](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-04 | 공통 유틸리티 이식 (`global/util`, spring-java-utility) | — | 2026-07-23 | ✅ |
| REQ-05 | RestTemplate 설정 + 요청·응답 로깅 인터셉터 | — | 2026-07-23 | ✅ |
| REQ-06 | API 설계 초안 + 설계 결정 3건 확정 | [api-list.md](specs/api-list.md) | 2026-07-23 | ✅ |
| REQ-13 | ~~MySQL 전환~~ — 2026-07-27 기각 (PostgreSQL 유지) | [PLAN-REQ-07](plans/PLAN-REQ-07-auth-and-db-environment.md) | — | ❌ |
| REQ-14 | 패키지 구조 재설계 + 이행 (`business`/`data`/`framework` 3분할) | [PLAN-REQ-14](plans/PLAN-REQ-14-package-structure-migration.md) | 2026-07-28 | ✅ |
| REQ-07 | auth 도메인 + DB 환경 구성 (Kakao 로그인 · refresh 로테이션 · V2 `refresh_tokens`) | [PLAN-REQ-07](plans/PLAN-REQ-07-auth-and-db-environment.md) | 2026-08-07 | ✅ (미결 0건 — 2026-08-27 해소) |
| REQ-08 | user 도메인 (내 프로필 조회·수정 · 회원 탈퇴 · 프로필 이미지 제거 · 닉네임 규칙) | [PLAN-REQ-08](plans/PLAN-REQ-08-user-domain.md) | 2026-08-27 | ✅ (Phase 0~5 · 미결 2건은 관찰 후) |
| REQ-09 | pet 도메인 + `PetAccessGuard` (소유권 앵커) | [PLAN-REQ-09](plans/PLAN-REQ-09-pet-domain.md) | 2026-08-27 | ✅ (미결 1건 — D3 예외 3건은 REQ-10 Phase 0) |
| REQ-10 | 기록 도메인 5종 (weight/activity/feeding/shed/diary) + 계산기 2 | [PLAN-REQ-10](plans/PLAN-REQ-10-record-domains.md) | 2026-09-03 | ✅ (Phase 0~5 · 검증 계약 111건 전부 · Notion 역반영 4건 전부 완료 · Phase 1·2 로컬 DB keyset 경계 실측 1건씩만 보류) |
| REQ-11 | gallery (R2 presigned 업로드) | [api-list §9](specs/api-list.md) | — | ⏸ |
| REQ-12 | timeline (다중 테이블 union — QueryDSL 활성화 시점) | [api-list §10](specs/api-list.md) | — | ⏸ |
| REQ-15 | 컨트롤러 테스트 관례 도입 (`@WebMvcTest`) | [PLAN-REQ-15](plans/PLAN-REQ-15-controller-test-convention.md) | 2026-08-10 | ✅ |
| REQ-16 | 시각 처리 규약 — `timestamptz` 전환 (저장 = 순간 · 노출·계산 KST 고정) | [PLAN-REQ-16](plans/PLAN-REQ-16-time-handling-timestamptz.md) · [ADR-0002](adr/ADR-0002-time-handling-timestamptz.md) | 2026-09-01 | ✅ (Phase 0~4 전부 완료 · Notion 탭 2곳 사람 손 반영 확인 · 미결 ⑦⑧은 이 REQ 밖 판단으로 별건) |

범례: ✅ 완료 · 🟡 진행 · ⏸ 보류 · ❌ 기각

---

# 로그

<!-- 최신이 위. 날짜 헤딩은 `## YYYY-MM-DD` 형식을 반드시 지킬 것 (/progress 가 파싱) -->

## 2026-09-03

> **REQ-10 Notion 역반영 4건(ⓐⓑⓓⓚ) 전부 완료 — REQ-10 완전 마감.** 사람 개입이 필요했던 건 예상(사람이 직접 셀 재입력)과 실제(사람이 고치자 API 저장 형태 자체가 정상화됨)가 갈렸다.

### ⓑ·ⓓ·ⓚ — API로 바로 처리, 계획에 없던 중복 1건 발견

`feeding_logs`에 `food_size varchar(1)` 행 추가(ⓑ), 탈피 기록 "완료일은 시작일 이후" 콜아웃 삭제(ⓓ), `is_assisted↔condition_tag'탈피도와줌'` 연계 폐기(ⓚ) — 셋 다 `notion-update-page`의 `update_content`로 바로 반영됐다. ⓓ를 처리하며 계획서에 없던 **「비즈니스 규칙」 `BR-DIARY-05`** 행(같은 규칙의 별도 사본)을 찾아 같이 폐기 표시했다 — 승인된 항목 목록만 보고 멈췄으면 놓쳤을 중복이다.

### ⓐ(condition_tag 7종→4종) — "API로 못 읽는다"에서 "사람이 고치니 API로도 읽힌다"로

「테이블 정의서」의 `condition_tag` 값 목록 셀이 `fetch`·`search` 양쪽에서 첫 값(`정상`) + `\` 에서 끊긴 채 반환됐다 — 재조회로도 재현되는 결정적 증상이라 CLAUDE.md에 새 함정으로 승격했다(`9d3863b`). 같은 문서의 `species`·`gender`·`provider`·`activity_type` 셀도 같은 증상이었지만 값 자체가 이미 정확해 손대지 않았다.

**같은 문제가 실은 두 종류였다.** 「ERD 설계」·「소스 구조」 문서에 있던 **같은 7종 목록의 다른 사본**(코드블록·표·확정 문구, 총 6곳)은 전혀 문제없이 API로 읽고 고쳐졌다 — 값 목록이 일반 텍스트/SQL 코드블록이었기 때문. 막힌 건 「테이블 정의서」의 **테이블 셀** 형태 하나뿐이었다. 이 과정에서 「소스 구조」§8에 `is_assisted↔'탈피도와줌'` 연계 문구의 **세 번째 사본**을 또 발견해 같이 폐기했다(ⓚ가 테이블 정의서·ERD·소스구조 세 곳에 흩어져 있었던 것).

사용자가 Notion UI에서 그 셀을 직접 지우고 4종을 다시 입력하자, `fetch` 재조회에서 **`"개체 상태 태그. (정상 \| 활발 \| 거꾸리 \| 구토)"`로 온전하게 읽혔다** — 이전엔 끊기던 자리가 이번엔 안 끊겼다. **저장 형태 자체가 망가져 있었고(어떤 과거 편집 경로가 남긴 것으로 추정 — 정확한 원인은 미확인), 사람이 새로 입력하면서 정상 형태로 재작성된 것으로 보인다.** 값을 지우고 다시 쓰는 것 자체가 우회책이었던 셈 — 다음에 같은 증상을 만나면 "API로 못 고친다"에서 멈추지 말고 이 경로를 먼저 시도할 만하다.

**REQ-10 역반영 목록 전 항목(ⓐ~ⓚ) 완료.** 남은 건 Phase 1·2의 로컬 DB keyset 경계 실측(인증 토큰·펫 생성 선행 필요)뿐 — REQ-10을 ✅로 승격했다.

### Notion 작업 이력(All Tasks · All Tasks History) 계약 승격 — 다른 세션이 소급까지 마쳤다(`352d9f9`)

이 세션 밖(다른 Claude 세션, `session_01Nn9he7kZvuuZwLfSeWuwjn`)에서 `CLAUDE.md`에 「Notion 작업 이력」절을 새로 추가했다 — `docs/PROGRESS.md`를 원본으로 두고 Notion의 두 외부 DB(☑️ All Tasks · 👣 All Tasks History)를 파생 요약으로 규정한다. Task 생성은 `/workplan`(REQ 번호가 인덱스에 들어갈 때), 일별 History 작성과 상태 전이는 `/checkpoint` 한 손(upsert) — 이 커맨드가 Phase마다 여러 번 돌 수 있으므로 그날 History 행이 있으면 새로 만들지 않고 덧붙인다. **완료 정의는 인덱스 ✅와 동일하고 배포는 포함하지 않는다**(2026-08-27 일괄 등록 이후 아무도 안 쓰고 있던 것이 계약 필요성의 근거였다).

같은 커밋에서 **소급분까지 이미 채워져 있었다** — REQ-10·11·12·16 Task 행 생성, REQ-13 완료→폐기 정정, 08-28~09-03 History 5행. 이 `/checkpoint` 실행에서 재조회로 확인한 결과 — **REQ-10 Task는 이미 `완료`, 오늘(2026-09-03) History 행도 이미 있고 REQ-10 Task에 정확히 연결돼 있다**(diary CRUD·Notion 역반영 4건·이 계약 자체까지 한 행에 담겨 있음). 이번 실행에서 추가로 쓸 Notion 변경은 없다 — 다른 세션이 같은 날 이미 upsert 규칙대로 처리해 뒀다.

> **REQ-10 Phase 4(shed) 완료.** `/testgen`→`/implement`→`/testrun`이 전부 1회에 녹색으로 끝났다(수정 루프 0회) — Phase 3까지 매번 뭔가 걸렸던 것과 다르다. 그 전에 사용자가 준 실제 도메인 지식(게코 성장 단계별 탈피 주기)이 설계 방향 하나를 바로 뒤집었다.

### 탈피 예측 기본값을 정하려다 "고정 상수 자체가 틀렸다"는 걸 알았다

Phase 4 미결 질문("기록 1개일 때 `average_cycle_days`를 뭘로 하나")에 처음엔 "고정 기본값(예: 30일)"으로 사용자가 답했는데, 실제 숫자를 물으니 대신 **크레스티드 게코의 성장 단계별 실측 자료**가 나왔다 — 베이비(1~2주)부터 성체(한 달~몇 달)까지 탈피 주기가 10배 넘게 갈린다. 이 자료를 `docs/reference/gecko-growth-and-shed-cycle.md`로 남기고(`3915e9c`), 애초에 물었던 "고정 기본값"이 성립할 수 없다는 게 자료 자체로 드러나 **`null`로 되돌렸다** — 처음에 권장했던 안으로 복귀.

> 이 문서는 지금 당장 코드에 안 쓰인다. "용도는 나중에"라고 사용자가 명시했다 — 성장 단계 인지 예측을 하려면 `weight_logs`·`pets.birthday`에서 나이·몸무게를 끌어와야 해서 이번 Phase 범위 밖이다. **당장 안 쓸 참고 자료도 나오면 바로 문서화한다** — 나중에 이 대화를 다시 찾아야 하는 비용을 지금 치르는 게 싸다.

### REQ-10 Phase 4(shed) — 케이스 26건, 자체 실행부터 `/testrun`까지 전부 1회 통과

**`/testgen`** — 완료 기준의 에러 코드가 낡아 있었다 — `SHED_NOT_SUPPORTED_SPECIES`(2026-08-27 작성)를 그대로 썼으면 2026-08-28에 이미 뒤집힌 결정(→ `FEATURE_NOT_SUPPORTED_SPECIES`, 거식 스트릭과 공통 코드)을 무시하는 것이었다. `docs/specs/api-list.md § 8`이 이미 최신 값으로 갱신돼 있어 그쪽을 근거로 삼았다 — **완료 기준 문구보다 더 최신 결정이 이긴다**는 원칙을 실제로 적용한 사례.

**`/implement REQ-10 4`** — `feat/req10-phase4-shed` 브랜치. `ShedRecord`(`LocalDate` 필드라 `Clock` 불필요) · `ShedService`(다섯 엔드포인트 전부 진입 시 종 검증) · `ShedPredictionCalculator`(순수, 최근 3건 간격 평균). `ErrorCode.SHED_NOT_SUPPORTED_SPECIES` 제거까지 — 2026-08-28 결정("소비자 없으니 제거") 실행. 자체 실행 32/32 **1차 통과**, 수정 루프 0회. 커밋(`4b4ad4a`)·푸시 완료.

**`/testrun REQ-10`** — REQ-10 전체(weight·activity·feeding·shed) 114 메서드 실행·실패 0·(a) 수정 0건. 표 90행 ↔ 코드 90 ID 정확히 일치(Phase 0 프로브 3건 제외). 근거 인용 전건(계획서 자체 인용 5건 + `api-list.md` 신규 인용 3건) 원문에서 확인.

### 미결 하나가 완전히 안 닫혔다 — `is_complete = false` 기록의 주기 계산 포함 여부

Phase 4 착수 전 미결 3항목 중 2개(기본값·0건 응답)만 사용자에게 확인받았고, 세 번째("`is_complete = false`인 부분 탈피 기록도 주기 계산에 넣는가")는 물어보지 않은 채 구현이 **암묵적으로 "넣는다"**로 정했다 — `ShedPredictionCalculator`가 받는 최근 `shed_date` 목록이 `is_complete` 필터링을 안 한다. `/implement`의 "미결 질문을 구현으로 확정하지 않는다"를 이 부분에서 어겼다 — 계획서 미결 질문 절에 열어 둔 채로 남겼다(체크는 부분적으로만 켰다).

**남은 것 (REQ-10)** — Phase 5(diary) 하나. 착수 전 미결 3건(미래 날짜 불가 기준 타임존 · `limit` 50 vs 100 · 거꾸리 경고) 전부 미확인 — 이번 세션에서 손 안 댔다.

### 개발 플로우 여섯 커맨드 계약 — AGENTS.md → 레포 CLAUDE.md → 사용자 전역 CLAUDE.md로 재배치

세 커밋(`d07ef26`·`48d9c63`·`3609ba5`)에 걸쳐 자리를 두 번 옮겼다. 처음엔 "`/workplan → /testgen → /implement → /testrun → /checkpoint → /progress` 순서로 작업한다"는 문장 자체가 어디에도 없어(흔적만 §0·§4·`CLAUDE.md`에 흩어져 있었다) `AGENTS.md §4`에 통째로 승격했다(`d07ef26`). 그런데 슬래시 커맨드는 Claude Code 전용이라 "모든 AI 에이전트용 진입점"인 `AGENTS.md`에 있을 것이 아니었다 — 커맨드 여섯 개의 책임 표를 레포 `CLAUDE.md`로 옮기고 `AGENTS.md §4`에는 도구 중립 원칙 세 개(계획서→검증 계약→구현→판정→기록 순서 · Phase 1개=커밋 1개 · 판정하는 손은 구현을 고치지 않는다)만 남겼다(`48d9c63`). 다음 단계로 커맨드 정의가 실제로 사는 `claude-commands` 저장소에 전역 `CLAUDE.md`를 두고 각 프로필(`~/.claude`, `~/.claude-apple` 등)이 심볼릭 링크하도록 바꿔(별도 작업, DEV-13), 이 레포 `CLAUDE.md`는 전역을 가리키는 포인터 + 레포에서만 다른 것(Java 테스트 컴파일 제약·계획서 경로·Notion 원본·새 머신 clone 순서)만 남겼다(`3609ba5`). **레포 문서는 "이 레포에서만 다른 것"만, 전역 계약은 전역 파일에** — 이 원칙이 이번 재배치 세 번의 공통 이유다.

### REQ-10 Phase 5(diary) 완료 — 케이스 24건, `/testrun`이 승인 표 대비 코드 누락 1건을 실제로 잡아냈다

**착수 전 미결 3건 확정(대화, `181e8a3`)** — ① "미래 날짜 불가"의 기준 타임존: **KST 자정.** REQ-16 ADR-0002가 "계산은 `Asia/Seoul`"을 이미 다이어리 미래 날짜 케이스까지 이름을 대며 답해 둔 상태라 새로 정할 것이 없었다. ② `limit` 50 vs `CursorRequest.MAX_LIMIT`(100): **`DiaryService`에서 `min(limit, 50)`으로 한 번 더 클램프**, framework 전역 상한은 안 건드림(이미 나간 weight·activity·feeding·shed 계약을 깰 수 없어서). ③ 거꾸리 경고: **범위 제외 확정** — 애초에 `API I/F` 응답 어디에도 형태가 없어(「소스 구조」에만 있음) Phase 5를 막는 미결이 아니었다.

**`/testgen`** — 21건 승인 후 작성 직전에 `ConditionTag` enum 설계가 넷째 미결로 드러났다: 이 프로젝트 첫 **한글 값** enum이라 `Species`·`ActivityType`처럼 그대로 갈 전례가 없었다. **영문 상수(`NORMAL`/`ACTIVE`/`FLOPPY_TAIL`/`VOMITING`) + `@JsonValue`/`@JsonCreator`**로 확정 — DB·Java 상수명은 AGENTS §5(영문 UPPER_SNAKE_CASE)를 지키고 JSON 왕복만 한글로 갈라 둔다.

**`/implement REQ-10 5`** — `feat/req10-phase5-diary` 브랜치. `DiaryEntry`(`BaseTimeEntity`, `updated_at` 있음 — D11) · `DiaryService`(종 제한 없음, `Clock` 주입해 미래 날짜 거부) · `condition_tag` 필터 전용 쿼리 메서드 2종. 자체 실행 23/23 1차 통과, 커밋 `183f7e4` 푸시.

**`/testrun REQ-10`** — 여기서 **REQ-10-107(`DiaryService` 응답에 `updated_at` 있음, D11 근거)이 검증 계약 표엔 있는데 테스트 코드가 없는 누락**으로 잡혔다. 승인까지 됐던 케이스가 실제로는 안 쓰인 것 — Phase 5를 21건으로 셌지만 표엔 23건이 있었고 그중 하나를 빠뜨린 채 구현까지 넘어갔던 셈이다. `/testgen`이 `DiaryServiceTest`에 케이스를 추가하고(`update()` 경로, `ReflectionTestUtils`로 `updatedAt` 고정값을 심어 응답에 그대로 전달되는지 단언 — `create()`는 JPA Auditing이 실제로 채우는 값이라 mock 리포지토리로는 검증 불가), `/implement`가 커밋(`acf1299`)·푸시했다. **구현 자체는 이미 정답을 반환하고 있어 소스 코드 변경은 없었다** — 순수하게 검증 계약의 빈틈만 메운 경우.

재실행한 `/testrun REQ-10` — REQ-10 전체(weight·activity·feeding·shed·diary) **141 메서드 실행 · 실패 0** · 표 111행(Phase 0 프로브 3건 제외) ↔ 코드 111 ID 정확히 일치 · 근거 인용 표본 검사 전건 원문 확인. **REQ-10 Phase 0~5 전부 완료.** 단, Phase 1·2는 이전부터 열려 있던 로컬 DB keyset 경계 실측 1건씩이 여전히 보류라(인증 토큰·펫 생성 선행 필요) 계획서 체크박스는 그대로 열어 뒀다 — 이번 세션에서 손대지 않았다.

### REQ-10 미결 질문 잔여 5건 중 결정 성격 2건 정리 (`f8a8ef7`)

Phase 마무리 후 남은 미결을 확인해 보니 실행 성격(로컬 DB 실측 · Notion 원본 편집)이 아니라 **판단이 필요한 것**이 shed 관련 2건 있었다 — 대화로 닫았다.

- **"완료일은 시작일 이후여야 함"(탈피 기록 콜아웃)** — **삭제 역반영 대상으로 확정.** `shed_records`(`docs/specs/db-schema.md:183`)는 `shed_date` 컬럼 하나뿐이고 완료 여부는 `is_complete` 불리언으로만 표시한다 — 시작일/완료일 두 날짜를 쓰던 예전 설계의 흔적으로 보이고, 지금 스키마엔 대응하는 컬럼 쌍이 없다. 코드 영향 없음
- **`is_assisted ↔ condition_tag '탈피도와줌'` 연계**(`docs/specs/db-schema.md:189`) — **폐기.** 근거는 D1이 이미 세워 둔 것과 같다(**ADR-0001, 파생 상태 단일 출처**) — `shed_records.is_assisted` 자체가 이미 "탈피도와줌" 상태의 단일 출처라 다이어리 `condition_tag`로 재반영할 이유가 없다. timeline(REQ-12)이 이 상태를 보이고 싶으면 `shed_records`를 직접 읽으면 된다

둘 다 계획서 미결 질문에 `[x]` + 결론을 남기고 **역반영 목록에 ⓓ·ⓚ로 편입**했다 — 실제 Notion 삭제는 사람 몫으로 남아 있다.

**REQ-10 잔여 상태 (2026-09-02 세션 종료 시점)** — 코드·테스트·판단성 미결은 전부 닫혔다. 남은 건 순수 실행 항목 셋뿐: ① Notion 원본에서 문구 2건(ⓓ·ⓚ) 삭제 ② Phase 1·2 로컬 DB keyset 경계 실측(인증 토큰·펫 생성 선행 필요) — 셋 다 사람 또는 다음 세션의 실측 작업이고, 이번 세션에서 더 진행하지 않는다.

## 2026-09-01

> **REQ-16 완결.** Notion 탭 2곳(API "날짜 포맷" 행·DB DDL 코드블록)까지 사람 손으로 반영되며 미결 ④가 완전히 닫혔다. 그 과정에서 새로 드러난 것 셋 — **ERD 페이지가 전수 조사에서 또 빠져 있었다**(여섯 번째 열거 오류), **DDL 붙여넣기가 한 번 깨졌다가 재확인으로 잡혔다**, **머지·삭제된 로컬 브랜치 위에 커밋하는 사고**.

### PR #43 머지 — REQ-16 Phase 4 문서 역반영이 `main`에 들어갔다

전날 `docs/req16-phase4-backfill`이 로컬에만 있던 것을 PR로 만들어 CI 확인(SHA 대조) 후 머지, 브랜치 삭제까지 정상 처리했다.

### ⭐ 「ERD 설계」 페이지가 전수 조사 범위 밖이었다 — 여섯 번째 열거 오류

2026-08-28 미결④ 전수 조사는 `API I/F` 응답 12행·「소스 구조」·「테이블 정의서」까지는 잡았지만 **「ERD 설계」 페이지의 테이블 스키마 코드블록 9개**를 놓쳤다. 사용자가 직접 지적해 드러났다 — `timestamp`가 9개 코드블록(19곳) + 산문 2곳에 그대로 남아 있었다. ERD 페이지는 DB 탭 본문과 달리 **일반 페이지 객체**라 `notion-update-page`로 정상 반영됐다.

> 앞선 다섯 번(날짜 컬럼 개수 · `now()` 호출 곳 수 · `LocalDateTimeUtil` 존재 · 타입 이름 · 테이블 정의서)과 같은 패턴이다. **"전수 조사"라고 부른 것이 실제로는 전수가 아니었다** — Notion 설계 트리 안에서 시각 리터럴이 있을 수 있는 위치를 사람이 나열하는 방식 자체가 계속 새는 지점을 만든다. 이 REQ 안에서만 여섯 번이면, 다음에 비슷한 전수 조사를 할 때는 "나열이 아니라 순회"로 방법을 바꿀 필요가 있다.

### DB 탭 DDL 붙여넣기가 한 번 깨졌다 — 재조회로 잡았다

사용자가 1차로 DDL 코드블록(레포 V1+V2+V3 병합본)을 Notion에 붙여넣었으나, **10개 테이블 중 8개에서 줄 중간이 잘려나가 있었다** — `references us`(`ers (id),` 유실), `-- 앱 검증T`(코멘트 전체 유실), `created_at ... now()` 뒤 쉼표 누락(다음 줄과 붙어 문법 깨짐), `idx_photos_diary_entry_id on photos (diary_e_id is not null;`(단어 중간이 뭉개짐) 등. 붙여넣기 소스의 줄바꿈 폭이 원인으로 추정된다. 다시 붙여넣도록 안내했고, 재조회에서 10개 테이블 전부 정상 확인됐다.

> ⚠️ **Notion에 사람이 붙여넣은 긴 코드블록은 "붙여넣었다"는 보고만으로 믿지 말 것.** 반드시 `fetch`로 재조회해 실제 저장된 텍스트를 확인한다 — 이번처럼 절반 넘는 줄이 조용히 잘려나가도 Notion 쪽에서 에러를 내지 않는다.

### ⚠️ 머지·삭제된 로컬 브랜치 위에 커밋했다 — `git fetch --prune` 전에는 안 보인다

`docs/req16-phase4-backfill`은 PR #43로 머지되고 원격 브랜치도 삭제된 상태였는데, **로컬 브랜치는 그대로 남아 있었고 그 위에 `db-schema.md` 수정을 그대로 커밋해버렸다.** `git status`·`git branch --show-current` 어디에도 이상 신호가 없었다 — `git fetch --prune`으로 원격 추적이 `gone`으로 바뀐 뒤에야 드러났다. 복구는 `origin/main` 기준 새 브랜치를 파서 해당 커밋만 `cherry-pick`, 낡은 로컬 브랜치는 삭제. PR #44로 다시 만들어 CI 확인(SHA 대조) 후 머지했다.

> AGENTS §4에 이미 있는 "로컬 전용 브랜치는 다음 세션에서 안 보인다" · "스택 PR base 자동 재지정" · "CI 초록불은 SHA로 확인" 항목과 같은 결이다 — **PR이 머지되고 원격 브랜치가 삭제된 뒤에도 로컬 브랜치는 스스로 없어지지 않고, git은 그 위에 이어지는 커밋을 경고 없이 받아준다.** AGENTS.md §4 승격을 제안한다(아래 "계약 승격 제안" 참고).

### `docs/specs/db-schema.md`가 V3 이후로 한 번도 갱신되지 않았었다

전날 Phase 4 실행 기록은 "개요 표에 두 행 신설"이라고 적었지만 실제 커밋 diff에는 이 파일이 없었다 — 기록과 코드가 어긋난 사례. 오늘 시각 컬럼 19개 `timestamp` → `timestamptz`, 개요 표 2행 신설(시각 컬럼 타입/날짜만 있는 컬럼), 대조 이력 갱신까지 마치고 PR #44로 머지했다.

### REQ-16 Phase 4 완료 기준 충족 — 체크 켬, REQ-16 ✅

미결④의 남은 ⏸ 항목(탭 2곳)이 모두 닫혀 Phase 4 완료 기준 "원본과 코드가 어긋나는 곳 0건"을 충족했다. REQ-16 상태를 ✅로 올린다. **미결 ⑦⑧**(`BaseSoftDeleteEntity.softDelete()`의 `Clock` 주입 여부 · `JwtTokenProvider.create()`의 `new Date()` 규약 포함 여부)은 이 REQ 밖 판단으로 계획서에 열린 채 남아 있다.

**계약 승격 제안** — 위 "머지·삭제된 로컬 브랜치" 함정을 AGENTS.md §4에 추가할 것을 제안한다. 다음 세션에서 확인·승인 필요.

> ✅ **계약 승격 완료 (같은 날 오후).** 사용자 승인 후 AGENTS.md §4에 반영(`87c1188`).

### Notion 역반영 ⓖ~ⓙ 완료 — REQ-10 Phase 3 착수 전 원본 정리

REQ-10 Phase 3(feeding) 착수 전 선행 조건이던 Notion 역반영 4건(급여 기록 callout·food_size 절, 거식 스트릭 응답 예시 자기모순, 「소스 구조」 §10 `FEATURE_NOT_SUPPORTED_SPECIES`)을 API로 직접 반영했다(`016a0ca`) — 이번엔 REQ-16 때와 달리 개별 행(row)·일반 페이지라 쓰기가 됐다. **ⓘ의 전제가 틀렸었다** — "`SHED_NOT_SUPPORTED_SPECIES` 제거" 대상이 Notion 어디에도 없었다(`notion-search`로 확인, 레포 자체 명명이었다). 제거할 게 없어 추가만 했고 `api-list.md`도 맞췄다 — REQ-16에서 반복된 "열거가 실제 원본과 어긋난다" 패턴과 같은 종류.

### REQ-10 Phase 3(feeding) — `/testgen` → `/implement` → `/testrun`, 케이스 25건 전부 통과

**`/testgen`** — 계획서 완료 기준·미결 질문에서 케이스 25건(REQ-10-43~67)을 뽑아 표를 승인받고 테스트 코드까지 썼다. 대상 클래스가 없어 컴파일 안 되는 상태(Phase 1·2 와 같은 순서)로 남겨 뒀다. 설계로 정한 것 — `AnorexiaStreakCalculator.calculate(lastEatenAt, now)` 는 급여 기록 List 가 아니라 "마지막 정상 급여 시각(nullable)"만 받는 순수 정적 메서드다. **이 시그니처 자체가 "일수냐 건수냐" 미결을 답한다** — 건수는 파라미터에 아예 없어 계산에 들어올 수가 없다.

**`/implement REQ-10 3`** — `main`이 보호 브랜치라 `feat/req10-phase3-feeding`을 새로 팠다. `FeedingLog`(`Pet`과 같은 이유로 필드 8개 → `@Builder`+`@AllArgsConstructor(PRIVATE)` 필요, AGENTS §5) · `FeedingService` · `FeedingController` · `V4__feeding_food_size.sql` · `ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES` 신설까지 구현. 자체 실행 26/28 통과, `REQ-10-51` 2건 실패로 `wip` 커밋(`9e148df`) 후 미푸시.

**`/testrun REQ-10`** — 실패 2건을 근거(D8 "`id` desc 타이브레이크", 시간대 표기와 무관)와 대조해 **(a) 테스트 결함**으로 분류하고 고쳤다.

> ⭐ **테스트가 만드는 `CursorCodec`이 오프셋을 조용히 뭉갠다.** `FeedingServiceTest`는 `NOW`를 `2026-07-07T12:00:00+09:00`으로 선언했는데, 이 테스트가 직접 만든 `CursorCodec`(`new ObjectMapper().findAndRegisterModules()`)은 앱의 `JacksonConfig`(KST 존 설정)를 안 거친 무설정 매퍼다 — 인코드 시 Jackson jsr310 기본값(`ADJUST_DATES_TO_CONTEXT_TIME_ZONE`)이 `+09:00`을 `Z`로 정규화해, **순간은 같은데 레코드 동등성 비교(`OffsetDateTime.equals`)만 깨진다.** `WeightServiceTest`는 `LocalDate`라 애초에 무관했고 `ActivityServiceTest`는 우연히 `ZoneOffset.UTC` 리터럴을 써서 드러나지 않았다 — feeding이 KST 리터럴을 쓴 첫 케이스라 처음 노출됐다. `NOW`를 `Z` 표기(같은 순간)로 바꿔 해결.

수정 후 `/testrun`을 다시 돌려 REQ-10 전체(weight·activity·feeding) 82케이스 전부 통과 확인. 검증 계약 표의 `결과` 열을 채우고 Phase 3 절 갱신 — 단 이 시점엔 **완료 기준의 "V4 로컬 DB 적용·`ddl-auto: validate` 통과"가 여전히 미확인**이라 체크는 켜지지 않았다(Phase 1·2 와 같은 성격의 보류. 아래에서 닫힌다).

**계약 승격 제안** — "테스트에서 `CursorCodec`을 직접 만들 때 `OffsetDateTime` 리터럴은 `ZoneOffset.UTC`(또는 `Z`)를 쓴다 — 비-UTC 오프셋을 쓰면 인코드 후 레코드 동등성 비교가 조용히 깨진다"를 AGENTS.md에 승격할 것을 제안한다. shed·diary Phase에서 같은 함정을 또 밟을 수 있다.

> ✅ **계약 승격 완료 (같은 날).** 사용자 승인 후 AGENTS.md §5에 반영(`1cf9a5b`).

### PR #45 머지 — 그런데 `main`이 origin과 3커밋 벌어져 있었다

Phase 3 구현·테스트 수정을 전부 커밋한 뒤 PR #45를 만들려고 보니, **`main`이 애초에 `origin/main`과 3커밋 차이가 나 있었다** — 어제(`7a78bda`)와 오늘 오전(`87c1188`·`016a0ca`) 작업이 로컬 `main`에서만 커밋되고 한 번도 푸시된 적이 없었다. PR #45는 이 상태의 `main`에서 딴 브랜치라 REQ-16 문서 3건이 Phase 3 구현과 한 PR에 같이 실렸다 — 의도한 묶음은 아니지만 내용 유실은 없다. CI 확인(SHA 대조) 후 스쿼시 머지, 원격 브랜치 삭제까지 확인.

> 머지 후 로컬 정리도 스쿼시 특성을 고려해서 했다 — `git diff origin/main..feat/req10-phase3-feeding`가 빈 걸 먼저 확인해(스쿼시 커밋이 브랜치 내용을 전부 담았다는 뜻) `git branch -f main origin/main`으로 로컬 `main`을 맞추고, `git branch -D`로 강제 삭제했다(스쿼시라 `-d`는 "안 머지됨"으로 거부한다 — 정상 동작).

### ⭐ V4 로컬 DB 확인이 실제 구현 결함을 잡았다 — `food_size` CHAR 추론

Phase 3 완료 기준에 남아 있던 마지막 항목("V4가 로컬 DB에 적용되고 `ddl-auto: validate` 통과")을 실제로 돌렸다 — `colima start` → `docker start petkok-pg` → `set -a && . ./.env && set +a && ./gradlew bootRun`. **기동이 막혔다:**

```
Schema-validation: wrong column type encountered in column [food_size]
in table [feeding_logs]; found [varchar (Types#VARCHAR)], but expecting
[char(1) (Types#CHAR)]
```

원인 — `FeedingLog.foodSize`가 `@Enumerated(EnumType.STRING)` + `@Column(length = 1)` 조합이었는데, **Hibernate 6가 이 조합을 `CHAR(1)`로 추론한다.** `V4__feeding_food_size.sql`은 Notion 원본 그대로 `varchar(1)`이 맞으므로 — 마이그레이션이 아니라 엔티티의 `length = 1` 힌트를 뺐다. 재기동으로 `Started PetKokApplication` 확인, REQ-10 82케이스 재확인 통과. `fix/req10-feeding-food-size-column-type` 브랜치 → PR #46 → CI 확인 후 머지(`ee57090`).

> ⭐ **REQ-16 계약("`ddl-auto: validate`는 컬럼 존재만 보고 타입은 안 본다")과 정면으로 부딪히는 것처럼 보이지만 아니다.** REQ-16이 실측한 사각지대는 `timestamp`/`timestamptz`처럼 **JDBC 타입 코드가 같게 취급되는 조합**이었다. 이번 `VARCHAR`/`CHAR`는 **JDBC 타입 코드 자체가 다르다**(`Types#VARCHAR` vs `Types#CHAR`)—validate가 원래도 잡는 부류다. "타입을 안 본다"는 전체 규칙이 아니라 **"타입 코드가 같은 것끼리는 못 가른다"**로 좁혀 읽어야 정확하다. `docs/plans/PLAN-REQ-10-record-domains.md` 제약·함정 절에 이 구분을 남겼다.

**계약 승격 제안** — "단일 문자 값을 갖는 문자열/enum 컬럼에 `@Column(length = 1)`을 쓰지 않는다(Hibernate 6가 `CHAR`로 추론해 `varchar` DDL과 충돌한다)"를 AGENTS.md에 추가할 것을 제안한다. shed·diary Phase의 단일 문자 enum(있다면)에서 재발할 수 있다.

### REQ-10 Phase 3(feeding) 완료 — 체크 켬

25개 케이스 전부 `✅` + V4 로컬 DB 확인까지 닫혀 완료 기준을 전부 채웠다. `docs/plans/PLAN-REQ-10-record-domains.md` Phase 3 체크 켬. **다음은 Phase 4(shed)** — 선행 조건 없음, 곧바로 `/testgen`부터 시작할 수 있다.

### `length = 1` 계약 승격 — "CHAR가 더 맞는 거 아닌가"라는 타당한 반문에 답했다

계약 승격을 올리자 사용자가 되물었다 — **"애초에 DDL이 잘못된 거 아닌가? CHAR가 더 올바른 거 아닌가?"** 컬럼 하나만 보면 맞는 말이다: 값이 항상 정확히 1글자면 교과서적으론 `CHAR(1)`이 자연스럽다.

**하지만 이 레포는 "enum 컬럼은 전부 varchar"를 이미 AGENTS §5에 예외 없는 규칙으로 못 박아 뒀고**, 실제로 `species`·`gender`·`provider`·`activity_type`·`condition_tag` 전부 varchar다. `food_size`(S/M/L)만 우연히 1글자라고 CHAR로 가면 **이 레포에서 유일하게 다른 타입 규칙을 쓰는 enum 컬럼**이 된다 — `TimeConstant.KST`를 한 곳에만 두기로 한 것, `PATCH` DTO에 `@NotNull` 금지를 전역으로 둔 것과 같은 결의 "예외 하나가 규칙을 무력화한다" 패턴. 그래서 **DDL이 아니라 엔티티의 `length = 1` 힌트를 고치는 방향이 맞다**고 답했고, 사용자가 승인해 AGENTS §5에 반영했다(PR #47, `40eec58`).

> 판단 근거를 남기는 이유 — 다음에 비슷한 "왜 이 타입이 아니라 저 타입이냐"는 질문이 나오면, "프로젝트 전역 규칙과의 일관성"이 개별 컬럼의 이론적 정확성보다 우선한다는 이 레포의 판단 기준을 다시 설명할 필요가 없다.

### ⚠️ 같은 세션에서 "main이 origin과 벌어짐"이 두 번째로 재발했다

PR #47을 만들려고 브랜치를 파기 전, 그날 오전 `/checkpoint` 커밋(`bc3c4ad`)을 `main`에 바로 찍어 두고 푸시하지 않은 채 그 위에서 `docs/req10-char-length-pitfall` 브랜치를 팠다. PR #47 자체는 문제없이 머지됐지만(스쿼시가 `bc3c4ad`의 내용도 함께 실어 올렸다), **머지 후 로컬 정리에서 또 같은 함정을 밟았다** — `git branch -f main origin/main`이 "현재 체크아웃된 브랜치는 강제 이동 못 한다"로 막혔고, `git merge --ff-only`도 "diverged"로 막혔다. 이번엔 이틀 전 PR #43~44 때 겪은 것과 **완전히 같은 원인**이다 — `main`에 직접 커밋하고 안 미는 습관.

> 대응은 같았다(`git diff` 양방향으로 로컬 `main`에 원격에 없는 내용이 없는지 확인 → `git reset --hard origin/main`)지만, **한 세션에서 두 번 겪었다는 것 자체가 신호다.** AGENTS §4의 기존 문구("PR이 머지되고 원격 브랜치가 삭제된 뒤에도 로컬 브랜치는 스스로 없어지지 않는다")는 "브랜치를 지우는 시점"의 함정을 다루는데, 이번 두 사고는 **"`main`에 직접 커밋한 뒤 안 미는" 더 앞 단계의 습관**이 원인이다.

> ✅ **계약 승격 완료 (같은 날, `0fb0da3`).** 사용자 승인 후 AGENTS §4에 "`main`에 직접 커밋했으면 그 자리에서 바로 `git push`한다"를 반영했다. **이 커밋 자체를 그 규칙대로 브랜치·PR 없이 `main`에 바로 push해 원칙을 실증했다** — 문서 1줄뿐이라 리스크는 낮았지만, `main` 직접 커밋이 PR·CI 게이트를 건너뛰는 유일한 경로가 됐다는 점은 의식적으로 남겨 둔다. 이후 `git status`·로컬 브랜치·`main`↔`origin/main`·열린 PR 4가지를 전부 훑어 미커밋·미푸시·미머지가 0건임을 확인했다.

## 2026-08-31

> 3일 만의 재개. 한 일은 둘이다 — **`main` 밖에 있던 REQ-16 Phase 0~2 를 머지**하고, **Phase 3(계산 기준 KST 고정)을 끝냈다.** 코드는 작지만(6파일) 기록할 것은 **검증 계약 문구가 낡아 규칙이 공허해질 뻔한 것**과 **프로브 되돌리기로 작업을 날린 사고** 둘이다.

### 커밋 9건이 사흘간 `main` 밖에 있었다 — PR 을 안 만들었기 때문이다

`docs/req16-workplan` 브랜치가 원격에 **푸시까지 돼 있었는데 PR 이 없었다.** Phase 0~2 전체(`V3` 마이그레이션 · 엔티티 타입 전환 · Jackson 규약)와 진행 기록 4건이 여기 묶여 있었다. AGENTS §4 가 적어 둔 것은 "로컬 전용 브랜치는 다음 세션에서 없는 것과 구별되지 않는다"였는데, **푸시했어도 PR 이 없으면 인덱스만 보고는 알 수 없다** — `PROGRESS.md` 인덱스가 "Phase 0~2 완료"라고 말하고 있어 `main` 에 있다고 읽힌다. 실제로 이번 `/progress` 조회에서 그 불일치로 드러났다.

> **CI 초록불 판정에서 도구 두 개가 갈렸다.** `gh pr checks 41` 은 `pass`, `gh run list` 는 같은 SHA 를 `in_progress` 로 보고했다. AGENTS §4 의 "이전 실행분 결과를 그대로 보여줄 수 있다"와 방향이 반대인 경우다(이번엔 목록 쪽이 낡았다). `gh run view <id> --json headSha,status,conclusion` 으로 **run 자체를 열어** `e43be94` · completed · success 를 확인하고 머지했다. **어느 쪽이 낡았는지는 미리 알 수 없으므로 SHA 를 들고 run 을 직접 여는 것이 유일하게 안전하다.**

### ⭐ 검증 계약 문구가 Phase 1 에서 낡아 있었다 — 그대로 썼으면 통과하는 가짜 규칙

REQ-16-10 은 "`LocalDateTime.now()` 직접 호출이 `business`·`framework` 에 0건"이었다. 그런데 **Phase 1 이 대상을 전부 `OffsetDateTime` 으로 바꿨다.** 문구대로 규칙을 쓰면 —

| 대상 | 문구대로면 |
|---|---|
| `LocalDateTimeUtil` 의 `LocalDateTime.now()` 2건 (D10 대상) | 걸린다 |
| **`AuthService` 의 `OffsetDateTime.now()` 2건 (Phase 3 본 목표)** | **안 걸린다** |

즉 **D10 만 하고 `Clock` 주입을 아예 안 해도 REQ-16-10 은 초록불**이었다. 무인자 `now()` 5종(`LocalDateTime`·`OffsetDateTime`·`LocalDate`·`Instant`·`ZonedDateTime`)으로 넓히고 `now(Clock)` 오버로드만 열어 뒀다.

> **이것이 "계획서 열거가 어긋났다"의 네 번째 얼굴이다.** 앞의 셋은 `date` 컬럼 개수 · `now()` 호출 곳 수 · `LocalDateTimeUtil` 의 존재였고, 이번은 **타입 이름**이다. 공통점은 전부 *Phase 1 이 실제로 코드를 바꾼 뒤에도 계획서 문장이 그대로 남아 있었다*는 것 — **계획서는 착수 시점의 사실을 적고, 코드가 바뀌어도 스스로 갱신되지 않는다.** 그래서 Phase 착수 때마다 열거를 다시 세야 한다.

### `Asia/Seoul` 은 두 곳이 아니라 세 곳이었다

계획서 함정 절이 `JacksonConfig` · `OffsetDateTimeDeserializer` 둘만 셌는데 **`LocalDateTimeUtil.ZONE_ASIA_SEOUL` 이 빠져 있었다.** 하필 D10 이 어차피 건드리는 파일이다. 셋을 `TimeConstant.KST` 한 곳으로 합쳤고, **"늘리지 말 것"이라는 사람이 읽는 경고를 REQ-16-16 케이스로 바꿨다** — 소스 텍스트를 훑어 상수 클래스 밖의 리터럴을 잡는다.

> ⚠️ **REQ-16-16 은 0건이 "깨끗함"인지 "스캐너 고장"인지 구별되지 않는 종류다**(CLAUDE.md — 빈 패턴은 전건 매치). 그래서 단언 앞에 **역프로브를 내장했다** — 훑은 파일이 비지 않았고 `TimeConstant.java` 자신은 리터럴을 갖고 있어야 통과한다.

### 프로브 3건 — 규칙이 실제로 무는지 실측했다

CLAUDE.md 의 "구조 규칙을 고치면 일부러 위반을 심어 잡히는지 확인할 것"을 그대로 돌렸다.

| 프로브 | 결과 |
|---|---|
| `AuthService` 를 `Clock` 이전으로 되돌림 | REQ-16-10 이 **2곳을 모두 지목하며 FAIL** — 규칙이 공허하지 않다 |
| `JacksonConfig` 에 `Asia/Seoul` 재삽입 | REQ-16-16 이 **파일명까지 지목하며 FAIL** |
| `isExpired` 를 항상 `true` | **REQ-16-17 만 FAIL** — REQ-07-21·REQ-16-12 는 통과 |
| `isExpired` 를 항상 `false` | REQ-16-12·REQ-07-21 FAIL |

> ⭐ **세 번째가 REQ-16-17 을 추가한 이유를 사후에 증명했다.** 만료 판정이 **전부 거절로** 고장 나도 기존 스위트(REQ-07-21)와 신규 REQ-16-12 는 **둘 다 초록불**이다. 둘 다 "만료면 거절"만 보기 때문이다. 경계는 한쪽만 재면 고정되지 않는다.

### ⚠️ 프로브를 되돌리다 작업을 날렸다 — `git checkout <파일>`

프로브 1 을 되돌리려고 `git checkout src/.../AuthService.java` 를 썼는데, **그 파일의 변경이 스테이지되지 않은 상태였다.** 이 명령은 인덱스(=HEAD)에서 복원하므로 프로브로 심은 것뿐 아니라 **그 Phase 의 구현 전체(`Clock` 주입)가 함께 사라졌다.** 재작성해 복구했고 이후 프로브 2·3 은 백업 복사본(`cp`)으로 되돌렸다.

> **이 레포는 프로브를 "심었다 지우는" 방식으로 상시 돌린다**(REQ-09·10·16 전부). 그 워크플로가 `git checkout` 과 만나면 **아직 커밋 안 한 구현을 조용히 삼킨다** — 에러도 경고도 없고, 되돌린 파일이 "원래대로"로 보인다. 프로브 되돌리기는 **커밋 전이면 `cp` 백업**으로 한다. CLAUDE.md 계약 승격 대상이다.

### Phase 3 완료 기준 셋 중 하나는 채운 게 아니라 넘겼다

완료 기준의 "KST 자정 전후 판정이 케이스로 고정됨"은 **케이스가 없다.** 검증 계약 절이 이미 *"판정 로직(당일·미래·일수)은 REQ-10 Phase 3 이후에 들어온다. 여기서는 상수·`Clock` 까지만 고정하고, 자정 경계 케이스는 REQ-10 이 가져간다"*고 적어 둔 대로다. 계획서에 취소선과 이유를 남기고 체크했다 — **케이스 5건 전부 녹색인 것과 완료 기준 충족은 같지 않다.**

### 미결 2건이 새로 생겼다 (둘 다 이 REQ 밖 판단)

- **⑦ `BaseSoftDeleteEntity.softDelete()`** — 계획서가 자기 자신과 어긋났다. `범위 — 포함` 은 "`AuthService` 2곳", `제약·함정` 은 "3곳 · `Clock` 주입은 Phase 3 몫". **전자를 따랐다** — JPA 엔티티라 빈 주입이 불가능하고, `deleted_at` 은 벽시계 파생이 아니라 **순간**이라 D4 의 자리가 아니다. TZ 위험은 Phase 1 의 `OffsetDateTime` 전환에서 이미 사라졌고 **남는 이득은 테스트 고정 하나뿐**이라 엔티티 시그니처를 바꿀 값을 못 했다
- **⑧ `JwtTokenProvider.create()` 의 `new Date()`** — 계획서 어디에도 없다. `java.time` 이 아니라 규칙에 안 걸리고 순간이라 TZ 위험도 없지만, **발급 시각을 고정할 수 없다**

**남은 것 (REQ-16)** — **Phase 4(문서 역반영) 하나.** 케이스가 없고, 2026-08-28 의 `API I/F` 40행 전수 조사 결과가 그대로 작업 지시서다(응답 12행 · 「소스 구조」 §2·§5·§6 · `CLAUDE.md` 계약 2건). 미결 ⑦⑧ 은 Phase 4 를 막지 않는다.

### Phase 4 — 문서 역반영. 조사에서 빠진 곳이 하나 더 있었다 (오후)

`API I/F` 응답 12행의 리터럴 20개를 `Z` → `+09:00` 으로 바꾸고, 「소스 구조」 §2·§5·§6 과 레포 2개 문서를 맞췄다. 조사 목록대로 움직였으므로 **기록할 것은 목록에 없던 것들**이다.

**⭐ 표기 방식이 미결이었다 — "같은 순간의 다른 표기"가 예시를 망친다.** 계획서는 그렇게 적었지만, 리터럴을 그대로 변환하면 **12행 중 9행에서 날짜가 하루 밀린다**(`15:00:00Z` → `07-01T00:00:00+09:00`). 다이어리는 `entry_date: 2026-06-30` 과, 탈피는 `shed_date` 와, 갤러리는 `taken_at` 과 어긋나고, **통합 타임라인은 `"date": "2026-06-30"` 안의 이벤트 셋 중 둘이 다음날로 넘어가 "시간순 정렬" 예시가 깨진다.** D4(달력 판정 = KST)를 설명해야 할 문서가 정반대로 읽히게 된다.

> **숫자를 두고 `Z` 만 `+09:00` 으로 바꾸는 쪽을 골랐다.** 순간은 달라지지만 **예시는 데이터가 아니라 설명**이고, 이쪽이 12행 전부를 내부적으로 일관되게 만든다(타임라인 09:12 → 18:00 → 21:00 KST 로 정렬 유지). 계획서 문구를 곧이곧대로 따랐으면 계약 문서가 스스로를 반박했을 것이다.

### ⭐ 「테이블 정의서」가 전수 조사 범위 밖이었다 — 다섯 번째 열거 오류

`timestamp` 가 **19컬럼** 남아 있었다. 공교롭게 `V3` 가 바꾼 19개와 정확히 같은 수다. 미결 ④ 조사는 레포 `db-schema.md` 는 확인했지만(0건) **그 원본인 Notion 페이지는 보지 않았다** — `db-schema.md` 가 파생 요약이라는 것을 알면서도 원본으로 거슬러 올라가지 않은 것이다.

> **AGENTS §0 이 DDL 의 1차 출처로 지목한 곳**이라, 그대로 뒀으면 다음 사람이 `timestamp` 로 설계했을 자리다. 앞의 네 번(날짜 컬럼 개수 · `now()` 호출 곳 · `LocalDateTimeUtil` 존재 · 타입 이름)은 전부 **계획서 안의** 열거였는데, 이번은 **조사 자체의 범위**가 좁았던 경우다. 파생 요약을 확인한 것으로 원본을 확인했다고 치지 말 것.

`date` 컬럼 6개(`entry_date`·`shed_date`·`measured_at`·`birthday`·`adoption_date`·`taken_at`)는 그대로 뒀고, 개요 표에 "시각 컬럼 타입 / 날짜만 있는 컬럼" 두 행을 신설해 **왜 둘이 갈리는지**를 문서 안에 남겼다.

### 탭 본문은 역시 막혔다 — 이번엔 실제로 쳐서 확인했다

`CLAUDE.md` 가 적어 둔 그대로다. 가정하지 않고 실제 호출로 확인했다.

```
validation_error: Object 38eb81b5-…-fe5be636d196 is not a page or database and cannot be updated
```

**사람 손이 필요한 2곳** — ⓐ 「설계」→ API 탭 본문의 "날짜 포맷" 행(아직 `2026-06-30T15:00:00Z`) ⓑ 「설계」→ DB 탭의 DDL 코드블록(아직 `timestamp`). ⓑ 의 원본인 「테이블 정의서」는 고쳤으므로 파생 쪽만 남았다.

> **그래서 Phase 4 완료 기준 "원본과 코드가 어긋나는 곳 0건" 은 아직 미충족이고, 체크를 켜지 않았다.** 내가 할 수 있는 것은 다 했지만 기준은 기준이다.

### Notion 편집 함정 2건 (실측)

- ⚠️ **`old_str` 에 리스트 마커(`- `)를 포함하면 매칭되지 않는다.** `- 컬럼명: \`deleted_at timestamp NULL\`` 이 `No matches found` 로 거절됐는데, 마커를 뺀 `` `deleted_at timestamp NULL` `` 은 바로 통과했다. 리스트 항목의 본문만 콘텐츠로 취급되는 모양이다
- ✅ **`content_updates` 배열은 원자적이다.** 위 실패로 **같은 배치의 다른 3건도 전부 적용되지 않았다** — 부분 반영이 없다는 뜻이라 오히려 안전하다. 다만 한 건이 실패하면 나머지가 조용히 안 들어가므로, **배치가 실패하면 "일부는 됐겠지" 라고 넘기지 말고 전부 다시 넣어야 한다**

**남은 것 (REQ-16)** — **Notion 탭 2곳(사람 손)** 이 닫히면 Phase 4 `- [x]` · REQ-16 ✅. 코드 작업은 없다. 미결 ⑦⑧ 은 이 REQ 밖 판단이라 별건.


## 2026-08-28

> REQ-10 첫 커밋. `DomainBoundaryTest` 에 `PetAccessGuard` 소비 예외 3건(`business.pet.service` · `data.pet.dto` · `data.pet.enums`)을 넣고 프로브로 확인했다(`016c692`, PR #36). 어제 문서 브랜치(PR #35)와 같이 머지됐다. 코드는 한 파일이고, 기록할 것은 **확인 방법**과 **경계 판단** 둘이다.

### 프로브는 4건이었다 — 계획서의 3건 + "예외 없는 원본에서 FAIL"

REQ-10-01~03 은 계획서대로 심었다 지웠다(가드 주입 PASS · `PetRepository` 우회 FAIL · `Pet` 엔티티 FAIL — 발화 규칙은 셋 다 `NO_CROSS_DOMAIN_DEPENDENCY`). 여기에 **규칙 변경을 `stash` 로 걷어낸 상태에서 01 을 한 번 더** 돌렸다 — FAIL 이 나야 "예외가 실제로 필요하고 프로브가 공허하지 않다"가 성립한다. REQ-09 프로브에도 같은 항목("예외 없이 사용 FAIL")이 있었는데, 규칙 파일이 바뀐 뒤에는 다시 재야 한다. CLAUDE.md "구조 규칙을 고치면 일부러 위반을 심어 잡히는지 확인"의 반쪽은 **고치기 전에도 잡혔는지**다.

> `--tests '*DomainBoundaryTest'` 로 걸어도 XML 에는 **ArchUnit 8건 전부** 기록된다(`ArchitectureTest` 의 7건 포함). 클래스 필터가 ArchUnit JUnit 엔진에는 그대로 안 먹는 모양인데, 이번엔 "8건 통과"가 완료 기준이라 오히려 편했다. 건수를 셀 때 놀라지 말 것.

### `/implement` 가 테스트 파일을 만졌다 — 이 Phase 한정

`/implement` 는 테스트 파일을 고치지 않는 것이 안전장치인데, Phase 0 의 산출물이 **구조 규칙 파일 자체**다. 계획서가 이 Phase 를 `/implement` 에 명시 배정했고, 검증 계약도 "심었다 지우는 프로브 · `✅ 수동`"으로 잡혀 있어 단언을 약화시킬 케이스가 없다. 구조 규칙은 검증 계약이 아니라 **구현물**로 취급했다. 다른 Phase 에는 적용하지 않는다.

### §13 역반영을 같은 날 끝내고 체크를 켰다

`/checkpoint` 시점엔 완료 기준 마지막 항목 "「소스 구조」 §13 ArchUnit 스케치 역반영"이 남아 `- [ ]` 로 두었다가, 승인 후 Notion 을 고치고(callout 1개 + 스케치 ① 의 `ignoreDependency` 7건 — REQ-08 의 `framework`·`auth→user` 도 스케치엔 빠져 있어 함께 맞췄다) `fetch` 로 저장 확인한 뒤 켰다. **문서가 코드보다 앞서지 않도록 코드 머지 → 역반영 순서**를 지켰다. "고치기 전 규칙에서도 FAIL 인지 대조"는 CLAUDE.md 로컬 검증 절에 승격했다.

### 자율 실행 메모

- 브랜치는 `origin/main` 에서 분기했다 — 어제 docs 브랜치 위에 스택하면 AGENTS §4 의 base 함정을 밟는다. 두 PR 모두 base `main` · SHA 대조 일치 · 머지 후 `origin/main..` 빈 것 확인
- 이 머신에는 `lefthook` 이 없어 pre-commit 훅이 안 걸린다 — 게이트(spotless · build · checkstyle `-PciStrict` · test 114건)를 직접 돌렸다

**남은 것 (REQ-10)** — ~~Phase 1 착수 전 미결 2건~~ → 같은 날 오후 아래에서 처리.

### 미결 2건을 Notion 에 먼저 쓰고 Phase 1 을 끝까지 돌렸다 (오후)

체중 경고 형태는 사람이 정했다 — `weight_change_rate`(직전 대비 %, 소수 1자리, 첫 기록 `null`) · `is_weight_warning`(`|변화율| >= 20`, 첫 기록 `false`) · 직전 = `measured_at` desc, `id` desc 의 바로 다음 1건. **원본 행 5개에 먼저 쓰고**(「체중 목록」 파생 필드 절 · 「체중 기록」 201 형태 · 활동/체중/탈피 목록 `Query Parameters`) 레포로 옮겼다(PR #38). 그 뒤 `/testgen` → `/implement` → `/testrun` 을 한 세션에 돌려 weight 4행이 들어갔다(`acde9ab`).

- **"직전"의 정의에서 `id` 타이브레이크가 파생 필드까지 규정한다.** `measured_at` 이 `date` 라 같은 날 2건이면 시각 비교가 불가능하다 — 커서 정렬 키를 그대로 "직전" 정의에 썼다. 목록에서는 정렬상 다음 항목이 곧 직전이라 **추가 조회 없이** 계산되고, 마지막 항목만 `limit+1` 번째 행(있으면) 또는 1건 추가 조회로 갚는다
- **REQ-10-10(keyset 누락·중복 없음)은 DB 없이 필요조건만 고정했다.** 이 레포에 H2·Testcontainers 가 없다. `next_cursor` 에 `id` 가 실리는 것 + 다음 페이지 조회가 두 키를 다 넘기는 것을 목으로 고정하고, 실제 경계는 로컬 DB 수동 확인으로 남겼다. **이 세션 머신에는 `.env` 도 Postgres 도 없어 그 확인을 못 했다** — `@Query` 의 `w.id < :id`(UUID 비교)가 Hibernate 6 HQL 을 통과하는지도 기동해 봐야 안다. `@WebMvcTest` 는 JPA 를 안 띄우므로 32건 초록불이 이걸 보장하지 않는다. Phase 1 체크를 켜지 않은 이유
- **`WeightCursor` 는 `business/weight/service` 에 뒀다.** `data/weight/dto` 에 두면 ArchUnit `DTO_NAMING`(`*Request`/`*Response`)에 걸리고, 클라이언트에 노출되는 형태도 아니다(opaque). 다음 도메인도 같은 자리
- **`/testrun` 의 인용 검사가 브랜치 상태에 속을 수 있다.** 코드 브랜치(`origin/main` 분기)에서 돌리자 인용 4건이 1건(표 행 자체)만 매칭됐다 — 원문은 아직 안 머지된 문서 PR(#38)에 있었다. 스펙 변경이 아니라 **문서 브랜치와 코드 브랜치가 갈라진 것**. 검사는 두 브랜치를 합친 상태에서 읽어야 한다
- `grep` 이 `ugrep` 별칭이면 `-c` 가 빈 출력을 낸다 — 세는 건 `| wc -l` 로. 0건이 아니라 **빈 문자열**로 나와 "0건"으로 오독하기 쉽다

~~**남은 것 (REQ-10)** — 로컬 DB 확인 2건(→ Phase 1 체크) · Phase 2 착수 전 미결 1건(게코가 `distance_km` 를 보내면).~~ → 아래.

### Phase 2 (activity) — 복제가 실제로 됐다, 결정 하나(D13)로 (저녁)

`distance_km` 는 **거부하지 않고 그대로 저장**(D13). "게코 미사용"·"실내 활동은 NULL"은 입력 UI 규약이지 서버 거부 규약이 아니고, 원본에 없는 거부 규약을 만들지 않는다(REQ-09 D5 와 같은 결). 서버가 거부하는 것은 종별 `activity_type` 뿐 — `ActivityType.isAllowedFor(Species)` 한 줄(게코 = `HANDLING` 만, 나머지 종 = `HANDLING` 제외)이 규칙 전부다. Notion 「활동 기록」 행에 먼저 적었다.

- **weight 형태가 그대로 복제됐다.** 엔티티·저장소·DTO·커서·서비스·컨트롤러 8파일이 이름만 다르다. 차이는 종 검증 3줄과 `logged_at` 이 `LocalDateTime` 인 것뿐. **PATCH 로 유형이 바뀔 때도 종 검증을 다시 거는 것**(REQ-10-29)이 복제에서 빠뜨리기 가장 쉬운 자리라 케이스로 고정했다
- **Phase 1 의 공통 케이스(가드 위임·D6·커서·PATCH 병합)를 Phase 2 에서 다시 썼다.** 같은 코드 형태라도 도메인마다 한 번씩 고정한다 — 복제 과정의 누락이 이 REQ 의 주 실패 모드다
- 문서와 코드를 **한 브랜치**에 커밋 분리로 올렸다(`docs:` 2개 · `feat:` 1개). Phase 1 에서 문서/코드 브랜치가 갈려 `/testrun` 인용 검사가 1건으로 보이던 문제가 없어진다. 커밋 주체 분리(파일 집합)는 그대로다
- 응답의 `logged_at` 은 `Z` 없이 나간다(`2026-06-30T09:00:00`) — Jackson 에 시각 포맷 설정이 없어 기존 엔티티(`created_at`)와 같다. D12 가 "응답은 ISO-8601 `Z`"라 적었지만 framework 전역 사항이라 이 Phase 에서 손대지 않았다. **REQ-10 미결로 올린다** — 요청 `…Z` 는 jsr310 기본(lenient)이 UTC 로 읽어 문제없다

**남은 것 (REQ-10)** — 로컬 DB 확인 2건(→ Phase 1·2 체크) · 응답 시각 `Z` 표기(미결) · Phase 3(feeding) 착수 전 미결 4건(`fed_at` "당일" · 스트릭 규칙 · 게코 외 종의 스트릭 · `food_size` 처리 — 마지막은 D13 과 같은 결이 자연스럽다).

### 시각 처리를 REQ-16 으로 떼어냈다 — 결정이 세 번 뒤집혔다 (밤)

Phase 2 에서 올린 "응답 시각 `Z`" 미결이 framework 전역 결정으로 번져 별도 REQ 가 됐다. **기록할 것은 결론이 아니라 뒤집힌 경로다** — 결론만 보면 왜 이 형식인지 알 수 없다.

1. "날짜는 전부 KST" → 처음엔 **계산만 KST · 저장은 UTC 유지**(현행 유지, 비용 0)로 답이 나왔다
2. 곧바로 **계정별 타임존**(기본 KST)으로 확대 — `users.timezone` · `PATCH /users/me` 확장 · 요청 스코프로 TZ 를 나르는 framework 포트가 따라오는 별건이라 REQ 를 새로 잡아야 했다
3. 다시 **KST 고정으로 축소**되면서 "저장은 KST 가 나은가 UTC 가 나은가"라는 원래 질문으로 돌아왔고, **`timestamptz`**(순간 저장)로 확정됐다 → [ADR-0002](adr/ADR-0002-time-handling-timestamptz.md)

**저장을 KST 로 바꾸는 안이 왜 기각됐는지가 이 REQ 의 핵심이다.** 변환이 없어 깔끔해 보이지만 "JVM 기본 TZ = KST" · "DB 세션 TZ = KST"라는 **암묵 전제** 위에 서고, 컨테이너에 `TZ` 를 안 넣거나 Supabase 세션 TZ(UTC)를 만나면 **에러 없이** 9시간 어긋난다. `.env` 의 빈 값이 기본값을 무력화한 건 · `db.schema` 를 한쪽만 배선한 건과 **같은 얼굴**이다. 반대로 `timestamptz` 는 **타입이 규약을 대신 기억한다** — "이 값이 어느 타임존인가"를 사람이 알 필요가 없어진다.

> **계정별 타임존은 기각이 아니라 연기다.** `timestamptz` 는 이미 순간을 저장하므로 나중에 열어도 **재마이그레이션이 필요 없다**(경계의 변환 대상만 KST 고정에서 사용자 값으로 바꾸면 된다). ADR-0002 의 재검토 조건에 그렇게 적었고, 이게 채택 근거의 절반이다. 나머지 절반은 "배포 전인 지금이 전환 비용이 가장 싸다"는 것.

- ⚠️ **컬럼을 두 번 셌다 — 처음 보고한 "17개"가 틀렸고 실제는 19개**(V1 16 · V2 3). `grep -c 'timestamp '` 가 줄 끝 `deleted_at timestamp`(뒤에 공백 없음) 2건과 `revoked_at timestamp,`(쉼표) 1건을 놓쳤다. **패턴 끝에 공백을 붙여 세면 컬럼 정의가 조용히 빠진다** — 0건이 아니라 "그럴듯하게 적은 수"로 나와서 더 위험하다
- **`V3` 를 REQ-16 이 가져간다** → REQ-10 Phase 3 의 `feeding_logs.food_size` 는 **`V4`** 다. 적용된 마이그레이션은 한 글자도 못 고치므로 번호를 먼저 확정했다
- ⚠️ **07-30 픽스처 규칙(`now() at time zone 'UTC'`)은 전환과 함께 폐기해야 한다.** `timestamptz` 에서 `now()` 는 세션 TZ 와 무관하게 올바른 순간을 반환하므로, 규칙을 남겨 두면 이번엔 **반대 방향으로** 9시간 어긋난다. 함정이 사라지는 게 아니라 **뒤집힌다**
- Phase 0(프로브)의 미결 ①②③ 중 **③(Jackson 이 `+09:00` 을 내는 설정)과 ① 일부는 DB 없이 닫힌다** — `ObjectMapper` 왕복만으로 확인된다. DB 가 필요한 것은 `timestamptz` 왕복 정확성과 `hibernate.jdbc.time_zone` 의 영향뿐이다
- `date` 컬럼 5개(`entry_date`·`measured_at`·`shed_date`·`birthday`·`adoption_date`)는 **타입을 바꾸지 않는다.** 날짜만 있는 값에는 타임존이 없고, 바꾸면 커서 정렬(REQ-10 D8)과 체중 파생 필드 정의(D3)가 전부 흔들린다

### REQ-10 Phase 3 미결 4건 — 답은 나왔고 Notion 역반영이 남았다

네 건 모두 "원본에 없는 거부 규약을 만들지 않는다"(REQ-09 D5 · D13)와 같은 결로 정해졌다.

| 미결 | 결정 |
|---|---|
| `fed_at` "당일 시간만 허용" | **미래 시각만 거부** — 서버 현재 시각 이전이면 과거 소급 허용. 순간 비교라 타임존 논쟁이 사라진다("오늘 날짜만" 안은 자정 직후 입력과 어제 급여 소급을 막는다) |
| 거식 스트릭 | **일수 기준** — 마지막 `is_refused = false` 급여(`last_eaten_at`)부터 기준 시각까지의 KST 달력 일수. `>= 7` DANGER · `>= 3` CAUTION · 나머지 NONE. 게코는 며칠에 한 번 먹는 게 정상이라 "연속 거식 **건수**" 안은 "7일"과 맞지 않는다 |
| 게코 외 종의 스트릭 호출 | **`FEATURE_NOT_SUPPORTED_SPECIES` 신설** — 게코 전용 기능 공통 코드. 문구가 탈피 전용인 `SHED_NOT_SUPPORTED_SPECIES` 는 아직 소비자가 없으므로 제거하고 shed 도 이 코드로 간다 |
| 개/고양이의 `food_size` | **그대로 저장** (D13 과 동일). 거부하는 것은 enum 밖의 값뿐 |

> ⚠️ **넷 다 아직 Notion 에 안 적었다.** 이 프로젝트의 원칙은 "원본에 먼저 쓰고 레포로 옮긴다"인데 시각 논의로 갈라지면서 순서가 끊겼다. 계획서에는 결론을 `[x]` 로 남기되 **역반영 대기**를 명시했다 — **Phase 3 착수 전에 Notion 부터 고칠 것.**

> **원본 자기모순 1건을 찾았다.** 거식 스트릭 응답 예시가 `current_streak_days: 5` 인데 `level: "DANGER"` 다. 같은 행의 규칙은 `DANGER (7일+)` 이므로 5일이면 `CAUTION` 이어야 한다. 예시를 고치거나 일수를 8 로 바꿔야 하고, **어느 쪽이든 사람이 정할 일**이라 역반영 목록에 올렸다.

**남은 것 (REQ-10)** — 로컬 DB 확인 2건(→ Phase 1·2 체크) · Notion 역반영 4건(Phase 3 결정) + 스트릭 예시 모순 · Phase 3 이후는 **REQ-16 완료 뒤**.

### 로컬 Postgres 를 이 머신에 세웠다 — 미뤄 둔 확인 2건 중 1건이 닫혔다

Docker 로 `postgres:17`(실제 17.11, 운영 Supabase 와 메이저 일치)을 포트 **5433** 에 띄우고 컨테이너 TZ 를 UTC 로 고정했다(Supabase 세션 TZ 와 같은 조건 — REQ-16 프로브의 전제다). Flyway 가 V1·V2 를 `petkok_local` 에 적용해 테이블 11개가 생겼다. 접속 정보와 함정은 `CLAUDE.local.md`(gitignore 대상, 새로 만들면서 `.gitignore` 에 먼저 추가했다 — 무시 대상이 **아니었다**)에 적었다. **비밀번호는 `.env` 한 곳에만 둔다.**

이걸로 REQ-10 Phase 1·2 의 보류 2건 중 **① `@Query` JPQL 기동 검증이 닫혔다.** 다만 "기동 성공 = JPQL 검증됨"은 추정이라 **일부러 깨서 확인**했다 — `w.measuredAt` → `w.measuredAtXX` 로 바꾸자 `UnknownPathException: Could not resolve attribute` 로 **기동 자체가 막혔다.** 즉 초록 기동은 실제 파싱 통과다. ② keyset 경계 수동 확인은 인증 토큰·펫 생성이 선행이라 아직 남아 있다.

> ⚠️ **`./gradlew test` 는 여전히 DB 를 쓰지 않는다.** Testcontainers 가 없어 keyset 경계 같은 것은 수동 확인이다. **DB 가 생겼다고 테스트 커버리지가 넓어진 것이 아니다.**

### `.env.example` 을 그대로 `cp` 하면 기동이 막혔다 — 파일 자신이 경고하는 함정을 본문이 밟고 있었다

`R2_ENDPOINT=` 같은 **빈 값**이 환경변수로 들어가 `application.yml` 의 더미 기본값을 무력화했고 `endpointOverride must not be null` 로 죽었다. `.env.example` 헤더에 "`KEY=`(빈 값)은 미설정이 아니다"라고 **이미 적혀 있는데** 본문 6줄이 그 형태였다(`ff24bfc`). 기본값이 있는 6개를 `#` 로 막고, **고친 템플릿을 다시 `cp` 해 `DB_PASSWORD` 한 줄만 채우고 기동되는 것까지 확인**했다. `DB_PASSWORD` 만 빈 값으로 남긴다 — 어디에도 기본값이 없어 어느 형태든 실패이고 "채워야 할 칸"으로 보이는 편이 낫다. 그 판단 근거를 파일 안에 적었다.

> 헤더의 실행 안내(`set -a && . ./.env`, IntelliJ, direnv)는 **낡았다.** `application.yml` 에 `spring.config.import: optional:file:.env[.properties]` 가 있어 `./gradlew bootRun` 만으로 읽힌다(오늘 두 번 기동해 확인). `CLAUDE.md` 로컬 검증 절도 같은 낡은 명령을 담고 있다 — **다음 세션에서 고칠 것.**

### REQ-16 Phase 0 — 미결 ①②③ 을 실측으로 닫았다

프로브 3건을 `req16_probe` 스키마(**Flyway 소유 밖** — `petkok_local` 에 수동 DDL 을 치면 AGENTS §5 를 어긴다)에 임시 표를 만들어 돌리고, 끝나고 코드·스키마를 지웠다.

| 미결 | 실측 결과 | 결정 |
|---|---|---|
| ① 엔티티 시각 타입 | `Instant` 는 **네 설정 전부 `Z`** 로 나가 D3(`+09:00`)와 충돌 → 탈락. `OffsetDateTime`·`ZonedDateTime` 은 거동·변경량이 **완전히 같다** | **`OffsetDateTime`** — `timestamptz` 는 오프셋만 보존하고 zone id 는 잃는다. `ZonedDateTime` 은 저장되지 않는 정보를 담는 척한다 |
| ② `hibernate.jdbc.time_zone` | `timestamptz` 3컬럼은 `UTC`/`Asia/Seoul` 에서 **결과가 완전히 같다**(무영향). 유일하게 작동하는 곳은 `timestamp`+`LocalDateTime` 쌍 — `UTC` 면 `09:00`, `Asia/Seoul` 이면 `18:00` 저장 | **유지.** 지우면 훗날 누가 `timestamp` 컬럼을 추가했을 때 JVM 기본 TZ 의존이 되살아난다. 주석의 근거만 "앱이 UTC 로 쓴다" → "남을지 모를 `timestamp` 컬럼을 JVM TZ 에서 떼어 놓는 안전망"으로 바꾼다 |
| ③ Jackson 설정 | `ObjectMapper.setTimeZone(Asia/Seoul)` **한 줄이면 된다.** `WRITE_DATES_AS_TIMESTAMPS`·`ADJUST_DATES_TO_CONTEXT_TIME_ZONE` 은 손댈 필요 없고, `WRITE_DATES_WITH_CONTEXT_TIME_ZONE` 은 **켜 둬야 한다**(끄면 `Z` 로 돌아간다) | 그대로 Phase 2 에서 적용 |

> ⚠️ **③ 은 케이스 문구대로 재지 못했다.** 계획서는 `hibernate.jdbc.time_zone` 을 "켠 상태와 **끈 상태**"로 대조하라고 했는데, Spring 에서 이 프로퍼티를 깨끗하게 "없음"으로 만들 방법이 없어(`=` 빈 값은 Hibernate 가 `GMT` 로 읽는다) `UTC` vs `Asia/Seoul` 두 값으로 갈음했다. `timestamptz` 에서 **결과가 완전히 같다**는 답은 그대로 성립하지만 **진짜 미설정 상태는 재지 않았다.**

부수로 **D6 의 전제가 실측으로 확인됐다** — KST 벽시계 `18:00` 을 넣으면 현행 설정에서 `09:00`(UTC)이 저장된다. 앱이 UTC 로 써 왔다는 뜻이고 `USING … AT TIME ZONE 'UTC'` 가 맞다.

또 하나 — `ObjectMapper` 의 TZ 는 `UTC`, `hasExplicitTimeZone = false` 다. **JVM 기본 TZ 를 따라가지 않는다.** 명시하지 않으면 어디서 돌든 `Z`, 명시하면 그 값. 이 프로젝트가 반복해 밟은 "환경에 따라 조용히 갈리는" 형태가 **여기엔 없다.**

### ⭐ 프로브 입력이 결과를 미리 정해 버렸다 — 네 설정이 전부 같은 답을 냈다

첫 프로브는 입력값에 **이미 `+09:00` 을 달아** 두었다. 그러자 네 설정이 모두 `+09:00` 을 냈고, 그대로 읽었으면 **"Jackson 설정이 필요 없다"는 정반대 결론**이 나왔을 것이다. 실제 응답 경로는 그게 아니다 — `timestamptz` 는 원래 오프셋을 저장하지 않아 **DB 에서 읽으면 항상 `Z`** 다. 그 모양으로 다시 재자 ⓐ·ⓒ 는 `Z`, ⓑ 만 `+09:00` 으로 갈렸다.

여기에 대조군으로 `America/New_York` 을 하나 더 넣었다 — `-04:00` 이 나와야 "설정이 실제로 작동한다"가 성립한다. **역프로브 없이는 ⓑ 의 초록불이 우연인지 알 수 없다.** 구조 규칙에 일부러 위반을 심는 것과 같은 자리다.

> 교훈은 "프로브를 돌렸다"가 아니라 **"프로브의 입력이 답을 가르는가"** 를 먼저 봐야 한다는 것이다. 모든 후보가 같은 답을 내면 그건 합의가 아니라 **측정 실패**일 수 있다.

### 조용한 무동작 3건 — 전부 에러 없이 그럴듯한 결과를 냈다

| 무엇이 | 어떻게 보였나 |
|---|---|
| `docker exec` 에 `-i` 누락 | heredoc 이 전달되지 않아 **테이블이 안 만들어졌는데** 명령은 성공. 컬럼 확인이 0줄인 것으로 잡았다 |
| BSD `sed` 의 `0,/re/` 미지원 | GNU 용법이라 **치환이 안 됐고**, 원본 코드가 정상 기동한 것을 "프로브 통과"로 읽을 뻔했다 |
| `grep -F ""` (빈 패턴) | 주석 처리된 `JWT_SECRET` 을 뽑아 변수가 비었고, 시크릿 유출 검사가 **168건**을 보고했다(실제 0건). 세 번째만 `CLAUDE.md` 계약으로 승격했다(`d872d45`) — 나머지 둘은 머신 종속이라 `CLAUDE.local.md` 에 넣었다 |

### 관찰 — Lombok 이 테스트 소스에 없다

`build.gradle.kts` 에 `compileOnly`/`annotationProcessor` 만 있고 `testCompileOnly`/`testAnnotationProcessor` 가 없다. 프로브 엔티티에 `@Getter` 를 붙였다가 컴파일이 깨져 손으로 접근자를 썼다. **REQ-16 Phase 1·2 에서 시각 타입을 다루는 테스트를 쓸 때 다시 걸린다** — 의존성 추가는 제안·승인 대상이라 손대지 않았다.

**남은 것 (REQ-16)** — Phase 1(`V3` 19컬럼 + 엔티티 6필드) 착수 가능. `D5` 의 `Clock` zone 과 "오프셋 없는 요청 값의 동작"은 아직 미결이다.

### REQ-16 Phase 1 — `V3` 19컬럼 + 엔티티·DTO 를 `OffsetDateTime` 으로

케이스 6건(04·05·06·07·13·14) 전부 통과 · 전건 174/0. `petkok_local` 은 v3 이고 `timestamptz` 19개다.

**적용 전에 버리는 DB 로 예행했다.** 적용된 마이그레이션은 한 글자도 못 고치므로(`CLAUDE.md`) `v3check` 데이터베이스를 만들어 V1~V3 를 처음부터 돌려 보고, 컬럼 타입과 기동을 확인한 뒤에야 `petkok_local` 에 넣었다. DB 를 갈아엎을 수 있는 지금이 이 예행의 값이 가장 싼 시점이다.

계획서가 열거하지 않았지만 **타입이 묶여 있어 함께 바뀐 곳이 3개** 나왔다 — `ActivityCursor`(keyset 페이로드) · 두 Repository 의 `@Query` 파라미터 · `JwtTokenProvider.getExpiresAt`. 마지막 것은 `refresh_tokens.expires_at` 을 채우는 값이라 따라올 수밖에 없고, 이 참에 `ZoneId.systemDefault()` 를 `ZoneOffset.UTC` 로 바꿨다 — **순간은 그대로이고 JVM TZ 의존만 사라진다.**

> **테스트 7파일은 `/implement` 가 고쳤다 — 사용자 승인을 받고서다.** 타입을 바꾸면 컴파일이 통째로 깨져 `/testrun` 이 실행조차 못 하기 때문이다(REQ-10 Phase 0 의 "이 Phase 한정"과 같은 자리). 범위는 **기계적 타입 치환만**으로 못박았고, 단언 문구·기대값은 건드리지 않았다. 애초에 **시각 값을 단언하는 케이스가 없다는 것을 먼저 확인**하고 들어갔다 — `created_at` 은 `exists()` 만 보고, `logged_at` 은 요청 본문에만 나온다. 있었다면 그건 Phase 2 영역이라 손대면 안 됐다.

### ⚠️ `ddl-auto: validate` 는 타입을 보지 않는다 — 계획서 제약이 틀렸다

계획서 「제약·함정」이 *"`ddl-auto: validate` 는 타입까지 본다 … 기동 시점에 터진다. 이건 좋은 실패다"* 라고 적어 두었고, **Phase 1 완료 기준의 "엔티티 ↔ 스키마 대조"가 이 문장 위에 서 있었다.** 실측은 반대다.

- 엔티티를 `OffsetDateTime` 으로 두고 컬럼을 `timestamp` 로 남긴 채(`SPRING_FLYWAY_TARGET=2` 로 Flyway 를 V2 에서 정지) 기동 → **그대로 떴다**
- 검사기 자체는 살아 있다 — `users.email` 을 지우자 `Schema-validation: missing column [email] in table [users]` 로 막혔다

즉 **컬럼 존재만 보고 타입은 안 본다.** 엔티티만 바꾸고 마이그레이션을 빠뜨리면(또는 그 반대) **조용히 통과한다.** 이 프로젝트가 반복해 밟은 얼굴이 하나 더 늘었다.

그래서 이번 Phase 의 실제 방어선은 `validate` 가 아니라 **REQ-16-04·05(마이그레이션 텍스트 검사)** 다. 다만 그건 *SQL 에 19개가 적혀 있는가* 를 볼 뿐 **DB 의 실제 컬럼 타입과 엔티티를 맞대보지는 않는다.** 그 구멍을 메우려면 DB 를 조회하는 케이스가 필요한데, **DB 가 있는 환경에서만 돌아 CI 에서 깨진다** — 미결로 올렸다(⑥).

> 계획서 제약 문장은 **실측대로 고쳤다.** 조용히 고친 게 아니라 여기 남기는 이유는, 저 문장이 완료 기준의 근거였기 때문이다. "계획과 실제가 어긋났으면 그게 가장 중요한 기록"이라는 규칙이 정확히 이 경우다.

### 계획서 열거가 셋 어긋났다 — 전부 동작은 옳고 문서만 틀렸다

| 계획서 | 실제 | 영향 |
|---|---|---|
| `date` 컬럼 **5개** | **6개** — `photos.taken_at` 이 빠졌다 | 동작은 옳다(안 건드림). REQ-16-07 이 "5개 중 3개"가 아니라 **"6개 중 3개"** 를 덮는다 |
| `now()` 호출부 **`AuthService` 2곳** | **3곳** — `BaseSoftDeleteEntity.softDelete()` | 이번엔 `OffsetDateTime.now()` 로만 바꿨다. `Clock` 주입은 Phase 3 이므로 넘기지 않았다 |
| — | `framework/util/date/LocalDateTimeUtil` 에 `LocalDateTime.now()` **2건** | ⭐ **REQ-16-10("`business`·`framework` 에 0건")과 정면 충돌.** 이식한 범용 유틸이라 없앨 수 없다 — Phase 3 착수 전에 예외를 둘지 정해야 한다 |

세 번째가 특히 중요하다. **Phase 3 완료 기준이 지금 상태로는 달성 불가능**하고, 그걸 모른 채 Phase 3 을 시작하면 유틸을 뜯어고치거나 케이스를 몰래 약화시키게 된다.

### 프로브가 두 번 무효였다 — 둘 다 "통과"로 보였다

`validate` 가 타입을 보는지 재려고 엔티티 한 필드만 `LocalDateTime` 으로 되돌렸더니 **컴파일이 깨졌다**(DTO 가 같은 타입으로 묶여 있다). 결과 grep 패턴에 `BUILD FAILED` 를 안 넣어 두어 **아무 줄도 출력되지 않았고**, 하마터면 "실패 신호 없음 = 통과"로 읽을 뻔했다. 방향을 바꿔 **DB 쪽을 V2 에 멈추는** 방식으로 다시 쟀다.

> 교훈: **프로브가 무효였음을 알려 주는 것은 출력의 존재이지 부재가 아니다.** 결과 grep 은 성공 패턴과 실패 패턴을 **둘 다** 넣어야 하고, 어느 쪽도 안 걸리면 그건 "판정 불가"이지 통과가 아니다. 오늘 세 번째로 밟은 조용한 무동작이다.

`REQ-16-14` 는 처음부터 초록이라 **성립 조건을 따로 쟀다** — `application.yml` 에서 `time_zone: UTC` 를 지우니 1건 실행 · 1건 실패. 회귀 방어 케이스는 이 역프로브 없이는 "지워도 통과하는 테스트"와 구별되지 않는다.

### REQ-16 Phase 2 — 응답은 `+09:00`, 오프셋 없는 요청은 KST

케이스 3건(08·09·15) 통과 · 전건 177/0.

**계획서는 이 Phase 를 "`JacksonConfig` 한 곳"으로 잡았는데 두 곳이 됐다.** 계획이 틀린 게 아니라 **D9 가 Phase 2 착수 직전에 추가되면서** 범위가 늘어난 것이다 — "오프셋 없는 요청 값의 동작"이 Phase 0 에서 정해졌어야 했는데 프로브가 그걸 재지 않아, Phase 2 를 시작하기 직전 대화에서 "오프셋 없으면 KST" 로 정했다.

**⭐ 설정 한 줄로는 절반만 된다.** Phase 0 이 고른 `setTimeZone(Asia/Seoul)` 은 **직렬화**의 렌더 기준일 뿐이다. 오프셋 없는 입력은 **Jackson 이 아예 거부해** 컨트롤러에 도달조차 못 하고 400 이 된다(실측 — 서비스 호출 0회, 목이 "zero interactions" 로 신고했다). `ISO_OFFSET_DATE_TIME` 이 오프셋을 필수로 요구하기 때문이라, `ISO_DATE_TIME` + `parseBest` 로 읽고 오프셋이 없을 때만 KST 를 채우는 `OffsetDateTimeDeserializer` 를 `processor/converter` 에 추가했다(AGENTS §3 이 이미 자리를 잡아 둔 패키지다 — 새 관례가 아니다).

> **응답 쪽과 요청 쪽은 서로 다른 두 장치다.** 한쪽만 보고 "설정 하나로 끝났다"고 읽으면 다른 쪽이 조용히 400 이 된다. 양쪽 javadoc 에 서로를 가리키는 경고를 남겼다.

**09 는 이 커밋 전에도 통과했다.** 회귀 방어 케이스라 정상이지만, 그대로 두면 "지워도 통과하는 테스트"와 구별되지 않는다. 성립 조건을 따로 쟀다 — `parseBest` 의 후보 순서를 뒤집어 `LocalDateTime` 을 먼저 시도하게 하면 `Z` 와 `+09:00` 이 **다른 순간**이 되어 빨간불이 난다. 실수로 충분히 일어날 만한 형태를 골랐다.

**`JacksonConfig` 는 전역이라 전건을 돌려야 한다.** `@WebMvcTest` 슬라이스 전부가 이것을 `@Import` 하고 응답 시각 표기가 모든 도메인에서 바뀐다. 깨진 것은 없었는데, 이유는 **시각 값을 단언하는 기존 케이스가 애초에 없어서**다(`created_at` 은 `exists()` 만 본다). 그래서 Phase 2 서술의 "기존 REQ 컨트롤러 테스트 갱신"은 **안 한 게 아니라 할 것이 없었다** — Phase 1 의 기계적 타입 변경으로 이미 끝나 있었다.

> ⚠️ **`Asia/Seoul` 이 지금 두 파일에 하드코딩돼 있다** (`JacksonConfig` · `OffsetDateTimeDeserializer`). Phase 3 의 REQ-16-11 이 `framework/constant` 상수로 합치는 케이스인데, **한 곳만 바꾸면 조용히 갈린다.** 양쪽 javadoc 에 "Phase 3 에서 옮긴다 · 늘리지 말 것"을 적어 두었다.

### REQ-16 미결 6건 정리 — Phase 3 을 막던 것이 풀렸다

**Phase 3 은 지금까지 달성 불가능한 상태였다.** 완료 기준이 "`LocalDateTime.now()` 직접 호출 0건"인데 `framework/util/date/LocalDateTimeUtil` 에 2건이 있었기 때문이다. 이걸 안 풀고 시작했으면 유틸을 뜯어고치거나 케이스를 몰래 약화시키게 된다.

**결정을 바꾼 사실 하나 — 그 유틸은 사용처가 0건이다.** 그래서 문제가 "쓰고 있는 코드를 어떻게 하나"가 아니라 "안 쓰는 코드가 규칙을 막고 있다"로 바뀌었고, 세 안(규칙에서 제외 / 두 메서드 수정 / 파일 삭제) 중 **수정**을 골랐다(D10).

> ⭐ **`framework.util` 을 규칙에서 제외하는 안을 버린 이유가 이 결정의 핵심이다.** 예외를 두면 **우회 경로가 열린 채 남는다** — 누가 `LocalDateTimeUtil.isNowBetween(...)` 을 부르면 그 호출부는 `now()` 를 **직접** 부르지 않으므로 규칙이 못 잡는다. 규칙이 "직접 호출"만 보는 형태라 한 겹 감싸면 통과한다. 파일 삭제는 30개 이식 유틸 중 하나를 "지금 안 쓴다"는 이유로 버리는 것이라 과했다.

**`Clock` 의 zone 은 `Asia/Seoul`.** zone 은 **저장에 영향을 주지 않는다**(순간은 동일). 갈리는 곳은 벽시계 파생 하나뿐인데 — `LocalDate.now(clock)` · `LocalDateTime.now(clock)` — 그게 정확히 D4(달력 판정 = KST)의 자리다. UTC 로 두면 KST 00:00\~09:00 에 "어제"가 **에러 없이** 나온다.

**미결 ⑥(엔티티↔DB 타입 대조)은 계약으로만 남긴다.** `범위 — 제외` 에 이미 *"`timestamptz` 를 쓰지 않는 신규 컬럼 금지 규칙의 자동 강제 — ArchUnit 은 SQL 을 보지 않는다. 계약으로만 남긴다"* 가 있었다. **뿌리가 같은 결정**이라 따로 정할 것이 아니었다 — SQL 과 코드를 맞대볼 수단이 없다는 하나의 사실이 두 얼굴로 나타난 것이다. Testcontainers 를 도입하면 REQ-10 의 keyset 경계·`@Transactional` 롤백까지 함께 닫히므로 **별건으로 묶어 그때 한꺼번에** 다룬다.

### ⭐ Notion `API I/F` 40행 전수 조사 — "전부 바꾼다"가 틀렸다

미결 ④ 는 정할 성격이 아니라 **셀 성격**이라 40행을 전부 열어 봤다. **시각 예시가 있는 행은 14개**(리터럴 20개)다. 나머지 26행은 시각 예시가 아예 없다 — DELETE 10건, 응답을 "…객체"로만 적은 행, `date` 필드만 쓰는 행.

**여기서 나온 것이 조사의 값이다 — 요청 예시 2행(급여 기록 `fed_at` · 활동 기록 `logged_at`)은 고칠 의무가 없다.** D9 가 `Z`·`+09:00`·오프셋 없음을 **모두 받으므로** 요청의 `Z` 는 여전히 유효하다. D3(응답 `+09:00`)에 걸리는 것은 **응답 12행뿐**이다.

> "`…Z` 를 전부 `+09:00` 으로 바꾼다"고 뭉뚱그렸으면 **계약이 아닌 것을 계약으로 만들 뻔했다.** 세어 보지 않았으면 그 차이가 보이지 않는다 — 미결 ④ 를 "조사"로 분류한 이유가 이것이다.

`date` 필드 7종(`measured_at`·`shed_date`·`entry_date`·`taken_at`·`birthday`·`adoption_date`·`predicted_date`)은 전부 `"2026-06-30"` 형태로 **균일**했다. 손댈 것이 없다.

**「소스 구조」 §6 에 시각 규약 절이 없다** — 신설이 필요하다. 함께 갱신할 곳도 확인했다: §5 표의 `JacksonConfig` 행(지금은 "SNAKE_CASE 전역 적용"만), §2 패키지 트리(`processor/converter/ ⏸ (예정)` 이 실재하게 됐고 `db/migration` 이 `V1·V2` 까지만 적혀 있다). 레포 파생 요약은 `docs/specs/api-list.md` 한 줄뿐이고 `db-schema.md` 는 0건.

**Phase 4 는 이제 목록이 곧 작업 지시서다.** 조사 결과를 계획서 미결 ④ 항목에 그대로 적어 두었다.

## 2026-08-27

> 17일 만의 재개. REQ-09 Phase 3(`PetAccessGuard`)으로 REQ-09 를 닫았다. 코드 자체는 작았고(가드 1 + DTO 1), 기록할 것은 **순서가 꼬였던 것**과 **셈이 틀릴 뻔한 것**이다.

### 커맨드 순서가 한 번 꼬였다 — 표는 있는데 코드가 없는 케이스

`/implement REQ-09 Phase 3` 로 들어갔더니 검증 계약 표에는 REQ-09-09~13 이 있는데 **테스트 코드가 없었다.** 08-10 `/testgen` 이 표는 채우고 코드는 "대상 클래스가 생기는 Phase 에 쓴다"(REQ-08 에서 정한 관례 — Java 는 대상이 없으면 테스트 소스가 컴파일되지 않는다)로 미뤄 둔 것이다. `/implement` 3절 게이트("Phase 에 걸린 케이스 0건 = 판정 불가")에 정확히 걸렸다.

그래서 **구현 → `wip` 커밋(푸시 안 함) → `/testgen` 으로 5건 코드화 → `/implement` 재진입 → wip 을 amend 해 `feat` 하나로 → 푸시** 순서로 갔다. 커밋은 결국 하나(`137dec6`)라 "Phase 1개 = 커밋 1개"는 지켜졌다.

> 이 관례(코드는 Phase 별로)는 옳은데, **`/testgen` 을 Phase 착수 직전에 한 번 더 도는 것이 정상 경로**라는 게 이번에 드러났다. 표를 채운 세션과 코드를 쓰는 세션이 다르면 이렇게 된다.

### 가드에는 컨트롤러가 없다 — HTTP 왕복을 어디로 태울 것인가

Phase 3 완료 기준이 "403 · 404 가 **HTTP 왕복으로** 검증됨"인데 `PetAccessGuard` 의 소비자는 REQ-10 이후 하위 도메인이라 **지금은 HTTP 로 도달할 경로가 없다.** `PetController` 슬라이스에서 `PetService` 목이 같은 `BusinessException(PET_FORBIDDEN / PET_NOT_FOUND)` 을 던지게 해 **`GlobalExceptionHandler` → 상태코드 · `error.code` 매핑**을 고정하는 것으로 옮겼다. 가드와 서비스가 던지는 예외는 동일 `ErrorCode` 라 매핑 계약은 같다. 계획서에 그 사실을 남기고, 가드 자체의 HTTP 경로는 REQ-10 첫 소비자에서 재확인하기로 했다.

### D3 예외 3건은 아직 넣지 않았다

프로브로 형태는 확정됐지만(정상 사용 PASS · 우회 FAIL · 엔티티 참조 FAIL · **예외 없이 사용 FAIL** — 예외가 실제로 필요하다는 것까지 확인) `DomainBoundaryTest` 에 영구 추가는 하지 않았다. **소비자가 없는 지금 넣으면 대상 없는 예외**가 되고, 이 프로젝트는 빈 규칙을 통과로 치지 않는다. REQ-10 착수 시 Phase 0 으로 가져가도록 미결에 등록했다. `/implement` 가 테스트 파일을 못 고친다는 제약도 같은 방향을 가리켰다.

### 셈이 틀릴 뻔했다 — JUnit XML 은 메서드명이 아니라 `@DisplayName` 을 기록한다

CLAUDE.md 는 "실행 건수는 `build/test-results/test/*.xml` 로 확인"이라고 하는데, 그 XML 을 **메서드명(`req_09_1[0-3]`)으로 grep 하면 0건**이다. `testcase name` 에는 `[REQ-09-12] 남의 펫은 …` 같은 DisplayName 이 들어간다. 08-10 의 `--tests` 문자 클래스 함정과 같은 얼굴 — **"안 돌았다"로 읽힐 뻔했는데 실제로는 9건 전부 돌았다.** `[REQ-09-xx]` 로 다시 세서 확인했다. CLAUDE.md 로컬 검증 절에 한 줄 보탰다.

### 계약 표 안의 `|` 가 표 파서를 깬다

REQ-09-15 · 16 의 근거 인용 `` `CRESTED_GECKO | DOG | CAT` `` 안의 `|` 가 마크다운 셀 구분자와 겹쳐 **자동 대조 스크립트가 Phase 열을 `DOG` · `FEMALE` 로 읽었다.** 렌더링과 `grep -F` 에는 문제가 없어 오늘은 고치지 않았다 — `\|` 로 이스케이프하면 근거 인용문이 원문(`범위—포함`)과 달라져 `/testrun` 의 인용 검사가 0건이 된다. 고치려면 **양쪽을 함께** 바꿔야 한다. 표를 기계로 읽는 도구를 만들 때 알고 있어야 할 함정이라 적어 둔다.

### 자율 실행이라 브랜치를 묻지 않고 만들었다

`/implement` 는 브랜치 이름·생성을 승인받게 돼 있는데 사람이 없는 세션이라 기존 이름 규칙(`feat/req09-pet-domain`)을 따라 `feat/req09-phase3-pet-access-guard` 를 만들고 보고에 명시했다. 되돌릴 수 있는 일이라 그렇게 했고, **PR 은 만들지 않았다** — 머지 판단은 사람 몫이다. 머지 시 AGENTS §4 의 SHA 대조·브랜치 삭제 규칙을 따를 것.

~~**남은 미결 (REQ-09, 전부 pet 밖)** — Notion 역반영 2건(§3 · §6) · `GET /pets` 커서 페이지네이션 · 탈퇴 시 pets 처리 · `DELETE` 후 하위 기록 조회 가능 여부 · D3 예외 3건 추가 시점(REQ-10 Phase 0).~~ → 같은 날 오후에 아래에서 처리.

### 미결 12건을 REQ-10 착수 전에 한 번에 털었다 (오후)

REQ-07 2건 · REQ-08 5건 · REQ-09 5건. **원칙은 하나 — 원본(Notion `API I/F`)을 먼저 읽고, 원본에 없으면 사람이 정한 뒤 Notion 에 먼저 쓰고 레포에 옮긴다.** 결과 10건 해소 · 2건은 재검토 트리거만 명시하고 유지(필터 조회 비용 → 첫 배포 후 실측 / 탈퇴 계정 refresh → 앱의 refresh 흐름 확인 후).

**원본을 읽고 나서야 보인 것** — 미결 6건의 원본 행이 전부 **한 줄짜리**였다(`DELETE /pets`: "소프트 딜리트. 연관 기록 보존. 204" / `DELETE /auth/logout`: "Refresh Token 무효화. Body 없음. 204" / `DELETE /users/me`: "소프트 딜리트. 204" …). 즉 미결의 답은 원본에 **없었고**, 파생 요약 `api-list` 가 덧붙인 문장(logout 의 "해당 토큰 `revoked_at`")이 미결처럼 보이게 만든 경우도 있었다. **"원본끼리 어긋난다"고 적어 둔 것이 실은 "파생 요약이 원본에 없는 말을 했다"였다** — REQ-08·09 에서 두 번 본 것과 같은 얼굴.

**결정과 근거** (전부 계획서 미결 절에 `[x]` + 결론으로 기록):
- `DELETE /pets` 연관 기록 보존 = **DB 행 보존, API 조회 불가** — 하위 엔드포인트가 전부 `/pets/{petId}/...` 아래라 `PetAccessGuard` 의 404 가 곧 계약. REQ-10 추가 작업 없음
- `GET /pets` 페이지네이션 **안 넣음** — 원본 응답에 `next_cursor` 없음 재확인
- 탈퇴 시 pets **그대로** — 필터가 탈퇴 사용자를 막고 재가입은 새 `users.id` 라 API 로 닿지 않는다. 함께 소프트 딜리트하는 안은 `business/user → data/pet` 참조가 생겨 기각
- logout **전체 revoke 유지** — Body 없어 특정 불가. api-list 의 낡은 문장 정정
- 동시 refresh **감수** — 유예 윈도우는 탈취 탐지를 약화시키고 Notion §7 에 근거 없음. refresh 직렬화는 클라이언트 책임(앱 구현 시 클라이언트 계약에 적을 것)
- 프로필 이미지 제거 → **D8 전용 엔드포인트 `DELETE /users/me/profile-image`** (REQ-08 Phase 4). `null` = 제거는 D3 을 뒤집고 의존성이 늘어 기각, `""` = 제거는 원본 없는 규약이라 기각
- 닉네임 → **D9 트림 후 1~100자 · 중복 허용 · `""`/공백만 400** (REQ-08 Phase 5). `@NotBlank` 는 AGENTS §5 금지, 중복 금지는 스키마 변경이 따라와 기각

**Notion 에 쓴 것 3건** (전부 fetch 로 저장 형태 확인) — 「소스 구조」 §3·§6·§11 역반영 · `API I/F` 에 행 1개 신규 · `PATCH /users/me` 행에 Validation 절 추가. **§13 ArchUnit 스케치는 일부러 안 고쳤다** — 예외 3건이 코드에 들어간 뒤(REQ-10 Phase 0) 맞춰야 문서가 코드보다 앞서지 않는다.

> **`API I/F` 데이터베이스에 행 추가는 `create-pages` 로 된다.** CLAUDE.md 가 "「설계」 탭은 API 로 수정 불가"라고 적어 둔 것은 **탭 페이지 본문** 얘기고, 그 안의 DB 에 행을 만드는 것(`data_source_id` 부모)은 열려 있었다. 이전엔 "사람이 직접"으로 미뤄 두던 종류의 일이라 CLAUDE.md 에 보탰다.

> **새 검증 계약 9행의 근거 인용을 쓰자마자 검사했다.** REQ-08-23 이 처음엔 계획서에 없는 문구("부분 반영 병합은 서비스에서" — 그건 REQ-09 계획서 문장이다)를 인용하고 있었다. 행 자체가 매칭돼 `grep` 1건으로 보여 놓치기 쉽다 — **2건 이상**이어야 원문이 있는 것이다.

### Notion 에 개발 상태를 반영했다 — `API I/F` 개발상태 · All Tasks 재편

REQ-10 착수 전에 Notion 쪽 현황을 코드와 맞췄다. 기록할 것은 구조 판단 둘.

- **`API I/F` 에 `개발상태`(status) 속성** — 사람이 만들었고 값은 API 로 채웠다. 40행 중 **완료 12**(Auth 3 · Users 4 · Pets 5) · 시작 전 28. "구현됐다"의 기준은 **검증 계약이 ✅ 인 엔드포인트**다 — 코드만 있고 계약이 없는 행은 없으니 지금은 같지만, 앞으로 갈라질 수 있는 정의라 적어 둔다
- **All Tasks 는 작업 단위, All Tasks History 는 일별** — 처음엔 일별 13건을 `All Tasks` 에 넣었다가 되돌렸다. 두 DB 의 역할이 다르다: `All Tasks` 는 REQ/작업 단위(관련 기술·유형 태그), `All Tasks History` 는 `작업일` + `☑️ All Tasks` 관계로 그날 무엇을 했는지. **일별 행은 삭제하지 않고 `move-pages` 로 옮겼다** — 본문(요약)이 보존되고, 이동 후 `이력명`·`작업일`·관계만 채우면 된다. 옮기면서 원래 DB 에만 있던 속성(유형·관련 기술·Project)은 떨어져 나가지만 관계를 타고 작업 단위 쪽에서 보인다
- `PROGRESS.md` 의 `## 날짜` 절 ↔ History 행이 1:1 이다. **앞으로 `/checkpoint` 가 날짜 절을 쓸 때 History 행도 같이 만드는 것**이 자연스럽다 — 이번엔 손으로 했고 자동화는 하지 않았다

> `notion-move-pages` 는 데이터소스 간 이동이 된다(2026-08-27 실측, 13건). 다른 DB 로 옮길 때 스키마가 안 맞는 속성은 조용히 사라지므로 **옮기기 전에 목적지 스키마를 먼저 읽는다**

### REQ-10 검증 계약은 표만 먼저 넣었다 (Phase 0 프로브 3 · Phase 1 케이스 14)

REQ-09 에서 "표는 있는데 코드가 없어 `/implement` 게이트에 걸린" 경험 때문에 이번엔 **처음부터 표만** 넣고 코드는 `/implement` 직전 `/testgen` 재호출로 쓰기로 했다. Java 는 대상 클래스가 없으면 테스트 소스 전체가 컴파일되지 않아 Phase 0 프로브까지 못 돌리기 때문이다. Phase 0 의 01~03 은 **프로브**(심었다 지우는 확인)라 영구 테스트가 아니고, `REQ-08-11` 선례대로 `✅ 수동` 으로 채운다. Phase 1 의 체중 경고·`cursor`/`limit` 2건은 미결이라 케이스를 쓰지 않았다.

### REQ-10 계획 수립 — 원본 23행을 읽었더니 파생 요약이 네 군데서 갈라졌다

`/workplan REQ-10` 전에 Notion `API I/F` 기록 도메인 행 23개를 전부 읽었다. **REQ-09 의 교훈("파생 요약은 양방향으로 배신한다")이 그대로 재현됐다** — `api-list §4~8`·테이블 정의서만 보고 계획했으면 넷 다 틀렸다.

| 충돌 | 원본 A | 원본 B / 스키마 | 결정 |
|---|---|---|---|
| `condition_tag` | `API I/F` **4종** — 이유까지 적혀 있다("거식·탈피는 급여·탈피 기록에서 파생, 단일 출처") | 테이블 정의서·「소스 구조」§8·ERD·`api-list` **7종**("2026-07-27 확정") | **4종** → **ADR-0001** |
| `food_size` (S/M/L) | `API I/F` 응답에 **있음** | `feeding_logs` 에 컬럼 **없음**, api-list "엔티티 그대로" | **V3 컬럼 추가** — 계약이 맞고 스키마가 늦은 것 |
| 체중 20% 경고 | callout 에 명시 | 응답 예시에 필드 **없음** | 파생 필드 — 형태는 Notion 에 먼저 명시(미결) |
| 다이어리 ↔ 사진 | `photo_ids`·`photos[]`·`photo_count` | `photos` 는 REQ-11 | **REQ-11 로 이관**, diary 는 텍스트만 |

> **7종은 나중에 덧붙은 쪽이었다.** `api-list §4` 가 "확정 — 2026-07-27" 이라 적어 둔 것이 오히려 함정이었다 — 그날 대조한 것은 ERD·테이블 정의서였고 **`API I/F` 행은 안 봤다.** 원본이 둘 이상이면 **날짜가 아니라 이유가 적힌 쪽**을 따랐다. 단일 출처 원칙은 이 프로젝트 파생 로직 전체의 전제라 계획서에 묻지 않고 **첫 ADR 로 올렸다** — `docs/adr/` 가 처음으로 비어 있지 않게 됐다.

> ⚠️ **ADR 번호 체계가 둘이다.** Notion 의 ADR-001(스택)·ADR-002(DB 엔진)는 3자리, 레포 `docs/adr/` 는 `/workplan` 규약대로 4자리(`ADR-0001`). 번호가 겹치지 않도록 **레포 ADR 은 0001 부터 새로 세고, Notion 두 건은 이관하지 않는다.** AGENTS §0 표의 "`docs/adr/`는 비어 있다" 문구를 고쳤다.

**원본에 없어 미결로 남긴 것 14건** — 전부 Phase 별로 묶어 "그 Phase 착수 전에 닫는다"로 뒀다. 특히 **거식 스트릭 계산 규칙**(일수 vs 건수 · 기준 시각 · 0건)과 **탈피 예측의 기록 부족 시 동작**은 계산기 검증 계약의 전제라, 답 없이는 `/testgen` 이 케이스를 못 쓴다. Phase 순서를 **weight → activity → feeding → shed → diary** 로 둔 이유 — weight 가 파생 로직·종 분기 없이 가장 단순해 가드 소비·커서·기록 귀속(D6) 패턴을 여기서 확정하고, diary 는 사진 연결 미결 때문에 마지막.

> **D6 은 원본에 없지만 결정으로 박았다** — 하위 기록 조회에 `pet_id` 를 함께 걸지 않으면 **남의 기록 id 를 내 펫 경로로 불러 읽을 수 있다.** 가드는 펫만 보기 때문이다. 원본이 침묵한 보안 결함은 미결이 아니라 결정이다. Phase 1 `/checkpoint` 에서 CLAUDE.md 계약으로 올릴 후보.

### REQ-08 Phase 4·5 — 오전에 정한 것을 저녁에 구현했다

같은 날 `/testgen` → `/implement 4` → `/implement 5` → `/testrun`. 코드는 작다(엔드포인트 1 · 메서드 2 · 애노테이션 1). 남길 것은 둘.

**Phase 5 의 "공백만" 거부가 두 층에 나뉜 이유** — `@Size(min = 1)` 은 `""` 는 잡지만 `"   "`(길이 3) 는 통과시킨다. 트림 후 검사를 애노테이션 하나로 표현하려면 `null` 처리까지 얽혀 D3("`null` = 변경 없음")과 부딪힌다. 그래서 **DTO 는 `""` 만, 서비스가 `strip()` 후 빈 값을 `INVALID_INPUT` 으로** 거른다. 새 `ErrorCode` 는 만들지 않았다(계획서 "기존 확인 후 결정" → 기존으로 충분).

**같은 브랜치에 Phase 를 두 커밋으로 쌓았다.** `/testgen` 이 Phase 4·5 케이스를 한 번에 같은 테스트 파일에 넣었기 때문에 Phase 4 커밋(`68d43c8`)은 **단독으로 CI 빨강**(Phase 5 케이스 3건)이다. 커밋 본문에 그 사실을 적었고 Phase 5 커밋(`978361e`)이 닫는다. Phase 단위 커밋과 "테스트는 먼저 쓴다"가 정적 타입 언어에서 만나면 이 모양이 된다 — **PR 은 두 커밋을 묶어서** 낸다.

> Phase 4 완료 기준의 "미인증 401" 은 케이스가 없어 **테스트로 고정되지 않았다.** `PUBLIC_PATHS` 에 없으니 기본 보호되지만, 계획서에 그 사실을 적고 체크했다. 필요하면 REQ-08-30 으로 추가.


## 2026-08-10

> REQ-09 착수. **설계 판단 하나에 하루의 절반을 썼는데 그게 옳았다** — `PetAccessGuard` 는 REQ-10~12 의 여섯 도메인이 그대로 복제할 형태라, 여기서 틀리면 여섯 번 틀린다.

### 가드의 형태를 문서가 아니라 프로브로 정했다

"어떤 ArchUnit 규칙에 걸리는가"를 추론으로 답하지 않고 **가짜 `PetAccessGuard` 와 `business/diary/service` 를 실제로 심어** 8가지 배치를 셌다(표는 PLAN-REQ-09 「프로브 결과」).

**착수 전에 적어 둔 예상이 틀렸다.** "Service → Service 라 `LAYER_DIRECTION` 에 걸린다"고 계획서에 썼는데 **걸리지 않았다** — `mayOnlyBeAccessedByLayers("Controller")` 는 같은 레이어 안의 참조를 막지 않는다.

> 08-03 프로브에서 같은 규칙이 필터의 Repository 직참조를 잡았던 것과 상황이 다르다. **그때 걸린 이유는 필터가 어느 레이어에도 속하지 않아서**였다. 같은 규칙이라도 "누가 부르느냐"에 따라 결과가 갈린다 — **규칙 이름만 보고 추론하면 틀린다.** 예상과 실측이 갈린 것을 계획서에 정정해 남겼다.

핵심은 **예외 개수가 아니라 그 예외가 여는 문**이었다.

- **A안**(원본 §3 그대로 `Pet` 엔티티 반환) — 예외 4→6건. 그런데 `data.pet` 을 통째로 열어야 해서 **하위 도메인이 `PetRepository` 를 직접 주입해도 ArchUnit 이 통과시킨다.** 즉 **가드를 우회하는 코드가 규칙에 안 걸린다** — 소유권 앵커를 두는 목적 자체를 규칙이 못 지킨다
- **B2′안**(framework 포트 + `boolean`) — 예외 **0건**으로 가장 깨끗한데 `species` 를 실어 나를 수 없다. **종 검증 자리가 사라지는 것은 기능 손실이라 다른 방식으로 갚을 수 없다**
- ⭐ **D안**(읽기 전용 DTO `OwnedPetResponse(id, species)` 반환 + 예외를 `business.pet.service`·`data.pet.dto`·`data.pet.enums` **셋으로 한정**) — 예외는 7건으로 A보다 **하나 더 많은데**, 우회(`PetRepository` 직접 주입)와 엔티티 누출(`Pet` 직접 참조)이 **둘 다 잡힌다.** `entity`·`repository` 를 닫아 두기 때문이다

**늘어난 예외가 오히려 경계를 더 정확히 그린다** — 이게 D안을 고른 이유다. AGENTS §5 "Entity 는 Service 밖으로 나가지 않는다"와도 결이 같다(하위 도메인에게 pet 은 남의 도메인이다).

> 다만 Notion 「소스 구조」 §3 은 "`Pet` 을 받아 처리한다"고 적었으므로 **역반영 대상**이다. §6(소프트 딜리트를 `@SQLRestriction` 으로)도 실제와 갈렸다. 둘 다 사람이 Notion 에서 고쳐야 한다.

### 파생 요약은 원본을 **양방향으로** 배신한다

REQ-08 때는 파생 요약이 **원본에 없는 내용을 담고** 있었다. 이번엔 반대였다 — **원본 `API I/F` 의 Validation 규칙(`name` 필수, `species`·`gender` 허용값)이 `api-list §3` 에 통째로 없다.** 상태코드도 없다. 원본 5행을 직접 읽지 않았으면 검증 케이스 3건이 아예 안 나왔다.

그래서 **원본 Validation 문구를 계획서 `범위—포함` 에 옮겨 적었다.** `/testrun` 의 근거 인용 검사는 **파일을 `grep`** 하는데 Notion 은 파일이 아니라 검사 대상이 못 된다. 원본에만 두면 케이스가 근거를 못 갖거나 "근거 소실" 오탐이 난다.

> **대신 새 취약점이 생겼다** — 옮겨 적은 사본이 Notion 원본과 갈라져도 `grep` 은 계속 초록불이다. 이 검사가 잡는 것은 "계획서 안에서의 정합성"뿐이다.

### 정의되지 않은 enum 값이 400 이 아니라 500 이었다

`POST /pets` 에 `"species":"HAMSTER"` 를 보내면 **500** 이 나갔다. Jackson 의 `InvalidFormatException` 은 `MethodArgumentNotValidException` 이 **아니라** `HttpMessageNotReadableException` 으로 올라오기 때문에 기존 핸들러 셋 중 어디에도 안 걸리고 `Exception` 핸들러까지 떨어진다.

REQ-08 D7 의 `@Size` 누락(101자 → 500)과 **같은 계열**이다 — 클라이언트 입력 오류가 서버 오류로 보고된다(AGENTS §5 금지).

> **Phase 경계를 한 번 넘었다.** 고칠 곳이 `framework/processor/handler` 라 pet 범위 밖이지만 "하는 김에"가 아니다 — `REQ-09-15·16` 이 이것 없이는 성립하지 않는다. **대신 이 변경은 전역이다**: 모든 엔드포인트에서 깨진 JSON 이 500 대신 400 이 된다. pet 밖의 영향은 이번에 테스트로 덮지 않았다.

### Checkstyle `ParameterNumber`(최대 7)와 필드 8개

`Pet` 은 필드가 8개라 정적 팩토리 메서드로 만들 수 없었다. **`@Builder` 를 생성자에 붙이는 안도 실패한다** — Lombok 이 빌더를 만들어도 **소스의 생성자가 여전히 8파라미터라 Checkstyle 이 그걸 센다.**

통과한 형태는 **클래스 레벨 `@Builder` + `@AllArgsConstructor(access = PRIVATE)`** 다. 생성자를 Lombok 이 만들면 소스에 없으므로 Checkstyle 이 보지 못한다.

> ⚠️ **부작용 — 빌더에 `id` 가 노출된다.** 엔티티 javadoc 에 경고를 남겼지만 **코드로는 못 막았다.** 규칙을 우회한 것이라 대가가 있다는 것을 기록해 둔다.

### 프로브가 거짓 음성을 냈다 — Gradle `--tests` 는 문자 클래스를 모른다

핸들러를 무력화해 `REQ-09-15·16` 이 빨간불이 되는지 확인하려고 `--tests '*req_09_1[56]*'` 를 돌렸는데 **BUILD SUCCESSFUL** 이 나왔다. "안 잡힌다"로 읽었지만 실제로는 **0건 매칭**이었다 — Gradle 의 `--tests` 패턴은 `*`·`?` 만 알고 `[56]` 같은 문자 클래스를 지원하지 않는다.

클래스명 필터로 다시 돌려 정정했고, 두 케이스 모두 정상 발화했다.

> CLAUDE.md 가 이미 경고하는 **"0건 매칭도 BUILD SUCCESSFUL"** 의 다른 얼굴이다. 그때는 커밋 근거로 못 쓴다는 맥락이었는데, **프로브에서는 더 위험하다** — "규칙이 결함을 못 잡는다"는 **반대 결론**을 만들어 낸다.

### Notion 역반영 1건 완료 (REQ-08)

테이블 정의서 §10 의 Redis 기각 문장("로그아웃·**탈퇴** 시 즉시 무효화가 필요하고…")이 REQ-08 D5(탈퇴 시 revoke 하지 않는다)와 어긋나 보이던 것을 고쳤다. **문장을 지우지 않고 단서를 붙였다** — 이건 Redis 기각 논거이지 무효화 시점의 명세가 아니라는 것, 그리고 `revoked_at` 이 찍히는 경로가 로테이션·로그아웃·재사용 감지 **셋뿐**이라는 것을 함께 명시했다.

## 2026-08-07

> REQ-08 Phase 2(탈퇴) 구현과 **첫 실제 카카오 로그인 왕복**. 왕복 한 번에 **동작한 적 없던 버그 1건**과 **`main` 을 비껴간 커밋 5건**이 같이 나왔다.

### 카카오 로그인은 한 번도 동작한 적이 없었다

`REQ-08-11`(탈퇴 → 재로그인) 검증을 하려고 처음으로 실제 로그인을 시도했다. REQ-07 상태에 "**로그인 왕복 수동 확인 남음**"이라 적혀 있던 그 확인이다. 첫 요청에서 502 가 났다.

- **카카오는 200 과 `access_token` 을 정상 반환**했는데 우리가 "no access token" 으로 판단했다. 응답 로그에는 값이 멀쩡히 찍혀 있어 **외부 API 장애로 오진하기 딱 좋은 형태**다
- 증상이 같은 원인이 둘이라 구분이 필요했다 — ① 필드 매핑 ② AGENTS §5 가 경고하는 **응답 스트림 소비**. 인터셉터가 `BufferingClientHttpResponseWrapper` 로 감싸고 `getBody()` 가 매번 새 스트림을 돌려주는 것을 확인해 ②를 배제했다
- 원인: **`JacksonConfig` 의 snake_case 커스터마이저는 Spring Boot 가 자동 구성하는 `ObjectMapper` 에만 적용된다.** 이 DTO 를 읽는 것은 `RestTemplateConfig` 의 `new RestTemplate()` 이고 그건 자기 컨버터 안에서 맨 `new ObjectMapper()` 를 쓴다 → `access_token → accessToken` 바인딩이 없어 전 필드 `null`
- `KakaoTokenResponse` javadoc 이 적어 둔 "전역 snake_case 설정이 매핑한다"는 **전제 자체가 틀렸다**

> **왜 지금까지 안 잡혔나 — 단위 테스트가 `KakaoOAuthClient` 를 목으로 대체해 이 경로를 한 번도 타지 않았다.** REQ-07 검증 계약 23건이 전부 초록불인 채로, 로그인이라는 기능 자체가 죽어 있었다. "테스트 통과"와 "동작함"의 거리를 이보다 잘 보여주는 사례가 없다.

`@JsonProperty` 명시로 고쳤다(`bf41915`). **회귀 테스트는 설정 없는 `new ObjectMapper()` 로 역직렬화한다** — 설정된 매퍼로 검증하면 버그가 있어도 초록불이 되기 때문이다. 프로브로 확인했다(애노테이션 제거 → REQ-07-24 빨간불).

`KakaoUserResponse` 도 같은 문제였다. 토큰 교환만 고쳤으면 `getProfile` 에서 다시 터졌고, `nickname` 을 못 읽어 `users.nickname` NOT NULL 때문에 **자동가입이 깨졌을 것**이다.

> ⚠️ **대가 — 인가코드는 1회용이다.** 실패할 때마다 브라우저로 새로 받아야 해서 이 버그 하나에 코드를 3번 받았다.

### 스택 PR 이 커밋 5건을 `main` 밖에 두고 갔다

Phase 1 을 스택 PR(#22, base = Phase 0 브랜치)로 올리면서 "#21 이 머지되면 base 가 `main` 으로 자동 재지정된다"고 봤는데, **재지정 조건은 "선행 PR 머지"가 아니라 "base 브랜치 삭제"였다.** #21 머지 후에도 브랜치가 남아 있어 28분 뒤 #22 가 그 위로 머지됐다.

**머지는 성공하고 PR 도 `MERGED` 가 된다.** `main` 에 `UserService` 조차 없다는 것은 따로 확인해야만 보였다. #23 으로 복구했고 AGENTS §4 에 계약으로 올렸다.

> 07-27 PR #8 사고(head SHA 불일치로 커밋 3건 누락)와 **증상은 같고 원인이 다르다.** 두 번째다 — `main` 을 확인하지 않으면 누락은 조용하다.

### Phase 0 "유실"은 오진이었다

08-03 에 "07-31 Phase 0 가 커밋되지 않고 유실됐다"고 기록했는데 **틀렸다.** 미푸시 로컬 브랜치 `chore/archunit-tighten-empty-allowance`(`f6f66c7`)에 동일한 변경이 전부 있었다.

원인은 진단에 쓴 명령이다 — **`git log -- <경로>` 는 HEAD 도달 가능 커밋만 본다.** 다른 브랜치 작업은 안 보이는데 출력은 "이 파일의 전체 이력"처럼 보이고 에러도 경고도 없다. `d9016e3` 은 결국 중복 재구현이 됐다(다만 프로브 3건은 08-03 판에만 있다).

AGENTS §4 에 올리면서 **뿌리 원인도 함께 적었다 — "커밋을 안 한 것"이 아니라 "푸시·PR 을 안 한 것"이다.** 로컬 전용 브랜치는 다음 세션에서 없는 것과 구별되지 않는다.

### `REQ-08-11` — D1 이 실제로 동작한다

빈 DB 에서 실제 카카오 계정으로 왕복했다. **자동화하지 않기로 한 케이스**이고, 계획서가 "목으로는 검증되지 않는다"고 못박은 이유가 여기 있다.

| | `users.id` | 상태 |
|---|---|---|
| 탈퇴 전 | `a02016c0…` | `deleted_at` 찍힘 |
| 재로그인 후 | **`94eef1f2…`** | 활성 · 소셜 행이 새 `user_id` 로 재생성 |

소셜 행을 하드 삭제하지 않았다면 조회가 옛 행을 찾아 `a02016c0…` 로 토큰을 발급했을 것이다.

**같은 왕복에서 Phase 1 커버리지 공백 2건도 실측으로 닫혔다** — `GET /users/me` 가 5필드·snake_case 로 나갔고(`updated_at` 없음), 101자 닉네임은 **400 `INVALID_INPUT`**(500 아님)이었다. 다만 **사람이 한 번 본 것이라 회귀는 못 막는다.**

### 계획서 미결이 틀렸던 것 — 401 이 아니라 404

D5 대가로 등록해 둔 "탈퇴 계정도 `/auth/refresh` 가 200 을 반환한다"는 그대로 재현됐다. 그런데 이어지는 "**그 토큰으로 API 를 부르면 401**"이 **실제로는 404 `USER_NOT_FOUND`** 였다 — Phase 3 필터가 없어 서비스의 `findByIdAndDeletedAtIsNull` 이 먼저 걸리기 때문이다.

> **그 문구는 Phase 3 완료를 전제한 서술이었다.** 미결을 쓸 때 "언제 시점의 동작인지"를 안 적으면 이렇게 어긋난다. Phase 3 에서 404 → 401 전환을 확인 대상으로 넣었다.

### 계약 3건 승격 (AGENTS)

전부 이번에 실제로 밟은 것이다.

- §4 — `git log -- <경로>` 범위 함정 + "끝냈으면 푸시한다"
- §4 — 스택 PR base 재지정 조건 + 머지 후 `git log origin/main..origin/<브랜치>` 확인
- §5 — **PATCH 요청 DTO 에 `@NotBlank`·`@NotNull` 금지.** `null` 을 거부해 "필드 미전송 = 변경 없음"을 깬다. D7 초안에서 밟았고 `/testgen` 중 철회했다

### 잔가지

- **`application.yml` 이 `.env` 를 읽는다** — `spring.config.import: optional:file:.env[.properties]`. `set -a && . ./.env` 가 더 이상 필요 없다. `optional:` 을 떼면 CI 가 통째로 깨진다
- lefthook `commit-msg` 가 `wip` 타입을 거부한다 (허용: `feat fix docs style refactor perf test build ci chore revert`)
- **`ci.yml` 트리거가 `push: main` + `pull_request` 뿐이다** — PR 없이 푸시한 브랜치는 CI 가 돌지 않는다
- REQ-07-24·25 는 **코드가 표보다 먼저 들어갔다**(CLAUDE.md 는 표가 먼저). 이번 checkpoint 에서 표를 채웠다
- 로컬 DB 에 테스트 계정 2건(탈퇴 1·활성 1)을 **의도적으로 남겼다** — Phase 3 의 `REQ-08-16`(탈퇴 계정 401)·`REQ-08-17`(활성 통과) 대조군으로 재사용한다

### Phase 3 — 포트 하나로 규칙 두 개를 만족시켰다

`JwtAuthenticationFilter` 가 매 인증 요청마다 `UserStatusChecker.isActive` 를 부른다. 이게 없으면 **탈퇴해도 기존 access 토큰이 최대 30분 살아 있다** — 필터가 서명·타입만 보고 DB 를 안 보기 때문이다.

**포트를 쓴 이유는 규칙 하나 때문이 아니다.** 07-31 D2 는 "규칙 #4 를 연다 vs 포트를 둔다"로 저울질했는데, 08-03 프로브에서 **직참조가 `FRAMEWORK_MUST_NOT_KNOW_DOMAIN` 과 `LAYER_DIRECTION` 을 동시에 깬다**는 것이 드러났다(필터는 정의된 세 레이어 어디에도 속하지 않는데 `Repository` 레이어는 `mayOnlyBeAccessedByLayers("Service")` 다). 즉 **앞쪽 안은 애초에 성립하지 않았고**, 포트가 유일한 길이었다. 이번에도 프로브로 재확인했다.

- **비활성일 때 예외를 던지지 않는다.** 필터는 DispatcherServlet 앞이라 던지면 `GlobalExceptionHandler` 에 닿지 않아 응답 형태가 갈린다. `SecurityContext` 를 세팅하지 않고 통과시키면 `SecurityConfig` 의 entryPoint 가 기존 규격대로 401 을 낸다
- 포트 시그니처가 `UUID → boolean` 인 것도 계약이다. 엔티티를 돌려주면 framework 가 `data..entity..` 를 알게 된다

### 미결 정정이 실측으로 확인됐다 — 404 → 401

오늘 낮에 "미결의 401 은 Phase 3 완료를 전제한 서술이었다"고 정정하고 **Phase 3 검증 대상으로 등록**해 뒀는데, 그대로 재현됐다.

| 시점 | 탈퇴 계정 토큰으로 `GET /users/me` |
|---|---|
| Phase 2 | **404** `USER_NOT_FOUND` (서비스의 `findByIdAndDeletedAtIsNull` 이 잡음) |
| Phase 3 | **401** `UNAUTHORIZED` (필터가 앞에서 잡음) |

**남겨 둔 테스트 계정 2건이 그대로 쓰였다.** access 토큰은 30분 만료라 죽어 있었지만 `refresh_tokens` 가 살아 있어 `/auth/refresh` 로 재확보했다 — 탈퇴 계정도 200 을 주는 D5 동작이 오히려 검증을 쉽게 만들었다.

### 남은 공백은 한 종류다 — 미결로 등록

REQ-08 자동 테스트 24건이 전부 초록불이지만, **HTTP 왕복이 있어야만 검증되는 지점 3건은 여전히 "사람이 한 번 본 것"뿐**이다.

| 지점 | 테스트가 덮는 곳 | 안 덮는 곳 |
|---|---|---|
| snake_case 직렬화 | record 컴포넌트 이름 | 직렬화된 JSON 키 |
| 101자 → 400 | Bean Validation 위반 생성 | `INVALID_INPUT` 매핑 |
| 탈퇴 토큰 → 401 | 인증 미설정 + 체인 계속 | entryPoint 가 내는 401 |

셋 다 **깨져도 조용하다.** `@WebMvcTest` 도입을 미결로 올렸고, **REQ-08 에 끼워 넣지 말고 별도 REQ 로** 잡을 것을 권했다 — pet·diary 등 다음 도메인이 전부 같은 공백을 갖게 되므로 컨트롤러 테스트 관례를 한 번 정하는 작업으로 다루는 편이 낫다.

> **한 번 봤다는 것과 고정됐다는 것은 다르다.** 오늘 카카오 로그인이 정확히 그 차이 때문에 죽어 있었다.

### REQ-15 — 테스트가 통과하면서 틀린 계약을 고정할 뻔했다

REQ-08 이 남긴 공백("HTTP 왕복이 있어야만 검증되는 지점 3건이 사람이 한 번 본 것뿐")을 닫기로 하고 별도 REQ 로 뗐다. **REQ-09 착수 전에 하는 것이 핵심**이다 — 나중에 하면 REQ-09~12 컨트롤러를 전부 소급해야 한다.

미결 ②(`@WebMvcTest` 가 우리 구성을 어떻게 끌어오는가)는 **문서로 답하지 않고 실제로 띄워서** 닫았다. 네 가지가 나왔고 셋은 모르면 반드시 밟는다.

| 관찰 | 안 하면 |
|---|---|
| `Filter` 빈이 슬라이스에 **자동 포함** | `JwtTokenProvider` 없으면 컨텍스트가 안 뜬다. `addFilters=false` 로도 빈 생성은 안 막힌다 |
| `SecurityConfig` 를 `@Import` 해야 | 기본 시큐리티가 401 을 내는데 **본문이 빈다.** CSRF 기본값 때문에 `PATCH`·`DELETE` 는 403 |
| ⭐ `JacksonConfig` 도 `@Import` 해야 | 응답이 **`profileImageUrl`(camelCase)** 로 나간다 |
| ⭐ `UserStatusChecker` 를 따로 목으로 두면 안 됨 | `UserService` 가 그 인터페이스를 구현해(REQ-08 D2) 목끼리 충돌 → **`UserService` 정의가 사라진다.** 에러는 "UserService 없음"이라 원인이 안 보인다 |

> ⭐ **세 번째가 이 REQ 의 존재 이유를 그대로 재현했다.** `@Import(JacksonConfig)` 없이 짰다면 테스트는 `profileImageUrl` 을 단언하고 **통과했을 것**이다 — 실제 계약과 반대인 값을 초록불로 굳히고, 심하면 누군가 그걸 보고 DTO 를 "고쳐" 운영을 깬다. 사용자 정의 `@Configuration` 이 슬라이스에서 **조용히 빠지는 것**이 원인이다.

### 초록불이 근거가 되지 못하는 REQ

이 REQ 는 **산출물이 테스트 자체**라 대상 코드가 이미 존재했다. 그래서 `/testrun` 은 처음부터 22건 초록불이었고, **그 초록불은 아무것도 증명하지 않는다.**

실질 검증은 프로브였다. 심은 결함 3종이 각각 정확한 케이스를 빨간불로 만들었다 — `JacksonConfig` 제거 → `REQ-15-01`, `SecurityConfig` 제거 → `03·04·05·06`, 필터 검사 제거 → `REQ-15-05`.

> **여기서 설계 검증이 하나 나왔다.** `SecurityConfig` 를 뺐을 때 `REQ-15-05` 는 **`error.code` 단언만 깨지고 `status().isUnauthorized()` 는 통과**했다 — 기본 시큐리티도 401 을 내기 때문이다. **상태코드만 단언했다면 잘못된 시큐리티 구성을 못 잡는다.** `/testgen` 에서 "한 케이스에 단언 하나"를 지켜 `status` 와 `code` 를 나눠 둔 것이 우연이 아니라 실제로 값을 했다.

### Phase 3 은 체크하지 않았다 — 자기 채점이 되기 때문

AGENTS §6 에 관례를 기록했고 문안과 실제 코드가 어긋나지 않는 것도 대조했다(목 구성 2종·`@Import`·인증 방식). 그런데 완료 기준이 **"다음 담당자가 이 문서만 보고 쓸 수 있는가"** 다.

**작성자가 그걸 판정하면 자기 채점이다.** 검증 계약에 케이스를 억지로 붙이는 것도 답이 아니다 — 자동 검증이 성립하지 않는 완료 기준에 케이스를 만들면 그게 가짜 테스트다. **사람이 §6 을 읽고 체크하도록 `- [ ]` 로 남겼다.**

> 이 REQ 에서 두 번 같은 판단을 했다 — ① 자동 검증 불가한 완료 기준에 케이스를 만들지 않는다 ② 그래서 Phase 도 켜지 않는다. **"계획서가 다 됐다고 말하는데 실제로 안 된 상태"를 피하는 쪽**을 택했다.

### 정리 (2026-08-10 확인)

PR #29 머지로 REQ-15 Phase 1~2 가 `main` 에 들어갔다. **그 결과 REQ-08 의 미결 하나가 닫혔다** — `@WebMvcTest` 도입은 REQ-15 로 분리해 실행됐고, 공백 3건(snake_case 직렬화 · 101자→400 매핑 · 탈퇴 토큰→401)이 이제 테스트로 고정된다.

> ⚠️ **기록이 하루 늦게 정확해졌다.** 08-07 checkpoint 에서 인덱스에 "REQ-08 미결 6건" 이라 적었는데 그 시점 실제 개수는 **7건**이었다 — `@WebMvcTest` 항목을 추가한 직후였는데 세지 않았다. 해소 처리하고 나니 6건이 되어 우연히 맞아떨어졌지만, **맞은 이유가 세어서가 아니었다.** 미결 개수를 손으로 적을 때는 세고 적을 것.

**REQ-15 Phase 3 은 2026-08-10 에 닫혔다.** AGENTS §6 작성은 08-07 에 끝났지만 완료 기준("다음 담당자가 이 문서만 보고 쓸 수 있는가")을 작성자가 판정할 수 없어 사흘간 `- [ ]` 로 두었고, **사람이 §6 을 읽고 충족으로 판정**해 체크했다. **이것이 이 완료 기준의 정상 경로다** — 자동화할 수 없는 기준은 사람이 읽는 것으로만 닫힌다.

## 2026-08-04

> REQ-08 Phase 1(프로필 조회·수정) 구현. **구조 규칙이 테스트 파일을 잡는 결함**이 여기서 처음 드러났다. 검증은 08-07 `/testrun` 에서 돌렸다.

### ArchUnit 이 테스트 클래스까지 보고 있었다

Phase 1 을 다 짜고 돌렸더니 `DTO_NAMING` 이 빨간불이었다. 원인은 구현이 아니라 **테스트 파일의 위치**였다 — `@AnalyzeClasses` 기본 설정이 테스트 클래스를 분석 대상에 넣어서, `com.petkok.data.user.dto.UserUpdateRequestTest` 가 "`..dto..` 는 Request/Response 로 끝난다"에 걸렸다.

- **`..dto..` · `..controller..` 에 테스트가 처음 들어온 시점이라 그전까지 안 보였다.** 07-29 도입 때도, 08-03 에 `allowEmptyShould` 를 끌 때도 드러나지 않았다. 규칙 8건이 전부 통과하고 있었지만 **미러 패키지에 테스트를 두는 순간 오발하는 상태**였다
- 계획서 「범위 — 제외」가 ArchUnit 변경을 막고 있어 **손대지 않고 멈춰 판단을 받았다.** `DoNotIncludeTests` 로 분석 범위를 프로덕션으로 한정하는 쪽으로 결정 — 완화가 아니라 **범위 정정**이다. 8개 규칙은 전부 프로덕션 구조에 대한 것이라 테스트를 빼도 잃는 커버리지가 없다
- **프로브로 확인했다.** 프로덕션 클래스로 심은 위반 3종에 규칙 4개가 발화 — `..dto..`/`..controller..` 에 이름 안 맞는 클래스 → `DTO_NAMING`·`CONTROLLER_NAMING`, 필터의 `UserRepository` 직참조 → `FRAMEWORK_MUST_NOT_KNOW_DOMAIN` + `LAYER_DIRECTION`

> **Phase 2 의 지뢰가 같이 제거됐다.** `REQ-08-15` 가 `UserController` 대상이라 `CONTROLLER_NAMING` 에 똑같이 걸릴 예정이었다.

### `@NotBlank` 가 PATCH 를 통째로 깨뜨릴 뻔했다 — D7 즉시 철회

08-03 에 "닉네임 검증 최소선"을 정하며 `@NotBlank` + `@Size(max = 100)` 으로 적었는데, `/testgen` 으로 케이스를 뽑다가 **D3 과 정면 충돌**하는 걸 발견했다.

`@NotBlank` 는 `null` 을 거부한다. 그런데 D3 은 "누락·`null` 모두 변경 없음"이라 **닉네임을 안 보내는 것이 정상 경로**다 — 그대로 뒀으면 부분 수정 요청이 전부 400 이 됐다.

> **`NOT NULL` 은 엔티티의 불변식이지 PATCH 요청 DTO 의 불변식이 아니다.** 두 층의 제약을 같은 것으로 착각한 실수다. `@Size` 는 `null` 을 통과시키므로 길이만 막는 것이 맞다.

부산물로 **빈 문자열(`""`)** 이 미결로 새로 올라왔다 — `@Size` 만으로는 통과해 그대로 저장된다(`NOT NULL` 은 만족). 최소 길이 규칙이 정해지면 `@Size(min = 1, ...)` 로 함께 닫힌다.

### 병합을 서비스에 둔 이유 (D6)

`User.updateProfile` 이 REQ-07 때 선반영돼 있었는데 **두 필드를 무조건 덮어쓴다.** 요청 값을 그대로 넘기면 닉네임만 담긴 PATCH 가 `profile_image_url` 을 지운다 — **응답은 200 으로 정상이고 DB 만 조용히 손상된다.** 07-31 에 잡은 함정 2건(유령 계정·잔존 토큰)과 같은 종류다.

엔티티가 `null` 을 "변경 없음"으로 읽게 고치는 안을 기각했다. 그러면 `null` 에 도메인 의미가 붙어 "원본에 없는 규약을 만들지 않는다"(D3)가 무너진다. 부분 반영은 HTTP PATCH 의 관심사다. `UserTest` 의 `REQ-08-08` 이 **엔티티 쪽 계약을 고정**해 두었으므로, 증상을 보고 엔티티를 고치면 빨간불이 난다.

### Notion 대조 — api-list 의 revoke 서술은 원본에 없었다

Phase 2 착수 전 확인 과제였던 "탈퇴 시 refresh revoke" 를 원본에서 확인했다. **D5(revoke 안 함) 유지**로 닫혔다.

| 출처 | 날짜 | 탈퇴 시 revoke |
|---|---|:--|
| `API I/F` → 회원 탈퇴 (1차 출처) | 07-04 | 언급 없음. "소프트 딜리트" + "204" 가 전부 |
| 테이블 정의서 §10 `revoked_at` | 07-29 역반영 | "로테이션·로그아웃·재사용 감지로 찍힌다" — **탈퇴 빠짐** |
| 테이블 정의서 §10 저장소 선택 근거 | 07-23 | ⚠️ "로그아웃·**탈퇴** 시 즉시 무효화가 필요" |

마지막 줄이 유일한 반대 근거인데 **"왜 Redis 가 아니라 DB 인가"의 논거**이지 탈퇴 동작 명세가 아니고, 나머지보다 6일 오래됐다(AGENTS §0 — 날짜로 판단).

> **`api-list.md` 의 "탈퇴 → 해당 사용자 토큰 전체 revoke" 는 어느 원본에서도 나오지 않았다.** 파생 요약이 자체 생성한 문장이다. 07-31 에 이 문서만 보고 D5 를 뒤집었다면 예외가 4→5 로 늘 뻔했다. **파생 문서가 원본에 없는 내용을 만들어 낼 수 있다**는 사례가 하나 더 쌓였다(이전: `/users/me/social-accounts` 3종).

### 완료 기준이 케이스보다 넓은 지점 2건 (08-07 `/testrun`)

Phase 1 케이스 8개 ID 는 전부 통과했지만, **완료 기준 문장 중 테스트가 안 덮는 부분**이 남아 있다. 실패가 아니라 커버리지 공백이라 Phase 는 체크하되 사실을 남긴다.

- **"전역 snake_case" 가 검증되지 않는다.** `REQ-08-01` 은 record 컴포넌트 이름(camelCase)만 확인한다 — 직렬화된 JSON 이 실제로 `profile_image_url` 로 나가는지는 아무도 안 본다. `JacksonConfig` 가 사라져도 초록불을 유지한다
- **"101자 → 400" 의 마지막 한 칸이 비어 있다.** `REQ-08-06` 은 Bean Validation 이 위반을 만든다는 것까지고, 그게 400 이 되는 것은 코드를 읽어 확인했을 뿐이다(`GlobalExceptionHandler` 가 `MethodArgumentNotValidException` → `INVALID_INPUT`)

둘 다 `@WebMvcTest` 한 건이면 닫힌다. Phase 2 의 `REQ-08-15`(컨트롤러 204) 와 함께 다루는 것이 자연스럽다.

### 잔가지

- **lefthook `commit-msg` 가 `wip` 타입을 거부한다.** 허용 목록은 `feat fix docs style refactor perf test build ci chore revert` 뿐이다. Phase 1 중간 커밋을 `wip(REQ-08):` 로 쓰려다 막혔고 `feat(user): … (미완)` 으로 바꿨다
- **Phase 1 브랜치는 CI 가 돌지 않았다.** `ci.yml` 트리거가 `push: main` + `pull_request` 뿐이라 **PR 이 없으면 검증이 없다.** 지금 초록불의 근거는 로컬 게이트 재현뿐이다
- `profile_image_url` 에도 `@Size(max = 500)` 을 붙였다 — D7 은 닉네임만 다루지만 실패 모드(`varchar(500)` 초과 → 500)와 근거(스키마)가 동일하다. **계획서 범위를 한 칸 넘은 판단**이라 기록해 둔다

## 2026-08-03

> `main` 기준으로 Phase 0 가 반영돼 있지 않은 것을 보고 재실행한 날이다. **08-07 에 밝혀졌지만 이 진단은 틀렸다** — 아래 정정을 함께 읽을 것.

### 문서가 "완료"라고 말하는데 `main` 의 코드는 그대로였다

`/progress` 로 현황을 훑고 계획서를 코드와 대조하다 나왔다. PROGRESS·계획서 둘 다 "Phase 0 완료 ✅" 로 체크돼 있었는데 —

- `57f5ca1` 이 바꾼 것은 **문서 3개뿐**이었다
- `git log -- src/test/java/com/petkok/architecture/` 의 마지막 변경이 `efef567`(07-29)
- `allowEmptyShould(true)` 7건, `withOptionalLayers(true)`, 예외 #4 의 "임시" 주석이 **전부 그대로**

그래서 "검증만 하고 커밋되지 않은 채 유실됐다"고 판단하고 `d9016e3` 으로 다시 만들었다.

> ### ⚠️ 정정 (2026-08-07) — 유실이 아니었다
>
> 미푸시 브랜치를 훑다가 **`chore/archunit-tighten-empty-allowance`** 를 발견했다. `f6f66c7`(07-31)에 **08-03 에 다시 만든 것과 기능적으로 동일한 변경이 전부 들어 있다** — `allowEmptyShould(false)` 7건 · `withOptionalLayers(false)` · 예외 #4 승격. 07-31 세션은 검증도 커밋도 했고, **푸시·머지·PR 만 안 했다.**
>
> **`git log -- <경로>` 는 HEAD 에서 도달 가능한 커밋만 본다.** 다른 브랜치의 작업은 보이지 않는데 출력은 "이 파일의 전체 이력"처럼 보인다 — 에러도 경고도 없다. 08-03 에 이 한 줄로 "커밋 안 됨"을 단정했고, 그 결과 `d9016e3` 은 **f6f66c7 의 중복 재구현**이 됐다.
>
> 확인하려면 `git log --all -- <경로>` 나 `git branch --all --contains <sha>` 를 써야 한다. **"커밋이 없다"고 말하기 전에 `--all` 을 붙였는지 확인할 것.**
>
> 재작업 자체는 헛되지 않았다 — 08-03 판이 프로브 3건과 `LAYER_DIRECTION` 발화라는 수확을 남겼고(아래), 그건 07-31 판에 없다. 하지만 **작업이 사라졌다는 진단은 틀렸고, 브랜치를 놓친 것이 진짜 원인이다.**

### 프로브에서 나온 예상 밖 수확 — D2 가 검토한 안은 애초에 성립하지 않았다

규칙을 고쳤으니 계약대로 위반을 심어 확인했는데(CLAUDE.md), 필터에 `UserRepository` 직참조를 넣자 **규칙 #4 와 `LAYER_DIRECTION` 이 함께** 빨간불이 됐다.

`LAYER_DIRECTION` 은 "어느 레이어에도 속하지 않는 클래스"의 접근까지 잡는다. 필터는 Controller·Service·Repository 어디에도 없는데 `Repository` 레이어가 `mayOnlyBeAccessedByLayers("Service")` 이기 때문이다.

> **즉 규칙 #4 를 열어도 직참조는 여전히 통과하지 못한다.** 07-31 에 D2 가 "규칙 #4 를 연다 vs 포트를 둔다"로 저울질했는데, **앞쪽 안은 처음부터 성립하지 않았다.** 규칙 하나만 보고 판단했기 때문에 몰랐다. Phase 3 의 포트 방식은 두 규칙을 동시에 만족시키는 유일한 길이다.

### 계획서를 코드와 대조해 결정 2건이 추가됐다

계획서만 읽고 구현했으면 밟았을 것들이다 — D6(병합 위치)·D7(닉네임 검증). 둘 다 08-04 절에 적었다. 이때 D7 에 `@NotBlank` 를 넣은 것이 다음 날 철회됐다.

## 2026-07-31

> 코드를 쓰지 않은 날이다. REQ-08 계획 수립과 ArchUnit 정리만 했고, **계획이 도중에 한 번 뒤집혔다.**

### 규칙을 열려다 규칙을 지키는 쪽으로 돌아왔다 — D2 번복

오늘의 핵심 기록이다. 탈퇴한 계정의 access 토큰이 최대 30분 살아 있는 문제(필터가 DB를 안 본다)를 놓고 **같은 질문에 두 번 답했는데 답이 달랐다.**

- **1차 — "제약을 제거한다".** `JwtAuthenticationFilter`가 `UserRepository`를 직접 참조하도록 ArchUnit 규칙 #4(`framework → business·data` 금지)를 열기로 했다. 포트를 끼우는 간접층보다 직참조가 읽기 쉽다는 판단이었다
- **2차 — "규칙을 계층 단위로 축소하면 어떤가"를 먼저 물었다.** 도메인 한정 예외(`data.user`만 허용) 대신 `framework → entity·repository 금지` 형태로 다시 그려 보자는 것이었다. 그런데 **그 형태가 옳다고 결론 내는 순간 직참조가 성립하지 않는다** — 필터가 쓸 수 있는 게 없어진다
- **결과: 규칙 #4를 손대지 않고 포트 방식으로 회귀.** 1차에서 기각했던 안이다

**되돌아왔지만 같은 자리가 아니다.** 1차에서 포트를 기각한 이유가 "클래스 2개와 새 패턴이 는다"였는데, 구현체를 따로 만들지 않고 **`UserService`가 `UserStatusChecker`를 구현**하면 새 클래스는 인터페이스 1개뿐이다. AGENTS §5의 트리 방향 문장도 고칠 필요가 없어졌다.

> **"예외를 열까"와 "규칙을 어떤 모양으로 좁힐까"는 다른 질문이다.** 앞의 질문만 던지면 답은 늘 "연다"로 수렴한다. 뒤를 먼저 물었더니 열 필요 자체가 사라졌다. 예외를 검토할 때 순서를 뒤집어 볼 것.

### ArchUnit 완화 지점은 예외 4건이 전부가 아니었다

예외 목록만 보고 있었는데 **`allowEmptyShould(true)`가 8개 규칙 전부에 붙어 있었다.** "검사 대상이 없으면 통과"라는 뜻이고, 2026-07-29 도입 당시엔 도메인 코드가 없어 필요했던 것이다.

REQ-07로 controller·service·repository·entity·dto가 모두 들어와 **이제 전부 실제 대상을 갖는다.** 껐는데도 8건이 그대로 통과하는 것을 확인하고(`tests=7`+`tests=1`, failures 0) `false`로 되돌렸다. `LAYER_DIRECTION`의 `withOptionalLayers`도 같이 껐다.

- **다만 이게 막는 것은 "`that()`이 0개를 매칭하는" 경우 하나뿐이다.** 패턴은 매칭되는데 키가 틀린 경우(07-29 슬라이스 괄호 사고)는 여전히 잡지 못한다. 구멍 두 개 중 하나만 막은 것이고, 나머지는 프로브가 유일한 수단이다
- 앞으로 새 규칙을 넣었는데 대상이 0개라 실패하면 **완화가 아니라 "규칙이 시기상조"라는 신호**로 읽는다

### 예외 4건 — 전부 "설계상 옳음"으로 확정

`business/auth → data/user`를 **임시에서 설계 결정으로 승격**했다. 소셜 자동가입은 본질적으로 user 프로비저닝이고, 이 참조를 없애려면 `business/user`에 진입점을 두어야 하는데 그러면 `business/auth → business/user` 예외가 대신 생겨 **개수는 그대로인 채 간접층만 는다.** PLAN-REQ-07 미결 1건이 닫혔다.

나머지 3건은 유지. 단 **`business.timeline` 예외는 대상 코드가 아직 0개라 REQ-12까지 공허하다** — 없애고 REQ-12에서 다시 넣는 안도 있었지만, §3에서 확정된 설계라 유지 쪽으로 갔다. 공허하다는 사실을 알고 두는 것과 모르고 두는 것은 다르다.

### 코드를 읽다 나온 함정 2건이 REQ-08 계획을 바꿨다

계획서를 쓰기 전에 기존 코드를 읽어서 나온 것들이다. 둘 다 **에러 없이 "정상처럼" 보이는** 종류다.

- **탈퇴 후 재로그인하면 유령 계정에 들어간다.** `AuthService.findOrCreateUser`가 `(provider, provider_user_id)`로 찾은 소셜 행의 유저를 **소프트 딜리트 여부를 보지 않고** 반환한다. `UNIQUE (provider, provider_user_id)` 때문에 새 계정 생성도 막혀 빠져나갈 길이 없다 → **탈퇴 시 소셜 행을 하드 삭제**하기로 했다. `user_social_accounts`에는 `deleted_at`이 없어(`V1__init.sql` 확인) 소프트 딜리트가 애초에 불가능하다. 재활성화 안은 "이전 반려동물·기록이 그대로 부활해 탈퇴의 의미가 사라진다"로, 재가입 거부 안은 "유예기간·복구 정책이 통째로 딸려온다"로 기각
- **탈퇴해도 access 토큰이 최대 30분 유효하다** → 위 D2로 이어졌다

### Notion 원본 대조 — 파생 요약만 봤으면 틀렸을 것 3건

`API I/F` DB의 user 3행을 직접 읽었다.

- **`PATCH /users/me`의 `null` 규약이 원본에 없다.** "변경할 필드만 포함"만 있고 `null`을 보냈을 때의 의미는 어디에도 없다 → **누락·`null` 모두 "변경 없음"**으로 정했다. `JsonNullable`/`Optional` 래핑은 기각 — 의존성이 늘기도 하지만 **원본에 근거 없는 규약을 만들면 그게 계약으로 굳는다.** AGENTS §5가 PATCH를 고른 이유(누락과 `null` 구분)와 어긋나므로 의도적 예외임을 계획서에 명시했다. 프로필 이미지 제거 수단은 미결로 올렸다
- **`GET /users/me` 응답에 `updated_at`이 없다.** 5개 필드뿐이다. 엔티티는 갖고 있으므로 DTO에 무심코 넣기 쉽다
- **소셜 계정 연결·해제 엔드포인트가 원본에 없다.** PROGRESS의 REQ-08 제목이 "(프로필 · 소셜 계정 연결)"이었는데 근거가 없어 "회원 탈퇴"로 고쳤다. 스펙 링크도 파생 요약(`api-list §2`) 대신 계획서로 바꿨다

### D5 — 탈퇴 시 refresh revoke를 하지 않는다

revoke하면 `business/user → data/auth` 참조가 생겨 **예외가 4→5로 는다.** D2로 탈퇴 계정은 어떤 access 토큰을 들고 와도 막히므로, refresh로 새 토큰을 받아도 결국 차단된다. 예외를 늘리지 않는 쪽을 택했다.

대가 둘 — `refresh_tokens`에 `revoked_at IS NULL` 행이 남고, **탈퇴한 사용자도 `/auth/refresh`가 200과 새 토큰을 반환한다**(그 토큰으로 API를 부르면 401). 후자는 클라이언트가 혼란스러울 수 있어 미결로 등록했다.

> **구현 시 주석이 필수다.** 근거가 없으면 다음 사람이 "revoke를 빠뜨렸다"고 보고 채워 넣어 예외를 늘린다.

## 2026-07-30

> 07-29 저녁부터 이어진 한 세션이 자정을 넘겼다. Phase 3은 커밋 날짜대로 07-29 절에 있다.

### 로그를 열어 보지 않았으면 못 잡았을 것 — `client_id` 평문 노출 (Phase 4)

Phase 4 완료 기준이 "로그에 토큰 원문이 남지 않는다(실제 로그를 눈으로 확인)"였다. **유효한 인가코드 없이도 이걸 검증할 방법이 있었다** — 잘못된 코드로 실제 카카오 왕복을 태우면(`KOE320` 수신) 요청 본문은 그대로 로깅 경로를 탄다.

그렇게 찍힌 로그를 보다가 **`client_id`(카카오 REST API 키)가 통째로 평문으로 남는 것**을 발견했다. `client_secret`은 마스킹 대상에 넣어 뒀는데 `client_id`는 "식별자니까 괜찮다"고 무의식적으로 넘긴 것이다. 마스킹 대상에 추가했다.

**교훈은 마스킹 목록이 아니라 검증 방식 쪽이다.** 마스킹 코드를 짜고 테스트를 통과시킨 상태에서도 이 누락은 남아 있었다. 실제 출력 한 줄을 눈으로 본 것이 유일한 발견 경로였다.

> ⚠️ **그 직후 같은 실수를 반복할 뻔했다.** 테스트 픽스처를 쓰면서 **로그에서 실제 키를 복사해 붙여 넣었다.** 2026-07-29 비밀번호 커밋과 정확히 같은 경로다(값을 설명하려다 실물이 따라온다). 더미로 교체하고 `.env` 값 기준 전수 스캔으로 레포에 실값이 없음을 확인했다. **재현 로그를 문서·테스트에 옮길 때는 붙여넣기 자체를 하지 말 것.**

### 마스킹 범위는 키 단위로 확정했다

전체 본문을 끄는 안을 기각했다. 카카오 오류 응답(`invalid_grant` / `KOE320` / `ip mismatched!`)이 **진단 정보의 거의 전부**인데 그걸 같이 잃는다. URL별 예외도 기각 — 대상이 늘 때마다 빠뜨린다.

키 **이름**으로 판단하므로 `{"code":-401,"msg":"ip mismatched!"}` 같은 숫자 진단값은 그대로 남는다. JSON의 `code`는 마스킹 대상에서 뺐고 form의 `code`(인가코드)만 넣었다 — 같은 이름이 한쪽에선 비밀이고 한쪽에선 진단값이다.

성공 응답의 `access_token`·`refresh_token` 경로는 **유효한 1회용 인가코드가 있어야** 재현되므로 실물로 못 태운다. 테스트로 고정했다(REQ-07-09).

### ArchUnit 규칙이 `business` 첫 클래스에서 무너졌다

`business/auth` → `framework/config`가 **도메인 간 참조 위반**으로 떨어졌다. 슬라이스 패턴 `com.petkok.*.(*)..`가 *의존 대상*에도 적용돼 `framework.config`가 "config" 슬라이스로 잡히기 때문이다.

**`@AnalyzeClasses` 범위를 좁히는 것으로는 못 막는다** — 그건 분석 *대상*을 줄일 뿐이고 문제는 **의존 방향의 끝**이다. 기존 주석이 "framework가 섞이면 규칙이 엉뚱해진다"며 범위 축소를 해법으로 적어 뒀는데, 절반만 맞았다.

이대로면 `ApiResponse`를 쓰는 **컨트롤러가 전부 위반**이라 규칙을 아예 못 쓴다. AGENTS §5가 `business`·`data` → `framework`를 허용하므로(금지 방향은 `ArchitectureTest`가 따로 잡는다) `framework`를 대상에서 제외했고, **프로브를 심어 교차 도메인은 여전히 잡히는 것을 확인**했다.

> 도메인 코드가 0개라 그동안 **빈 집합을 대상으로 통과**하고 있었다. 2026-07-29에 "구조 규칙은 일부러 위반을 심어 확인하라"를 계약으로 올려 뒀는데, 그 프로브도 *당시 존재하던* 코드 기준이었다. **첫 실사용자가 들어오는 시점에 규칙을 다시 의심해야 한다.**

### `PUBLIC_PATHS` 와일드카드 제거 — 계획보다 앞당겼다

원래 Phase 5 항목이지만 Phase 4가 `/auth/kakao`·`/auth/refresh`를 실제로 만드는 시점이라 함께 처리했다. AGENTS §5와 Notion §5·§7은 **이미 개별 나열을 계약으로 적고 있었고 코드만 뒤처져 있던** 상태다 — 문서가 계약을 현재 상태처럼 서술해 놓아서, 코드를 열어 보기 전에는 이미 지켜지는 줄 알았다.

사용자가 `/testgen`으로 넣어 둔 REQ-07-01·02·03이 이 항목을 정확히 빨간불로 잡고 있었고, 이 변경으로 초록불이 됐다.

### Phase 4에서 정한 것

- **`profile_image_url`은 저장 전 `https`로 정규화한다.** 카카오가 `http`로 내려주는데 그대로 두면 iOS ATS·Android cleartext에 막힌다. 정규화 지점을 OAuth 클라이언트에 둔 것은 provider별 차이를 그 계층에서 흡수하기 위해서다
- **`refresh_tokens.expires_at`은 JWT의 `exp`를 그대로 읽어 채운다.** 저장 쪽에서 TTL을 다시 계산하면 두 값이 어긋날 수 있다
- ⚠️ **`business/auth` → `data/user` ArchUnit 예외는 임시다.** 자동가입이 `users` 행을 만들어 생긴 참조이고, `data.common`·`timeline`·`framework` 세 예외와 달리 **"설계상 옳다"고 확정된 것이 아니다.** 계획서 「미결 질문」에 개선 방향 논의 건으로 등록했다

### 완료 기준 ①은 아직 못 채웠다

"로그인 왕복 성공"은 **유효한 인가코드가 필요하고 그건 사람이 브라우저로 로그인해야** 나온다. 자동화 대상이 아니라 수동 확인 항목으로 남겼다. 코드 경로는 전부 배선됐고 카카오까지 실제 요청이 나가는 것까지 확인했으므로 Phase 4를 완료로 표시했지만, **자동가입이 실제로 행을 만드는 것은 아직 아무도 보지 않았다.**

### 검증 계약을 구현보다 먼저 쓴 것이 결함 2건을 잡았다 (Phase 5)

`/testgen`으로 REQ-07-12~20을 먼저 쓰고 Phase 5를 구현했다. **그 순서 덕분에 나온 결함이 두 건이고, 둘 다 "겉보기엔 정상"인 종류였다.**

#### ① 로테이션이 이전 토큰과 **같은 문자열**을 발급하고 있었다

REQ-07-13("응답의 refresh 토큰은 제시된 것과 다르다")이 **바이트 단위로 동일한 토큰**을 잡았다. JWT `iat`/`exp`는 초 단위라 subject·type이 같으면 같은 초의 재발급 결과가 겹친다.

겹치면 새 토큰의 해시가 방금 revoke한 행과 충돌해 `uq_refresh_tokens_token_hash`를 위반하거나, **발급 즉시 revoke된 토큰을 클라이언트에 주게 된다.** `createRefreshToken`에 `jti`(랜덤 UUID)를 넣어 해소했다.

> **`jti`를 도로 빼는 프로브로 REQ-07-13이 실제로 잡는 것을 확인했다.** 안 그랬으면 "원래 잘 되던 것"과 구분이 안 된다 — 2026-07-29 ArchUnit 때 세운 계약을 테스트에도 그대로 적용했다.

#### ② 재사용 감지의 전체 revoke가 **롤백되고 있었다**

로컬 왕복에서 revoke된 토큰을 재제시하니 **401은 정상인데 다른 토큰이 살아 있었다.** `@Transactional` 기본 설정이라 뒤이어 던지는 `BusinessException`이 `revokeAllByUserId`까지 되돌린 것이다.

- **응답만 보면 완전히 정상이다.** 401 `INVALID_TOKEN`이 규격대로 나가므로 API 레벨 확인으로는 통과한다
- **목 기반 단위 테스트로는 원리적으로 잡히지 않는다.** 목은 롤백되지 않아 `verify(revokeAllByUserId)`가 초록불이다. REQ-07-16이 통과하는 상태에서 실제로는 무효화가 안 되고 있었다
- 남는 결과는 **공격자가 쥔 나머지 토큰만 조용히 살아남는 것**이다

`noRollbackFor = BusinessException.class`로 고쳤고, 애노테이션이 지워지는 것을 막으려고 REQ-07-23(애노테이션 고정)을 추가했다. 동작 자체는 테스트로 못 잡으니 **애노테이션을 고정하고 실제 롤백 여부는 DB 왕복으로 확인**하는 이중 구성이다.

> **교훈은 "예외를 던지는 트랜잭션에서 남겨야 하는 쓰기"라는 패턴 자체다.** "무효화하고 거절한다"는 모양이 Spring 기본값과 정면으로 충돌하는데, 충돌 결과가 **조용하다.** 앞으로 pet 소유권·회원 탈퇴에서 같은 모양이 또 나온다.
>
> 그래서 이 항목은 로그에 두지 않고 **AGENTS §5 계약으로 승격했다**(`f34d708`). 매 세션 자동 로드되지 않으면 다음에 똑같이 깨진다.

### 로컬 왕복으로 확인한 것 — 단위 테스트가 못 보던 자리

사용자 1명 + refresh 행을 `petkok_local`에 심고 실제 HTTP로 태웠다(확인 후 삭제).

| 확인 | 결과 |
| --- | --- |
| `POST /auth/refresh` 유효 토큰 | 200 · 옛 행 `revoked_at` 설정(**더티체킹 실증**) · 새 행 INSERT · 저장 해시 = 응답 토큰의 SHA-256 |
| revoke된 토큰 재제시 | 401 `INVALID_TOKEN` + 사용자 전체 revoke (위 ② 수정 후) |
| `DELETE /auth/logout` 무토큰 | 401 `UNAUTHORIZED` — **`PUBLIC_PATHS` 개별 나열이 실효**임을 확인 |
| 〃 유효 access 토큰 | 204, 본문 0바이트 |
| 〃 refresh 토큰을 access 자리에 | 401 — `isAccessToken` 방어 실효 |
| 로그에 토큰 원문 | 0건 |

**Phase 3에서 "아직 검증되지 않았다"고 적어 둔 항목들이 여기서 닫혔다** — JPA 더티체킹으로 `revoked_at`이 실제 UPDATE 되는 것, `findByTokenHash`가 revoke된 행도 반환하는 것(재사용 감지가 동작했으므로 간접 확인).

### 함정 — 같은 컬럼에 9시간 어긋난 값이 섞인다

검증 데이터를 SQL로 심다가 발견했다. 앱이 쓴 행은 `14:39`, `now()`로 심은 행은 `23:38`이었다.

`application.yml`의 `hibernate.jdbc.time_zone: UTC` 때문에 **앱은 UTC로 저장하고 DB 기본값 `now()`는 세션 타임존(KST)으로 저장한다.** 앱끼리는 일관적이라 버그는 아니지만, **SQL로 직접 심은 행은 앱 기준으로 9시간 미래**다. 만료 검증 픽스처를 이렇게 만들면 "만료됐어야 하는데 안 됐다"로 나타난다. 픽스처는 `now() at time zone 'UTC'`를 쓴다.

### logout의 revoke 범위 — 스펙 두 줄이 어긋나 한쪽을 골랐다

`api-list.md` §1은 "access 토큰으로 사용자를 식별해 refresh revoke", § refresh 토큰 저장소는 "해당 토큰 `revoked_at` 설정"이다. **Request Body가 없어 특정 토큰을 지목할 수단이 없으므로** 사용자 전체 revoke로 고정했다 — 대가는 **기기별 로그아웃 불가**다. Notion API I/F 원본 확인이 필요한 건으로 계획서 미결에 올렸다.

### 거부 경로 2건은 `INVALID_TOKEN`으로 통일했다

만료된 토큰 / 저장소에 없는 해시. `/testgen` 시점엔 근거가 없어 미결로 뒀다가 Phase 5 구현에서 정했다. **거부 사유를 구분해 알려 주면 공격자에게 "이 토큰은 존재하기는 한다"는 정보가 샌다.** REQ-07-21·22로 승격.

> 만료 판정은 **저장된 행의 `expires_at`**으로 한다. 만료된 JWT는 파싱 자체가 `ExpiredJwtException`으로 터져서 `getExpiresAt`을 부를 수 없다 — 테스트 픽스처가 여기서 한 번 걸렸다.

### Phase 6이 Phase 5 안에서 끝났다 (계획-실제 이탈)

계획서는 Phase 6(검증 체계)을 별도 단계로 뒀는데, `/testgen`이 REQ-07-12~23을 Phase 5 착수 **전에** 작성하면서 완료 기준("토큰 만료·로테이션·재사용 감지 테스트 통과 + CI green")이 그대로 충족됐다. 2026-07-29에 "Phase 6의 절반이 REQ-14 중에 끝나 있었다"고 적었던 것의 나머지 절반이다.

**단계를 쪼갠 전제가 바뀐 것이다** — 계획 당시엔 "구현 후 테스트"였는데 `/testgen`을 도입하면서 순서가 뒤집혔다. 남은 Phase가 없어 REQ-07은 **Phase 1~6 완료**지만, **Phase 4 완료 기준 ①(카카오 로그인 왕복)이 수동 확인으로 남아 있어** 상태는 🟡로 둔다.

## 2026-07-29

### Kakao 콘솔 왕복을 서버 코드보다 먼저 검증했다

Phase 4 코드를 한 줄도 쓰지 않은 상태에서 curl로 **인가코드 → 토큰 교환 → `/v2/user/me`** 왕복을 먼저 통과시켰다. 콘솔 설정 문제와 서버 코드 문제가 섞이면 원인 분리가 어려워지기 때문이다. 실제로 이 왕복에서 함정 3건이 나왔고 **그중 하나는 코드 안에서 만났으면 키 문제로 오진했을 것**이다.

전제가 하나 깨졌다. 이 검증은 "설정값이 맞는지"만 확인할 생각이었는데, **수신 필드가 설계 전제를 바꾸는 것**이 두 건 나왔다.

#### 함정 — 「허용 IP 주소」는 `kapi`에만 걸린다

토큰 교환(`kauth.kakao.com`)은 **성공**하는데 `/v2/user/me`(`kapi.kakao.com`)만 `{"code":-401,"msg":"ip mismatched!"}`로 거부됐다. 두 호스트에 서로 다른 정책이 걸려 있다.

**진단 규칙: 토큰이 발급됐다면 키 3개는 정상이다.** 그 뒤에 나오는 401은 키가 아니라 IP·동의항목 쪽을 의심한다.

콘솔에 값이 **하나라도** 등록돼 있으면 allowlist가 켜진 것으로 동작한다. 배포 시 서버 egress IP를 등록하거나 이 설정을 비워야 하고, **고정 egress IP가 없는 실행 환경이면 운영 로그인이 통째로 막힌다.**

#### `email`은 내려오지 않는다 — 설계 전제가 바뀐다

`kakao_account.email`이 `null`이었다. 이메일은 **비즈니스 앱 전환 + 검수**가 필요한 동의항목이라 현재 앱에서는 받을 수 없다.

`users.email`이 NULL 허용이라 자동가입 자체는 막히지 않는다. 문제는 그다음이다 — **Kakao 사용자는 `email`이 항상 비어 있다고 보고 설계해야 한다.** 식별자는 `(provider, provider_user_id)` 하나뿐이고 `idx_users_email`은 당분간 빈 인덱스다. 이메일을 전제로 한 계정 병합·중복 검사는 지금 만들 수 없다.

테이블 정의서가 이 컬럼을 **"소셜 provider 제공 여부 불확실"** 로 남겨 뒀던 것이 이걸로 확정됐다.

#### `profile_image_url`이 `http://` 스킴으로 온다

`http://k.kakaocdn.net/...`. `varchar(500)`이라 저장은 문제없지만 그대로 클라이언트에 내려주면 **iOS ATS·Android cleartext 정책에 막힌다.** 같은 경로를 `https`로 요청하면 200이 나오는 것을 확인했으므로 정규화는 가능하다 — 저장 시점에 스킴을 바꿀지는 Phase 4 판단으로 남겼다.

### 검증 스크립트 버그 하나당 재로그인 한 번

**인가 코드는 1회용이고 약 10분 만료다.** 스크립트가 토큰 교환까지 성공한 뒤 뒷단에서 죽으면 코드는 이미 소비돼 **그 코드로는 재시도가 불가능하다.** 실제로 두 번 태웠다.

- **`[[ 조건 ]] && 명령`은 조건이 거짓일 때 종료코드 1이라 `set -e` 아래에서 스크립트를 죽인다.** `if`로 풀어 써야 한다
- **`python3 -c '...'` 안의 f-string에 `\"`를 쓰면 bash 작은따옴표가 백슬래시를 그대로 넘겨 `SyntaxError`가 난다.** 스크립트는 heredoc으로, 데이터는 argv로 넘기는 형태가 안전하다

두 번째는 **실패 경로만 테스트해서 놓쳤다** — 성공 경로에만 있는 코드였다. 1회용 외부 토큰을 쓰는 스크립트는 **샘플 데이터로 전 경로를 먼저 드라이런**하고 실물을 태운다.

> 스크립트는 `.env`의 실키를 읽으므로 레포에 넣지 않았다(스크래치패드에만 존재). 다시 필요하면 키를 인자로만 받는 형태로 재작성한다.

### Notion 역반영 — 테이블 정의서 `users`

레포가 실측으로 확정한 내용을 Notion에 되돌렸다(AGENTS §0의 "레포가 앞서는 경우"). `email` 설명·`idx_users_email` 목적을 고치고 Kakao 실측 주석을 붙였다.

**「설계」 섹션의 DB·API 탭과 달리 「테이블 정의서」는 일반 페이지라 API로 수정된다.** 편집 후 **옛 문자열로 역프로브**를 걸어(`No matches found` 확인) 실제 저장을 검증했다 — 치환이 성공을 반환하고도 아무것도 안 바뀌는 경우가 있기 때문이다.

> 개요의 **"PostgreSQL 15+"는 손대지 않았다.** 로컬 17.10 / Supabase 17.6으로 확정된 상태라 여기도 어긋나 있지만 이번 델타 밖이다 — 별도로 역반영이 필요하다.

### Kakao 설정 골격을 Phase 4보다 먼저 놓았다

**서버가 필요한 값은 3개뿐이다** — `client-id`(REST API 키) · `client-secret` · `redirect-uri`. 커스텀 플로우이기 때문이다: 클라이언트가 인가코드를 받아 서버에 넘기고 서버는 `kauth.kakao.com/oauth/token`으로 교환만 한다. **`spring-security-oauth2-client`의 리다이렉트 로그인이 아니라서** 그쪽 설정 트리(`registration`/`provider`)가 통째로 필요 없다.

콘솔의 앱 키 4종 중 **REST API 키**다. 네이티브·JavaScript 키와 헷갈리기 쉽고, **Admin 키는 전권이라 서버에도 두지 않는다.**

R2와 같은 방침으로 더미 기본값을 뒀다 — Phase 4 전까지 기동을 막지 않기 위해서다. 기동 확인했고, `petkok_local`은 "up to date"로 재적용도 멱등이었다.

**`client-secret`만 기본값이 빈 문자열이다.** 콘솔에서 "사용함"으로 켠 경우에만 필요한 값인데, **빈 값을 실어 보내면 카카오가 거부한다** — 구현 시 비어 있으면 파라미터 자체를 생략해야 한다. Phase 4 계약이라 계획서 "제약·함정"에 올렸다.

`.env.example`의 Kakao 항목은 **주석 처리**로 넣었다. 바로 위에서 승격한 "`KEY=`(빈 값)은 기본값을 무력화한다" 계약을 그대로 적용한 첫 사례다 — 빈 값으로 뒀다면 더미 기본값이 죽어 기동이 막혔을 것이다.

**미결 해소 — Kakao 앱 등록 완료, 키 보유.** Phase 4 착수 조건이 충족됐다(콘솔 설정 검증은 별도 진행 중).

### 프로파일별 스키마 분리 (REQ-07 Phase 2) — 값의 출처를 한 곳으로 모았다

`local`/`dev`/`prod`가 각각 `petkok_local`/`petkok_dev`/`petkok_prod`를 쓴다. 계획서가 이 Phase의 **최대 리스크**로 꼽은 것은 "Flyway와 Hibernate 중 한쪽만 스키마를 지정하는" 사고였다 — 테이블이 생긴 곳과 조회하는 곳이 갈리는데 **에러 없이 "테이블이 없다"로만 나타난다.**

**두 곳에 같은 값을 적는 대신 `db.schema` 한 곳을 만들고 양쪽이 그것을 참조하게 했다.** 프로파일 파일이 값을 정하고 `application.yml`이 Flyway·Hibernate 양쪽에 배선한다. 한쪽만 바뀌는 상태가 구조적으로 불가능해진다. `application.yml`에는 기본값을 두지 않아 프로파일이 값을 빠뜨리면 **기동 즉시** 실패한다.

**세 프로파일을 전부 실제로 띄워 확인했다.** 같은 로컬 DB에 프로파일만 바꿔 붙였더니 스키마 3개가 각각 생성되고 V1이 적용됐다(dev·prod는 확인 후 삭제). **prod까지 띄운 이유는 `application-prod.yml`의 오타가 배포 시점에야 드러나기 때문**이다 — 지금 1분이면 끝나는 확인이다.

`petkok_local`은 통째로 지우고 새 설정 경로로 재생성했다. 기존 스키마는 소유자가 `pg_database_owner`였는데(새로 만든 것은 `root`) — `public`을 **rename**한 흔적이다. 설정을 거치지 않고 만들어진 상태라 완료 기준 ②의 "V1이 **새로** 적용된다"를 충족하지 못한다고 보고 다시 만들었다. 행 0건이라 잃을 것이 없었고 덤프는 미리 떠 뒀다.

> Flyway가 만든 스키마는 `flyway_schema_history`에 version이 빈 행(`"petkok_local"`)으로 기록된다. `clean` 시 스키마 자체를 지울지 판단하는 표식이다.

### ⚠️ 로컬 DB 비밀번호가 public 레포에 커밋돼 있었다

`5c7313b`(오늘 16:25, 이미 push됨)의 "URI에서 `@`가 잘린다" 함정 설명에 **실제 로컬 비밀번호를 예시로 그대로 썼다.** 문서는 더미 값으로 교체했지만 **git 이력에는 남아 있고 레포는 public이다.**

Phase 2 완료 기준 ④가 "레포에 실제 비밀번호가 없다"였는데, 검증하려고 찾다가 나왔다. **기준을 형식적으로 체크했으면 놓쳤을 것이다** — `.gitignore`와 `.env.example`만 보면 통과로 보인다.

> **함정 사례를 적을 때 실제 값을 쓰지 말 것.** 재현 로그를 붙여넣는 흐름에서 자연스럽게 섞여 들어간다. 여기서도 "왜"를 남기려다 값까지 함께 남았다.

**결론: 이력은 그대로 둔다(2026-07-29 결정).** localhost 전용 개발 DB라 외부에서 닿지 않고, `main`을 force push 하는 비용이 얻는 것보다 크다. 다만 **같은 비밀번호를 다른 곳에 재사용했다면 그쪽이 실제 위험**이다 — 노출된 값은 이미 공개돼 있고 이 결정으로 회수되지 않는다.

### 잘못된 기본값 2건 — 둘 다 "조용히 틀리는" 종류였다

- **`application-local.yml`이 Phase 1 실측과 어긋나 있었다.** `5432`/`postgres`/`postgres`로 남아 있어 **다른 버전의 빈 DB에 조용히 붙을** 값이었다. `5433`/`root`로 맞추고 **`DB_PASSWORD`는 기본값을 없앴다** — `pg_hba`의 `host` 라인이 이제 scram이라 어떤 더미 값도 통하지 않는데, 기본값이 있으면 "비밀번호 틀림"으로만 보여 원인 파악이 늦어진다. 값이 없어 기동이 막히는 편이 낫다
- **`.env`의 `KEY=`(빈 값)은 "미설정"이 아니다.** `set -a && . ./.env`로 주입하면 **빈 문자열**이 환경변수로 들어가고 Spring의 `${VAR:기본값}`은 미정의일 때만 기본값을 쓴다. README가 권하는 실행 경로에서 그대로 밟는 함정인데, `.env.example`은 `R2_*`를 포함해 여러 키를 빈 값으로 두고 있었다(= 더미 기본값이 무력화된다). 두 문서에 경고를 넣었다

### 로컬 DB 구축 — 운영과 메이저 버전을 맞췄다

`petkok` DB에 Flyway `V1__init.sql`을 적용하고 앱 기동까지 확인했다. 테이블 9개 + `flyway_schema_history`, `GET /actuator/health` 200, `GET /api/v1/users/me` 401.

**버전은 두 번 바뀌었다.** 처음엔 로컬 18.4를 그대로 쓰다가 Flyway 경고를 보고 15로 통일하려 했고, **Supabase 대시보드를 실제로 확인하니 17.6**이어서 최종적으로 17로 갔다. 로컬은 17.10 — 마이너 차이는 무방하고 메이저가 중요하다. REQ-07이 `V2__refresh_tokens`를 추가하는 작업이라 그 전에 맞춰 둔 것이 요점이다.

> **Supabase는 아직 PostgreSQL 18을 지원하지 않는다.** 당초 2026-01 목표였으나 넘겼고, 2026-05 메인테이너 답변이 *"not very soon, but eventually in 2026"*이다. 로컬을 18로 두면 운영보다 앞서게 되므로 선택지가 아니었다.

`root` 롤이 존재하지 않아 만들었다. **`public` 스키마 소유자가 `pg_database_owner`라서 DB 소유권만 넘기면** Flyway가 테이블을 만들 `CREATE` 권한이 자동으로 따라온다 — 별도 `GRANT`가 필요 없다.

### Flyway 경고의 원인은 DB 버전이 아니었다

18.4에서 뜨던 경고를 "18이 너무 새것"으로 읽었는데 **틀렸다.** 문구가 답이었다.

```
The latest supported version of PostgreSQL is 16.
```

Spring Boot 3.3.5 BOM이 고정하는 **Flyway 10.10.0이 16까지만 검증**돼 있던 것이라, 17로 맞춘 뒤에도 같은 경고가 그대로 났다. 추측으로 넘기지 않고 17에서 10.10.0으로 한 번 띄워 확인한 뒤 `extra["flyway.version"] = "10.22.0"`으로 상향했고 경고가 사라졌다. 11/12/13도 있으나 부트 BOM과의 호환 위험을 줄여 10.x 최신을 골랐다 — 부트를 올릴 때 재검토 대상이다.

**교훈: 경고 문구가 원인을 정확히 말하고 있으면 그걸 먼저 읽어야 한다.** 버전 정합이라는 그럴듯한 가설에 끌려 두 번 헛돌았다.

### scram-sha-256 전환 — `host`만 바꾸고 `local`은 남겼다

`pg_hba.conf`가 전부 `trust`라 **비밀번호가 아예 검증되지 않는 상태**였다. `host` 라인 4개만 `scram-sha-256`으로 바꾸고 리로드했다. 틀린 비밀번호가 실제로 거부되는 것까지 확인했다.

**`local`(유닉스 소켓)은 의도적으로 `trust`로 남겼다.** `postgres`와 `yjkim` 두 슈퍼유저에 비밀번호가 아예 없어서(`rolpassword IS NULL`), 소켓까지 scram으로 바꾸면 관리 접속이 통째로 잠긴다. 소켓을 잠그려면 두 롤에 비밀번호를 먼저 설정해야 한다.

같은 이유로 **`postgres` 계정은 TCP로 붙지 못했다** — 소켓(`trust`)은 되는데 TCP(`scram`)에서 `fe_sendauth: no password supplied`. GUI 클라이언트는 대개 TCP라 여기서 걸린다.

### 함정 — 비밀번호의 `@`가 URI에서 잘려 "비밀번호 틀림"으로 오진된다

`ab@00`처럼 `@`가 든 비밀번호를 연결 URI에 그대로 넣으면 `@`가 호스트 구분자로 파싱된다.

| 방식 | 결과 |
| --- | --- |
| `PGPASSWORD=...` + `-U root` | ✅ |
| `postgresql://root:ab@00@localhost:5433/petkok` | ❌ `could not translate host name "00@localhost"` |
| `postgresql://root:ab%4000@localhost:5433/petkok` | ✅ |

클라이언트가 `@`에서 잘라 앞부분만 보내면 **인증 실패로 나타나** 비밀번호가 틀린 것으로 오인한다. 서버 쪽 해시는 멀쩡한데도 그렇다. URI를 쓸 땐 `%40`으로 인코딩하거나, 비밀번호에 `@`를 쓰지 않는 편이 낫다.

### 함정 — macOS에 `timeout`이 없다

`timeout 180 ./gradlew bootRun ... | grep ...`이 조용히 아무것도 출력하지 않았다. `command not found: timeout`(coreutils의 `gtimeout`)으로 즉시 죽었는데, **파이프 때문에 종료코드가 `grep`의 것이 되어 0으로 보였다.** 어제 `| tail`로 빌드 실패를 놓친 것과 정확히 같은 구조를 하루 만에 반복했다.

→ CLAUDE.md 로컬 검증 절의 파이프 항목을 보강했다.

### README가 이행 후에도 `global/`로 남아 있었다

PR #10에서 패키지를 3분할로 옮겼는데 **`AGENTS.md`만 고치고 README를 놓쳤다.** 어제 하루 종일 다룬 "문서-코드 drift"가 같은 자리에서 또 나온 셈이다. 3분할 구조로 교체하면서 `business/{도메인}`↔`data/{도메인}` 이름 일치 규칙도 명시했다.

README에 **로컬 DB 준비 절**을 신설했다 — 롤·DB 생성, 포트 확인, `pg_hba` 전환 범위, `.env` 주입. 2026-07-27에 존재하지 않는 `.env.example`을 가리켜 지웠던 자리를 실물 기준으로 다시 채웠다.

### 워킹트리에 남아 있던 문서 작업을 되돌릴 뻔했다

사용자가 작성한 `docs/specs/db-schema.md`(252줄)와 출처 우선순위 갱신이 **checkpoint(PR #11) 이전 버전 위에** 얹혀 있었다. 그대로 커밋했다면 어제 승격한 §4 SHA 대조·`| tail` 계약이 조용히 사라질 상황이었다. 백업 후 `origin/main` 기준 새 브랜치로 옮겨 3-way 병합했고 양쪽이 모두 살아 있음을 확인했다.

> **브랜치를 갈아타기 전에 워킹트리 변경이 어느 커밋 위에 얹혀 있는지 확인할 것.** 오래된 베이스 위의 편집을 최신 브랜치에 그대로 올리면 그 사이 들어간 변경이 되돌려진다.

### ArchUnit 도입 — 규칙이 조용히 공허해질 뻔했다

`src/test`를 신설하고 `archunit-junit5:1.4.2`로 §13 규칙을 실제 코드로 옮겼다. 규칙 8개가 실행되며 CI 게이트에 포함된다. **패키지 구조 결정의 근거였던 규칙이 이제 검증된다.**

**검증 과정에서 스케치의 함정이 드러났다.** 노션 §13에는 `com.petkok.*.(*)..`로 적어 뒀는데, 구현하면서 "business|data만 매칭되게" 명확히 한다고 `com.petkok.(business|data).(*)..`로 바꿨다. 그랬더니 **괄호가 캡처 그룹이라 트리 이름까지 슬라이스 키에 포함**됐고, `business/feeding`과 `data/feeding`이 서로 다른 슬라이스가 되어 **같은 도메인 참조까지 위반으로 잡혔다.**

```
Slice business - feeding depends on Slice data - feeding   ← 허용돼야 하는데 위반
Slice business - feeding depends on Slice data - shed      ← 이것만 위반이어야 정상
```

일부러 위반을 심는 프로브(같은 도메인 참조 1건 + 교차 도메인 참조 1건)를 만들어 확인했기에 잡았다. **통과/실패만 봐서는 알 수 없었다** — 규칙이 "더 엄격하게" 동작하고 있었으므로 도메인 코드가 들어오기 전까지는 아무 증상이 없다가, 첫 도메인에서 정상 코드가 막혔을 것이다.

올바른 패턴은 첫 세그먼트를 캡처하지 않는 `com.petkok.*.(*)..`이고, 이때는 `framework.config`가 "config" 슬라이스로 섞이므로 **`@AnalyzeClasses` 범위를 좁힌 별도 테스트 클래스**(`DomainBoundaryTest`)가 필요하다. 수정 후 같은 프로브로 재확인했다 — 같은 도메인 참조는 통과하고 교차 도메인만 1건 잡힌다.

> **구조 규칙은 일부러 위반을 심어 잡히는지 확인해야 한다.** 빈 집합을 대상으로 통과하는 규칙은 통과해도 아무 의미가 없고, 잘못 쓴 규칙은 반대 방향으로 조용히 동작한다. CLAUDE.md에 계약으로 올렸다.

Checkstyle이 테스트 소스에서 10건 걸렸다(한글 상수명 `ConstantName`, `HideUtilityClassConstructor`). AGENTS.md §7이 "억제하지 말고 소스를 규칙에 맞게 수정"이라 필드명을 대문자 스네이크로 바꾸고 private 생성자를 넣었다. 사람이 읽을 설명은 `.as(...)`가 담당한다 — 위반 메시지에 그대로 출력되므로 가독성 손실이 없다.

### 나머지 미결 2건 결론

- **`data/timeline`은 만들지 않는다.** `business/timeline`만 두고 DTO가 실제로 필요해지는 REQ-12에서 추가한다. 지금 만들면 빈 패키지가 되고 git이 추적하지 못한다
- **`ApiUri`는 도입하지 않는다.** 컨트롤러가 0개라 상수화할 경로가 없다. 근거 없이 만들면 실제 사용처와 어긋난 채 굳는다. auth 컨트롤러가 생기는 REQ-07에서 "경로 문자열이 2곳 이상에서 반복되는가"를 보고 판단한다

### `local` 소켓 scram 전환은 아직 불가

`postgres`에는 비밀번호가 설정됐지만 **`yjkim`은 여전히 없다.** 소켓까지 scram으로 바꾸면 Postgres.app이 쓰는 관리 계정이 잠긴다. `host`(TCP)만 scram인 현재 상태가 실용적 균형이며, 앱이 쓰는 경로는 TCP라 실질 보호는 이미 걸려 있다.

### 계획-실제 이탈 — REQ-07의 절반이 REQ-14 중에 끝나 있었다

작업이 끝난 뒤 `PLAN-REQ-07`을 대조하니 **계획서가 이미 낡아 있었다.** REQ-14(패키지 구조)를 하다가 필요해서 처리한 것들이 실은 REQ-07의 범위였다.

- **Phase 1(로컬 DB 기동) 완료** — 완료 기준(`bootRun` 정상 기동 + `flyway_schema_history` V1 성공 행)을 그대로 충족한다. 계획서는 이걸 auth 착수의 선행 조건으로 잡아 뒀는데, 패키지 이행 검증(PLAN-REQ-14 Phase 4)을 하려다 먼저 끝냈다
- **Phase 6(검증 체계)의 절반 완료** — "`src/test` 신설 + ArchUnit 활성화"가 REQ-14 미결을 닫으면서 처리됐다. 남은 것은 auth 로직 테스트뿐이다
- **미결 2건 해소** — 로컬 PostgreSQL 접속 정보(Postgres.app / 5433 / `root`), PostgreSQL 버전(17)

계획서 배경의 "② 검증 수단이 없다"도 절반만 맞는 상태가 됐다 — `src/test`는 생겼지만 **도메인 로직 테스트는 여전히 0개**다. 작성 시점의 문제 인식이라 문단은 남기고 현재 상태를 주석으로 덧붙였다.

REQ-07 상태를 ⏸ → 🟡로 올렸다. **auth는 이제 "착수 전"이 아니라 "진행 중"이다.**

### PLAN-REQ-14 Phase 4가 닫혔다

어제 "완료 기준 미정"으로 보류했던 기동 확인이 여기서 해소됐다. 신구조(`business`/`data`/`framework`)에서 컴포넌트 스캔·빈 등록·시큐리티 필터 체인이 모두 정상 동작한다.

### Notion 역반영 3건 — 하나는 반영할 것이 없었다

계획서·문서에 흩어져 있던 역반영 대기 건을 처리했다.

| 건 | 결과 |
| --- | --- |
| 테이블 정의서 개요 `PostgreSQL 15+` | → `PostgreSQL 17`(운영 17.6 / 로컬 17.10) |
| 「소스 구조」 §7 RestClient → RestTemplate | **이미 RestTemplate 기준이었다.** 페이지 전체에 `RestClient` 표기가 없다 — PLAN-REQ-07 「결정」 표의 기술이 낡아 있었던 것이고, 그 칸을 정정했다 |
| 테이블 정의서 `refresh_tokens` | §10 신설(V2 DDL 확정 후) |

덤으로 「소스 구조」의 실제 drift도 정리했다 — §13 ArchUnit "아직 컴파일해 보지 않았다" → 도입 완료 + 괄호 캡처 함정, §12 프로파일 3분리 + `db.schema` 배선, 「구현 노트」에 2026-07-29 절 신설.

> **함정 — `old_str`과 `new_str`이 같은 no-op 프로브는 무용하다.** CLAUDE.md에 "다른 문자열로 프로브를 걸라"고 적혀 있는데 같은 문자열로 걸었더니 **매칭이 없는데도 성공을 반환**했다. 즉 이 방식으로는 반영 여부를 전혀 알 수 없다. 옛 문자열을 **다른 값으로** 걸어 `No matches found`를 받는 방식만 실제 증거가 된다.

### 엔티티 배치로 ArchUnit 예외를 0건으로 막았다 (Phase 3)

`V2__refresh_tokens.sql` + `User`/`UserSocialAccount`/`RefreshToken`.

`auth`가 `users`를 건드리는 구조라 배치를 잘못하면 도메인 간 참조 금지에 걸린다. **`UserSocialAccount`는 쓰는 쪽이 auth지만 `data/user`에 뒀다** — `User`를 `@ManyToOne`으로 참조하므로 `data/auth`에 두면 그 자체가 위반이다. `RefreshToken.user_id`는 **연관관계 없이 생 `UUID` 컬럼**으로 매핑했다. 토큰 행에서 User로 탐색할 일이 없어 잃는 것이 없고, DB의 FK 제약은 그대로 있다.

**`validate`가 실제로 무는지 프로브로 확인했다.** 계획서가 Phase 1·2에서 두 번 "`@Entity`가 0개라 아무것도 검증하지 않는다"고 적어 둔 항목이라 통과만 보고 넘기지 않았다. 없는 컬럼을 심으니 `Schema-validation: missing column ... in table [users]`로 기동이 막혔다.

**Phase 2의 미검증 항목도 여기서 닫혔다** — Hibernate가 Flyway와 같은 스키마(`petkok_local`)를 본다. 달랐다면 `missing column`이 아니라 `missing table`로 떨어졌을 것이다.

> **적용이 끝난 마이그레이션은 주석 한 글자도 못 고친다.** Flyway가 체크섬을 대조하므로(`validateOnMigrate` 기본 `true`) `V1__init.sql`을 수정하면 이미 V1을 적용한 DB에서 기동이 막힌다. '다음 단계'에 등재된 `condition_tag` 주석 4→7종 정정이 정확히 여기 걸린다 — **"주석이니까 안전하다"고 판단하면 안 된다.** 선택지를 db-schema.md에 적어 뒀다.

## 2026-07-28

### 노션 현행화 C단계 — 설계 문서와 구현이 갈린 4건

「소스 구조 / 아키텍처 설계」의 패키지 트리·global 표를 실제 코드와 대조해 맞췄다. 예외 서브클래스 트리(`NotFoundException`/`ForbiddenException`/`ConflictException`)는 채택된 적이 없고 실제는 `ErrorCode` enum + 단일 `BusinessException`, 베이스 엔티티는 2단계가 아니라 3단계, `global/util/` 30개와 `RestTemplateConfig`·`RestTemplateLoggingInterceptor`는 문서에 아예 없었다.

**범위 밖에서 더 심각한 걸 발견했다.** §5 표의 `SecurityConfig` 행이 아직 `/auth/**` permitAll로 적혀 있었다 — 2026-07-27에 확정한 "와일드카드 금지, 개별 경로 나열"과 정면으로 충돌한다. 하루 전에 정한 결정이 문서 다른 절에 반영 안 된 상태였다. 같이 고쳤다.

「다음 단계」 체크리스트는 REQ-01·04·05가 끝났는데도 전부 미체크였다. 완료 처리하면서 **코드 쪽 미해결 2건을 명시로 올렸다** — `SecurityConfig.PUBLIC_PATHS`가 아직 `/api/v1/auth/**`인 것, `V1__init.sql`의 condition_tag 주석이 7종이 아니라 4종인 것. 둘 다 REQ-07에서 처리한다.

### 함정 — 노션 코드블록이 이스케이프 문자열을 그대로 저장하고 있었다

§2 패키지 트리 블록이 박스문자·한글을 `├`, `도` 같은 **텍스트로** 담고 있었다. 노션에서도 깨져 보였을 것이다. 문제는 이 상태에서 부분 수정이 **불가능**하다는 것 — `update_content`의 `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없다(항상 실제 문자로 디코딩된다). 박스문자는 "`u251c` → ASCII 마커 → 실제 문자" 2단계 치환으로 우회했지만, 한글은 코드포인트 조합이 75개를 넘어 비현실적이었다. **결국 `replace_content` 전체 교체가 유일한 해법이었다.**

여기서 시간을 태운 진짜 원인은 따로 있다. **`old_str`과 `new_str`을 같은 문자열로 써 놓고 성공 응답을 받아 고쳤다고 착각한 것.** 이스케이프를 다루다 보니 양쪽이 같은 값이 되기 쉬웠고 도구는 성공을 반환한다. no-op 프로브(다른 문자열로 매칭 시도)를 걸고 나서야 저장 형태를 확정할 수 있었다. 같은 원인으로 §4 표의 🦎가 깨져 있던 것도 함께 복구했다.

→ 이 3건은 **`CLAUDE.md`로 승격**했다. 모르면 "수정이 안 먹는다"를 권한 문제로 오진한다.

### 패키지 구조 재설계 확정 — `business` / `data` / `framework`

기존 `global` + `domain` 단일 트리에서 3분할로 바꾸기로 했다. 도메인 코드가 0개인 지금이 이행 비용 최저점이다.

**결정의 핵심은 `data` 밑을 도메인별로 쪼갠 것이고, 근거는 ArchUnit이다.** 처음 제안은 `data/entity`·`data/domain`에 전부 평면으로 모으는 안이었는데, 그러면 `FeedingLog`와 `ShedRecord`가 같은 패키지라 Slices가 나뉘지 않아 **도메인 간 참조 금지 규칙이 성립하지 않는다.** 대안으로 클래스명 접두사 기반 커스텀 `ArchCondition`을 검토했으나 이름 규칙이 흔들리면 같이 무너져 기각했다. `business/{도메인}`과 `data/{도메인}`이 같은 이름을 쓰면 `slices().matching("com.petkok.*.(*)..")` 한 줄로 끝난다 — 같은 도메인끼리는 같은 슬라이스라 허용되고 교차 참조만 걸린다.

같이 정한 것:

- **DTO 패키지는 `domain`이 아니라 `dto`.** `domain`은 DDD에서 핵심 비즈니스 모델을 뜻해 entity와 혼동된다
- **repository는 entity 옆**(`data/{도메인}/repository`). 반환 타입이 Entity라 결합이 강하고, `business`에 두면 business 트리가 JPA를 알게 되면서 `business → data` 단방향이 깨져 규칙을 한 줄로 못 쓴다
- `uri`·`const`는 `data`가 아니다 — URI는 API 계약이라 `framework/constant`, 도메인 전용 값은 `data/{도메인}/enums`
- 베이스 엔티티는 `framework`가 아니라 `data/common/entity`. framework는 JPA 매핑 규약을 알지 않아야 한다
- `JwtAuthenticationFilter`는 `framework/processor/filter`. `security/jwt`에 두는 편이 응집도는 높지만, 요청 파이프라인에 끼어드는 위치가 성격을 결정한다고 봤다
- **`processor/resolver/`는 두지 않는다** — `@CurrentUser`가 `@AuthenticationPrincipal` 메타 애노테이션이라 ArgumentResolver가 실제로 존재하지 않는다(코드 확인). 설계상 있을 법해서 문서에 남으면 없는 클래스를 찾게 된다

노션 「소스 구조」에 **§13 구조 강제(ArchUnit)** 절을 신설해 규칙 5종을 코드로 적었다. **컴파일·실행해 본 적 없는 스케치**이고 문서에도 그렇게 명시했다.

> ⚠️ `business/{도메인}`과 `data/{도메인}`의 이름이 어긋나면 ArchUnit 규칙이 **에러 없이 조용히 무력화**된다 — 서로 다른 슬라이스로 잡혀 규칙은 통과하는데 경계는 안 지켜진다. AGENTS.md §3에 계약으로 등재했다.

### 이행 완료 (PR #10)

54개 파일을 옮겼다(`PetKokApplication`은 제자리). **`package`·`import` 외 변경 라인 0건**을 기계적으로 확인했다 — `git diff --find-renames -M -U0 | grep -vE '^[+-](package |import |$)'` 출력이 비어야 정상이고, 실제로 비었다. 게이트 4종(spotlessApply / build -x test / checkstyleMain -PciStrict / spotlessCheck) 통과.

**컴파일 에러 5건이 났고, 원인은 자리를 바꾼 3개였다.** `RestTemplateLoggingInterceptor`(config→processor/interceptor) · `GlobalExceptionHandler`(exception→processor/handler) · `JwtAuthenticationFilter`(security/jwt→processor/filter). 셋 다 **이전에는 의존 대상과 같은 패키지라 `import` 문이 아예 없었고**, 패키지를 벗어나면서 각각 `RestTemplateLoggingInterceptor`·`BusinessException`/`ErrorCode`·`JwtTokenProvider` import가 필요해졌다. 추가한 import 4줄이 이번 이행의 유일한 내용 변경이다.

> 같은 패키지 안에서 이동하면 안 나던 에러다. **패키지를 가로지르는 이동은 "옮기면 끝"이 아니라 암묵적 동일 패키지 참조가 드러나는 지점**이라고 보는 게 맞다.

빈 패키지(`framework/constant`, `processor/aspect`, `processor/converter`)는 만들지 않았다 — 넣을 클래스가 없고 git이 빈 디렉터리를 추적하지 못한다. 설계는 노션 §2에 남겨 뒀다. `business/`와 `data/{도메인}`도 도메인 코드가 들어올 때 생긴다. 현재 실재하는 것은 `data/common/entity`뿐이다.

### 사고 — PR #8이 낡은 SHA 기준으로 머지돼 커밋 3개가 누락됐다

푸시는 원격 브랜치(`aef47db`)까지 정상 도달했는데 **GitHub이 5분 넘게 PR head를 갱신하지 않았고**(`1f4297d` 고정, `mergeable: null`), 그 상태에서 머지가 실행되면서 그날 커밋 3개(07-27 기록 · 구조 확정 · 07-28 기록)가 `main`에 들어가지 못했다. `git ls-remote`와 `GET /repos/.../branches/...`는 `aef47db`를 반환하는데 `GET /pulls/8`만 `1f4297d`을 반환하는 상태였다.

내 쪽 원인은 **stale한 체크 결과를 통과로 읽은 것**이다. `gh pr checks`가 반환한 초록불은 전날(07-27) 실행분이었는데 SHA를 대조하지 않고 "CI 통과"로 판단했다. 같은 브랜치로 PR #9를 새로 열어 복구했고, 이후 PR #9·#10은 **머지 직전에 PR head와 로컬 HEAD를 대조**한 뒤 진행했다.

→ 이 대조 절차는 AGENTS.md §4에 계약으로 올렸다.

### 함정 — `| tail`이 gradle 종료코드를 가려 실패를 통과로 오보고했다

`./gradlew build -q 2>&1 | tail -15 && echo OK` 형태로 묶어 돌렸더니 파이프라인 종료코드가 `tail`의 것이 되어, **컴파일이 깨졌는데도 `=== build OK ===`가 출력됐다.** 위의 컴파일 에러 5건을 한 번 놓친 원인이다. 게이트는 파이프 없이 `set -e`로 각각 돌려야 한다.

→ CLAUDE.md 로컬 검증 절에 등재했다.

### 함정 — Notion이 인라인 코드 주변 굵게를 `****`로 재정규화한다

`**\`data\`**`를 쓰면 저장 시 `**** \`data\` ****` 형태로 바뀌어, 다음번 `update_content`의 `old_str`이 매칭되지 않는다. 변형을 추측으로 반복하다 결국 `fetch`로 실제 저장 형태를 확인하고서야 고쳤다 — 오늘 CLAUDE.md에 올린 함정("부분 치환이 계속 no-match면 저장 형태를 의심")과 정확히 같은 사례를 스스로 반복한 것이다. **인라인 코드와 굵게를 붙여 쓰지 않는 편이 안전하다.**

## 2026-07-27

이 파일(`docs/PROGRESS.md`)을 신설하면서 2026-07-06 baseline 이후 전체 이력을 소급 정리했다.
레포에 흩어진 진행 기록은 없었다 — README의 "다음 단계"는 로드맵, `docs/specs/api-list.md`는 스펙 문서라 이관 대상이 아니다.

**Notion이 설계의 1차 출처임을 계약으로 등재 (AGENTS §0 신설).**

MySQL 전환 계획을 세우던 중 Notion에 **ADR-002(DB 엔진 선택 — Supabase PostgreSQL, Status: Accepted, 2026-06-29)** 가 이미 있는 것을 발견했다. 레포 문서만 보고 판단한 결과 확정된 아키텍처 결정과 어긋난 계획을 만든 것이다. 원인은 **요구사항·설계·API 계약·ADR의 원본이 레포가 아니라 Notion에 있다는 사실이 어디에도 적혀 있지 않았던 것.** AGENTS.md 맨 앞에 출처 우선순위 표를 두어 매 세션 로드되게 했다.

대조에서 나온 방향별 격차:

- **Notion이 최신 → 레포를 고침**: `api-list.md`의 39개 엔드포인트를 Notion API I/F 기준으로 정합. 기록 도메인 경로가 복수형(`/diaries`)이었으나 원본은 단수(`/diary`), `logout`은 `POST`가 아니라 `DELETE`, presigned URL은 `/photos/upload-url`이 아니라 `/photos/presigned-url`. 근거 없이 추가돼 있던 엔드포인트 6개(social-accounts 3종·`/weights/chart`·photos 상세·PATCH) 제거
- **레포가 최신 → Notion에 역반영 필요**: refresh 토큰 저장소(Notion은 "이후 결정 DB/Redis"로 열려 있음), Gradle wrapper 포함 여부
- **Notion 내부 불일치**: 설계 탭 DDL 블록이 소스 구조 문서보다 뒤처져 있다 — `updated_at` 트리거가 남아 있고 `activity_type`에 `HANDLING`이 빠져 있다. 둘 다 소스 구조 문서에서는 확정 반영된 항목이라 **DDL 블록만 갱신 누락.** 그래서 AGENTS §0에 "DDL 블록과 소스 구조 문서가 다르면 소스 구조 문서를 신뢰"를 명시했다

**차단 조건 2건 해소 — 답은 DB 그리드가 아니라 행 본문에 있었다.**

Notion API I/F를 표(그리드)로만 읽었을 때는 `/auth/refresh`·`/auth/logout`이 똑같이 `🔒 인증 필요 = YES`였다. **각 행을 열어보니 서로 달랐다.**

- `/auth/refresh` → 본문 "🔓 **인증 불필요**", Request Body `{refresh_token}`. 체크박스가 오기였다 (Notion에서 해제)
- `/auth/logout` → 본문 "🔒 인증 필요, **Request Body 없음**", 204. body가 없으니 **access 토큰이 유일한 사용자 식별 수단**이다

→ **`PUBLIC_PATHS`는 와일드카드를 버리고 개별 경로로 나열한다**(`/auth/kakao`, `/auth/refresh`, `/actuator/health`). 현행 `/api/v1/auth/**`를 두면 `/auth/logout`이 무인증 노출된다. 2026-07-23 결정의 "logout은 refresh를 body로 받으니 access 없이 동작한다"는 **전제가 틀렸다** — 원칙은 유효하고 적용만 바뀌었다. AGENTS §5 갱신.

`condition_tag`도 **7개로 확정**. Notion ERD 설계가 "공통(정상/활발) + 게코 전용(거식/탈피도와줌/탈피완료/거꾸리/구토)"으로 명시하고, 테이블 정의서의 `is_assisted → '탈피도와줌' 연계` 규칙도 같은 값을 참조한다. `V1__init.sql` 주석의 4개가 누락이다.

**교훈**: Notion 데이터베이스는 **그리드 속성만 보면 안 된다.** 속성과 행 본문이 어긋날 수 있고, 이번엔 본문이 맞았다. api-list.md에 "체크박스보다 행 본문이 정확하다"를 명시했다.

**Notion 역반영 (이번에 수행)**: refresh 저장소 결정(소스 구조 §7) · Gradle wrapper 포함(구현 노트) · `updated_at` 트리거 제거(테이블 정의서 3곳 + 공통 설계 원칙) · `activity_type` HANDLING(테이블 정의서·ERD) · FastAPI 잔재 정리(테이블 정의서·ERD·시스템 컨텍스트 분석 11곳) · `/auth/refresh` 체크박스 해제 + 로테이션 응답 스펙 추가. 설계 탭 DDL 코드 블록은 API로 쓸 수 없는 객체(탭)라 사용자가 직접 수정했다. ADR·제약사항 DB는 역사적 기록이라 손대지 않았다.

**계약 승격 — RestTemplate 지적 2건을 AGENTS §5로 올렸다.** 응답 버퍼링 필수 / `getStatusCode()` 원본 위임. 둘 다 "모르는 사람이 고치면 조용히 재발"하는 종류라 로그에 묻으면 안 된다 — PROGRESS.md는 필요할 때만 읽히지만 AGENTS.md는 매 세션 로드된다.

**README stale 수정.** README "실행 §1"이 `gradle-wrapper.jar`가 저장소에 없다고 안내하고 있었다(IntelliJ 동기화 또는 `gradle wrapper` 실행 요구). 실제로는 `10f2ad2`(2026-07-07)에서 8.10.2 wrapper를 커밋해 추적 중이고 AGENTS §2는 "포함되어 있다"로 맞게 적혀 있어, 두 문서가 서로 모순이었다. wrapper 준비 절을 걷어내고 이하 번호를 당겼다.

같은 줄에 있던 **`.env.example` 참조도 함께 제거**했다 — 그 파일은 존재한 적이 없다. 없는 파일을 가리키는 안내는 신규 참여자·에이전트가 탐색에 시간을 쓰게 만든다.

### 함정 — Notion 탭 페이지는 API로 수정할 수 없다

「설계」 섹션의 DB·API 탭 본문은 `notion-update-page`가 **`validation_error: Object ... is not a page or database`** 로 거부한다. `fetch`로 읽히고 URL도 페이지처럼 생겼지만 쓰기가 안 되는 객체(탭)다. 같은 프로젝트의 일반 페이지(테이블 정의서·ERD 설계·소스 구조)는 정상 수정된다.

- 증상만 보면 권한 문제로 오인하기 쉽다. **읽기는 되는데 쓰기만 막히면 객체 타입을 의심할 것**
- 우회: 같은 내용이 실린 일반 페이지를 고치고, 탭 안의 원본(이번엔 DDL 코드 블록·HTTP 상태 코드 표)은 **사람이 직접 수정**해야 한다
- 이번에는 DDL 블록 4곳을 사용자가 직접 처리했다

### 브랜치 — 머지된 브랜치에 새 작업을 얹지 않는다

작업을 시작한 `docs/api-list`는 PR #7로 **이미 머지된 브랜치**였다. 여기에 커밋을 쌓아 새 PR을 열면 머지된 PR 페이지까지 어수선해진다. `docs/progress-and-mysql-plan`을 새로 파고 `origin/main` 위로 리베이스해 PR #8을 열었다.

> 로컬 `docs/api-list`에 리베이스 전 커밋 3개가 고아로 남아 있다(원격에는 없음). 내용은 새 브랜치에 모두 보존돼 있으므로 `git branch -f docs/api-list origin/docs/api-list`로 정리하면 된다.

### 다음 — 노션 현행화 C단계 (내일)

Notion 소스 구조 §5 패키지 트리와 실제 구현이 갈린 4건. 구현 노트가 일부 설명하고 있으나 본문 표·트리를 먼저 읽으면 오해한다.

1. 예외 서브클래스 트리(`NotFoundException`/`ForbiddenException`/`ConflictException`) — 실제로는 `ErrorCode` enum + 단일 `BusinessException`
2. 베이스 엔티티가 **3단계**(`BaseCreatedEntity` → `BaseTimeEntity` → `BaseSoftDeleteEntity`) — 표에는 2단계만
3. `global/util/` 계층 통째 누락 (REQ-04에서 이식한 30개)
4. `RestTemplateConfig` · `RestTemplateLoggingInterceptor` 누락 (REQ-05)

---

## 2026-07-23

### 공통 유틸리티 이식 (REQ-04)

date/json/list/map/number/string/encrypt/file/os/spring/http 30개를 `global/util`로 이식했다.
**왜 지금** — 도메인(auth→user→pet…) 착수 전에 변환·검증 보일러플레이트를 공통 계층에서 흡수하기 위함. 도메인마다 같은 걸 재발명하면 되돌리기 어렵다.

함정: 이식 코드가 commons-lang3 / guava / commons-compress를 쓰고 있어 의존성 3개가 따라 들어왔다. 유틸 하나만 골라 가져오는 게 아니라 묶음 이식이었기 때문.

### RestTemplate 로깅 인터셉터 (REQ-05)

Kakao OAuth 등 외부 API 연동 대비. 코드 리뷰에서 지적 3건이 나왔고, 그중 둘은 모르면 재발하는 종류다.

- **응답 본문 버퍼링이 필수다.** 인터셉터가 로깅하며 응답 스트림을 소비하면 호출부가 본문을 다시 읽지 못한다. `BufferingClientHttpResponseWrapper`로 복사한 뒤 로깅한다 — 이 래핑을 걷어내면 "응답이 비어 있다"로 조용히 깨진다
- **`getStatusCode()`는 원본 `HttpStatusCode`를 위임해야 한다.** `HttpStatus.valueOf(int)`로 재변환하면 비표준 상태코드 수신 시 `IllegalArgumentException`이 터진다 (예: Cloudflare 520). R2가 Cloudflare이므로 가상의 위험이 아니다
- 민감 헤더 5종(Authorization/Cookie 등) 마스킹 — AGENTS §5 로깅 규칙 준수. `MaskingUtil`에 자격증명용 메서드를 추가했다
- 오타 수정: contentType 비교 `"text/htmnl"` → `"text/html"`

**연기**: 응답 **본문**의 토큰 노출 마스킹은 처리하지 않았다. 어떤 필드가 민감한지는 연동 대상에 따라 다르므로 domain 단계 Kakao 연동 시점에 실제 응답을 보고 결정한다.

`ErrorCode.EXTERNAL_API_ERROR`(502)를 함께 추가했지만 호출부는 아직 없다 — domain 단계에서 쓰인다.

### CLAUDE.md 로컬 검증 실태 보완

에이전트가 잘못된 완료 보고를 하는 문제가 실제로 있었다. 원인 두 가지를 명시했다.

- **`src/test`가 없다.** `./gradlew test`는 통과하지만 검증된 것이 0이다. "테스트 통과"로 보고하면 안 된다 — 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐
- **lefthook은 자동 설치되지 않는다.** 새 클론·새 워크트리에서 `lefthook install`을 안 하면 `.git/hooks`가 비어 로컬 검증이 전부 무력화되고, CI `spotlessCheck`에서 터진다

### API 설계 초안 + 미결정 3건 확정 (REQ-06)

`V1__init.sql` 9개 테이블과 `ErrorCode`를 기준으로 엔드포인트를 정리하면서 미결정 3건을 남겼고, 같은 날 셋 다 확정했다.

**① refresh 토큰 저장소 → DB(`refresh_tokens`, V2 마이그레이션)**
stateless refresh 기각. 로그아웃·회원 탈퇴 시 즉시 무효화가 불가능하기 때문. 토큰 원문은 저장하지 않고 `SHA256Util` 해시로 보관한다 — DB 유출 시 그대로 재사용 가능해지므로. 무효화는 `revoked_at`으로 표현하며 소프트 딜리트가 아니다(그래서 `BaseCreatedEntity` 상속).

**② 로테이션 적용 + 재사용 감지**
`/auth/refresh` 호출마다 새 refresh를 발급하고 기존 것을 즉시 revoke한다. 정상 클라이언트는 revoke된 토큰을 다시 보낼 일이 없으므로, 재제시되면 탈취로 간주하고 해당 사용자 토큰 전체를 revoke한다.
- **재사용 감지 전용 ErrorCode 기각** — 기존 `INVALID_TOKEN`(401)으로 충분하다. 공격자에게 탐지 여부를 알려줄 이유가 없다
- **만료 행 정리 배치는 범위 제외** — 스케줄러 도입 결정이 함께 필요하다. 운영 부담이 실제로 보이는 시점에 별도로 다룬다

**③ 공개 경로 범위 + PATCH 통일 → AGENTS §5로 승격**
두 규칙 모두 어기면 회귀하므로 로그에 묻지 않고 컨벤션으로 올렸다.
- `/api/v1/auth/**`는 permitAll이므로 **토큰 없이 호출되는 엔드포인트만** 둔다. 인증이 필요한 기능을 `/auth/` 아래 두면 무인증 노출된다 — 소셜 계정 연결을 `/users/me/social-accounts`에 배치한 것이 이 원칙의 적용이다. (`/auth/logout`은 refresh를 body로 받아 revoke하므로 access 토큰 없이 동작 → permitAll이 맞다)
- 수정은 **PATCH로 통일, PUT 기각.** 클라이언트가 일부 필드만 보내는 경우가 대부분인데 PUT으로 받으면 누락 필드와 `null` 덮어쓰기 의도를 구분할 수 없다

---

## 2026-07-07

`develop-convention` 저장소의 컨벤션을 그룹 단위로 이식했다 (그룹 2·3·5·6).

### 빌드/품질 도구 (REQ-02)

- **gradle wrapper가 깨져 있었다.** 스켈레톤에 `gradle-wrapper.jar`가 없어 `./gradlew`가 스텁 에러로 종료 — 로컬·CI 빌드 자체가 불가능한 상태였다. 8.10.2 정식 wrapper를 복원하고 저장소에 커밋했다(이후 클론에서 즉시 빌드 가능)
- **Checkstyle 게이트를 둘로 나눴다.** 기본 실행은 경고만, `-PciStrict` 시 `maxWarnings=0`. 개발 중 매번 막히지 않으면서 머지 게이트는 유지하기 위함
- **`checkstyle.xml`의 SuppressionFilter를 제거했다.** 억제 없이 strict를 유지하고, 구조적 예외는 소스 수정으로 해결한다는 원칙. 실제로 아래 두 건이 이 원칙에 따라 소스 수정으로 처리됐다
  - `GlobalExceptionHandler`: 수동 `static final Logger log` 필드가 `ConstantName`(UPPER_SNAKE) 규칙에 걸림 → Lombok `@Slf4j`로 교체. 이후 프로젝트 전체 로깅이 `@Slf4j`로 통일됐다
  - `PetKokApplication`: `HideUtilityClassConstructor` 위반 → `@SpringBootApplication(proxyBeanMethods = false)` + private 생성자. CGLIB 프록시를 끄는 대가가 따라온다
- JaCoCo는 측정만, 게이트 없음 (`src/test`가 없으므로 현시점 의미 없음)
- 포맷 일괄 적용(`spotlessApply`, 23개 파일)은 **로직 커밋과 분리**했다. 4-space→2-space 전면 변경이라 섞이면 diff가 읽히지 않는다

### 워크플로우·AI 진입점 (REQ-03)

- lefthook `commit-msg`의 Conventional Commits 검증을 **shell/grep 정규식**으로 구현했다. commitlint를 쓰면 Node 툴체인을 들여야 하는데, Java 단일 스택에 런타임을 하나 더 얹을 이유가 없다
- **ArchUnit 스텝은 보류.** 레이어 의존 검사 대상이 `global`뿐이라 검사할 게 없다. feature 도메인 도입 시점(REQ-07~)에 활성화한다
- AGENTS.md 이식 시 원본에 있던 존재하지 않는 문서 링크(CONTRIBUTING / starter-decisions / `.claude/rules`)를 제거하고 README.md를 1차 출처로 지정했다. 없는 문서를 가리키면 에이전트가 탐색에 시간을 쓴다
- `.gitignore`에 `.idea/` 추가 — IntelliJ가 IDE 개인 설정을 index에 올리고 있었다

---

## 2026-07-06

baseline 스켈레톤 커밋. 이후 패키지·프로젝트명을 `com.petkok` / `PetKok`으로 리네임.
`global` 공통 계층(config·common·security·exception) + 베이스 엔티티 3단(`BaseCreatedEntity` → `BaseTimeEntity` → `BaseSoftDeleteEntity`) + Flyway `V1__init.sql` 9개 테이블까지가 범위. 도메인 없음.
