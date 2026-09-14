# PLAN-REQ-17 · `_at`/`_date` 컬럼 네이밍 정합화 (measured_at → measured_date, taken_at → taken_date)

> 출처: 현재 세션 대화 · 작성: 2026-09-11 · 상태: ✅ 완료 (Phase 1·2 전부 완료, 2026-09-14 `/testrun` 재확인 · PR #53 `main` 병합 완료)

## 배경

DB 네이밍 컨벤션을 "`_at` = 시각(`timestamptz`) · `_date` = 날짜(`date`)"로 못 박고 싶다는 요청에서
전수조사를 했다. 결과: 이 규칙에 어긋나는 컬럼은 **`weight_logs.measured_at`, `photos.taken_at`
2건뿐**이었다(나머지 `_at` 7종은 전부 `timestamptz`, `_date` 3종은 전부 `date`).

두 컬럼 다 우연이 아니다 — REQ-16/[ADR-0002](../adr/ADR-0002-time-handling-timestamptz.md)에서
"시각 개념이 없는 date 컬럼"으로 **타입은 이미 의도적으로 확정**됐고, 그때는 컬럼명까지 바꾸는
논의는 없었다(`docs/PROGRESS.md:446`— 처음엔 5개 목록에 `measured_at`까지만 있다가 나중에
`taken_at`이 추가된 걸로 보아 이름 정합성은 그때도 안건이 아니었다). 이번엔 타입이 아니라
**이름을 규칙에 맞춘다.**

## 범위

**포함**
- `weight_logs.measured_at` → `measured_date`, `photos.taken_at` → `taken_date` 컬럼 리네임
- 인덱스 `idx_weight_pet_measured_at` → `idx_weight_pet_measured_date` 동반 리네임(미결 ② 결정)
- 엔티티(`WeightLog.measuredAt`·`Photo.takenAt`) · DTO 4종(`WeightCreateRequest`·`WeightUpdateRequest`·
  `WeightResponse`·`WeightCursor`) · `PhotoCreateRequest`·`PhotoResponse`·`PhotoSummary`
  (`framework/port`) · Repository JPQL(`WeightLogRepository`) · 서비스(`WeightService`·`PhotoService`)
  필드명 동반 변경
- API 응답 snake_case 키 변경(`measured_at`→`measured_date`, `taken_at`→`taken_date`) —
  운영 배포가 없어 breaking 취급 없이 진행(미결 ① 결정)
- 문서 갱신: `docs/specs/db-schema.md`, `docs/specs/api-list.md`
- Notion 원본 역반영: 「테이블 정의서」(개요 행·weight_logs/photos 컬럼·인덱스 행) ·
  「API I/F」 4행(체중 목록·체중 기록·갤러리 목록·사진 메타데이터 저장) —
  **코드 변경보다 먼저 진행(미결 ③ 결정), 2026-09-11 완료.** DB 탭 DDL 코드블록은 탭 객체라
  API로 못 고쳐 사람 손 대기(선례: `docs/specs/db-schema.md:290`)

**제외**
- 컬럼 타입 변경 없음 — `date`/`LocalDate` 그대로. REQ-16/ADR-0002가 이미 확정한 부분이라
  다시 열지 않는다
- API 버전 전략(v1 유지 + v2 신설 등) 설계 — 미결 ①이 "운영 배포 없음 · breaking 아님"으로
  닫혀 애초에 범위에 들 근거가 없어졌다
- `birthday`·`adoption_date`·`entry_date`·`shed_date` 등 이미 규칙과 맞는 컬럼 — 손대지 않는다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 네이밍 규칙 | `_at` 접미사는 반드시 `timestamptz`, `_date` 접미사는 반드시 `date`로 1:1 대응시킨다 | 전수조사로 어긋난 케이스가 실제 존재했고(2건), 이름만 보고 타입을 오추정하는 사고를 막는다 | 규칙 없이 컬럼별 개별 판단 — REQ-16 때처럼 "타입은 맞는데 이름은 방치"가 반복된다 |
| 처리 방식 | `measured_at`·`taken_at`을 `measured_date`·`taken_date`로 **리네임**한다(타입 유지) | 사용자 결정 — 이름 그대로 두고 문서 예외로만 남기는 대안 대신 실제 정합화를 선택 | 이름 그대로 두고 ADR에 "예외 2건"으로만 문서화 |
| ADR 승격 여부 | 승격하지 않음 — 계획서 `## 결정` 표로 충분 | 기각 근거(REQ-16 시점엔 이름 정합성이 안건이 아니었다)가 이 계획서·PROGRESS 이력에 이미 남아 재발굴 필요가 없고, 리네임 자체는 되돌리기 쉬운 국소 결정 | ADR-0002에 추가 문단으로 덧붙이기 — ADR 본문은 불변 원칙 위반이라 기각 |

## 미결 질문

전부 닫힘 (2026-09-11, 사용자 확인).

- [x] ① 운영/배포 클라이언트가 이미 `measured_at`·`taken_at` 응답 필드명에 의존하고 있는가
      → **라이브된 배포가 없다.** 의존하는 클라이언트 없음 — breaking change 취급 불필요
- [x] ② `idx_weight_pet_measured_at` 인덱스명도 함께 바꿀지 → **바꾼다**
      (`idx_weight_pet_measured_date`)
- [x] ③ Notion 역반영을 코드 병합 전/후 중 언제 할지 → **먼저.** AGENTS.md §0 원칙(Notion이 원본)대로
      진행 — 2026-09-11 완료(아래 Phase 1)

## 작업 단계

- [x] **Phase 1 — Notion 원본 역반영 + 레포 파생 문서 갱신** — 완료 2026-09-11 (코드 변경 없음, 커밋 없음)
      완료 기준: 「테이블 정의서」 4곳(개요 행·weight_logs 컬럼/인덱스 행·photos 컬럼 행) ·
      「API I/F」 4행(체중 목록·체중 기록·갤러리 목록·사진 메타데이터 저장)을
      `measured_date`·`taken_date`로 갱신 후 `fetch` 재조회로 반영 확인 ✅ ·
      `docs/specs/db-schema.md`·`api-list.md` 동반 갱신 ✅.
      **닫지 못한 것** — 「설계」→ DB 탭의 DDL 코드블록(탭 객체라 API로 수정 불가, 사람 손 대기 —
      `docs/specs/db-schema.md:290`의 선례와 동일 케이스)
- [x] **Phase 2 — DB 마이그레이션 + 코드 리네임** — 완료 2026-09-11(코드) · 2026-09-14(`/testrun` 재확인)
      완료 기준: `V5__rename_at_to_date_columns.sql`(컬럼 2개 + 인덱스 1개) 적용 후
      `./gradlew build -x test` 통과 ✅, `WeightLog`·`Photo` 및 관련 DTO·Repository·Service 전부
      새 필드명으로 컴파일 ✅, 기존 REQ-10·REQ-11 검증 계약(측정/사진 관련 케이스)이 새 필드명으로 통과 ✅
      (`WeightServiceTest` 21·`WeightControllerWebMvcTest` 10·`WeightDtoContractTest` 1·
      `PhotoServiceTest` 25·`PhotoControllerWebMvcTest` 5, 2026-09-14 `--rerun` 재확인)

## 검증 계약

> 작성: 2026-09-11 · 스펙: 이 계획서(별도 스펙 문서 없음) · 검증: `/testrun REQ-17`

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-17-01 | `V5__rename_at_to_date_columns.sql` | `weight_logs.measured_at`→`measured_date`, `photos.taken_at`→`taken_date` 컬럼을 rename한다 | 정상 | PLAN §작업 단계 Phase 2 완료 기준 — "`V5__rename_at_to_date_columns.sql`(컬럼 2개 + 인덱스 1개) 적용" | 2 | ✅ |
| REQ-17-02 | `V5__rename_at_to_date_columns.sql` | `idx_weight_pet_measured_at` 인덱스를 `idx_weight_pet_measured_date`로 rename한다 | 정상 | PLAN §범위—포함 — "인덱스 `idx_weight_pet_measured_at` → `idx_weight_pet_measured_date` 동반 리네임(미결 ② 결정)" | 2 | ✅ (최초 실행 실패 — 테스트 정규식 결함, `/testrun` (a) 수정 후 통과) |
| REQ-17-03 | `WeightLog` 엔티티 | 구 필드명 `measuredAt`이 더 이상 존재하지 않는다 | 불변식 | PLAN §범위—포함 — "엔티티(`WeightLog.measuredAt`·`Photo.takenAt`) ... 필드명 동반 변경" | 2 | ✅ |
| REQ-17-04 | `Photo` 엔티티 | 구 필드명 `takenAt`이 더 이상 존재하지 않는다 | 불변식 | 상동 | 2 | ✅ |

**부수 수정(신규 ID 없음)** — `TimeFieldTypeContractTest`(REQ-16-07, PLAN-REQ-16 소유)의
`DATE_FIELDS`를 `WeightLog "measuredAt"` → `"measuredDate"`로 갱신하고, REQ-11이 `Photo` 엔티티를
들여왔을 때 빠져 있던 `Photo "takenDate"`를 추가했다. REQ-16-07의 불변식("날짜 필드는 여전히
LocalDate다") 자체는 바뀌지 않았지만 REQ-17 Phase 2 전까지는 새 필드명이 없어 **의도적으로
빨간불**이다.

## 제약·함정

- **인덱스 리네임 없이 컬럼만 바꾸면 인덱스명과 컬럼명이 어긋난 채 남는다** — 미결 ②가 "바꾼다"로
  닫혔으니 Phase 2의 `V5` 마이그레이션에 인덱스 리네임을 반드시 포함할 것
- **테스트명의 `[REQ-NN-MM]` 계약 ID는 이 리네임과 무관하게 그대로 둔다** — REQ-10-*, REQ-11-*
  케이스의 필드 참조(`measuredAt`→`measuredDate` 등)만 갱신하고 ID 문자열 자체는 건드리지 않는다
  (건드리면 `/testrun` 필터가 0건 매칭으로 깨진다)
- **`ddl-auto: validate`는 컬럼 존재만 보고 이름 변경 누락을 감지하지 못하는 축은 아니다** —
  이름이 안 맞으면 엔티티가 존재하지 않는 컬럼을 찾아 기동 자체가 막힌다(이 점은 오히려 안전망).
  다만 리네임 순서를 "엔티티 먼저 · 마이그레이션 나중"으로 하면 기동이 막히므로
  **마이그레이션(V5) 적용 후에 엔티티를 고친다**
