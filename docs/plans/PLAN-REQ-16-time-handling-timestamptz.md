# PLAN-REQ-16 · 시각 처리 규약 — `timestamptz` 전환 (저장 = 순간 · 노출·계산 = KST 고정)

> 출처: 2026-08-28 세션 (REQ-10 Phase 2 머지 직후) · 작성: 2026-08-28 · 상태: 🟡 진행 (Phase 0~3 완료 · **Phase 4 역반영 실행 완료 2026-08-31** — ⏸ Notion 탭 2곳이 API 로 수정 불가라 완료 기준 미충족)

## 배경

**응답의 시각에 타임존 정보가 없다.** `logged_at`·`created_at` 이 `2026-06-30T09:00:00` 으로 나가는데, 원본(Notion `API I/F`)의 예시는 전부 `"2026-06-30T18:00:00Z"` 다. 클라이언트는 이 값이 UTC 인지 로컬인지 알 방법이 없다. REQ-10 Phase 2 에서 발견해 미결로 올렸다.

파고들자 문제가 하나가 아니었다.

- **저장 형식이 타임존을 담지 못한다.** 19개 `timestamp` 컬럼(오프셋 없음)에 앱은 UTC 로 쓰고 DB `default now()` 는 세션 타임존으로 쓴다. 2026-07-30 에 **같은 컬럼에 9시간 어긋난 값이 섞인 것**을 실제로 목격했다(`14:39` vs `23:38`). 앱끼리는 일관적이라 버그로 드러나지 않지만, **값만 보고는 어느 타임존인지 알 수 없다**
- **"어느 시각인가"가 코드 바깥에 안 적혀 있다.** `hibernate.jdbc.time_zone: UTC` 한 줄이 유일한 근거다. 이 줄을 모르는 사람이 SQL 로 데이터를 심으면 9시간 어긋난 행이 조용히 들어간다
- **달력 판정의 기준이 정해져 있지 않다.** REQ-10 Phase 3 이후로 "당일 시간만 허용"(급여) · "미래 날짜 불가"(다이어리) · 거식 스트릭 **일수** · 탈피 예측 **주기**가 전부 달력 경계를 필요로 하는데, 어느 타임존의 자정을 쓰는지 원본에도 코드에도 없다. **REQ-10 Phase 3 을 이 결정 없이 시작할 수 없다**

## 범위

**포함**

- **V3 마이그레이션** — `timestamp` 19개 → `timestamptz`. 대상: `users`(3) · `user_social_accounts`(1) · `pets`(3) · `diary_entries`(2) · `feeding_logs`(2) · `activity_logs`(2) · `weight_logs`(1) · `shed_records`(1) · `photos`(1) · `refresh_tokens`(3)
- **엔티티 시각 타입 전환** — `BaseCreatedEntity.createdAt` · `BaseTimeEntity.updatedAt` · `BaseSoftDeleteEntity.deletedAt` · `RefreshToken.expiresAt`·`revokedAt` · `ActivityLog.loggedAt` (6개 필드, `LocalDateTime` → D2 가 정하는 타입)
- **직렬화 규약** — 응답 시각은 KST 오프셋을 붙여 내보낸다(`2026-06-30T18:00:00+09:00`). ~~`framework/config/JacksonConfig` 한 곳~~ **두 곳이 됐다**(2026-08-28) — 직렬화는 `JacksonConfig` 의 `timeZone(...)` 한 줄이지만, **역직렬화(D9)는 설정으로 얻을 수 없어** `processor/converter/OffsetDateTimeDeserializer` 가 필요했다
- **역직렬화 규약** — 오프셋이 있는 요청 값(`Z` · `+09:00`)을 그 순간 그대로 해석
- **계산 기준 고정** — 달력 판정(당일·미래·일수·주기)은 전부 `Asia/Seoul`. `ZoneId` 상수를 `framework/constant` 에 한 곳
- **`now` 획득 경로 통일** — `AuthService` 2곳의 `LocalDateTime.now()` (D5)
- 기존 REQ 테스트 갱신 — 시각을 다루는 7파일(auth 2 · user 1 · pet 1 · weight 1 · activity 2)

**제외**

- **계정별 타임존** — 2026-08-28 대화에서 제안됐다가 같은 대화에서 철회됐다("그냥 KST로 고정가자"). 사용자 설정 컬럼 · `PATCH /users/me` 확장 · 요청 스코프 TZ 전달 포트가 전부 따라오는 별건이다. **이 REQ 는 KST 고정만 한다** — `timestamptz` 는 순간을 저장하므로 나중에 계정별로 열어도 **재마이그레이션이 필요 없다**(그것이 이 형식을 고른 이유 중 하나다, → ADR)
- **`date` 컬럼 5개** (`entry_date` · `measured_at` · `shed_date` · `birthday` · `adoption_date`) — 날짜만 있는 값이라 타임존 개념이 없다. 타입을 바꾸지 않는다. 다만 **"오늘"을 판정할 때 KST 달력을 쓴다**(D4)
- **REQ-10 Phase 3 이후** (feeding · shed · diary) — 이 REQ 가 먼저 끝나야 `fed_at` "당일"·스트릭 일수·다이어리 미래 판정을 쓸 수 있다. **순서만 앞세우고 범위는 넘기지 않는다**
- **기존 데이터 보존** — 로컬·dev 뿐이고 배포 전이다. 변환 `USING` 절은 넣되(D6) **데이터 보존을 완료 기준으로 삼지 않는다**
- **`timestamptz` 를 쓰지 않는 신규 컬럼 금지 규칙의 자동 강제** — ArchUnit 은 SQL 을 보지 않는다. 계약(`CLAUDE.md`)으로만 남긴다

## 결정

> D1 은 2026-08-28 대화에서 **세 번 뒤집힌 끝에** 확정됐다(계산만 KST → 계정별 TZ → KST 고정 + 기술적으로 깔끔한 쪽). 뒤집힌 경로 자체가 근거라 ADR 로 올린다.

| 항목 | 결정 | 근거 | 기각한 안 |
|---|---|---|---|
| **D1** 저장 형식 | **`timestamptz`** — 순간(instant)을 저장하고, 노출·계산에서 KST 로 변환 | → [ADR-0002](../adr/ADR-0002-time-handling-timestamptz.md) | ↑ |
| **D2** 엔티티 시각 타입 | **`OffsetDateTime`** (2026-08-28 Phase 0 프로브로 확정) | `Instant` 는 네 설정 전부 `Z` 로 나가 D3 과 충돌해 탈락. `OffsetDateTime`·`ZonedDateTime` 은 거동·변경량이 같아 측정으로 못 가른다 — `timestamptz` 가 **오프셋만 보존하고 zone id 는 잃으므로** 후자는 저장되지 않는 정보를 담는 척한다 | **`Instant`** — Jackson 이 항상 `Z` 로 낸다(실측). **`ZonedDateTime`** — 위 이유 |
| **D3** 응답 오프셋 표기 | **`+09:00`** (`2026-06-30T18:00:00+09:00`). `Z` 로 내보내지 않는다 | "KST 고정" 결정과 응답이 일치해야 한다 — 클라이언트가 오프셋을 그대로 렌더할 수 있다. 원본 예시의 `Z` 는 **역반영 대상**(같은 순간의 다른 표기라 계약 위반은 아니다) | **`Z` 유지** — 저장 형식이 UTC 라는 내부 사정을 API 계약에 노출한다. 클라이언트가 KST 변환을 각자 구현하게 되어 목록 소비자마다 갈린다 |
| **D4** 달력 판정 기준 | **`Asia/Seoul` 고정.** "오늘"·"당일"·"미래"·"일수"는 전부 KST 자정 경계 | 사용자가 전원 국내라는 전제(D1 과 같은 근거). `date` 컬럼은 타임존이 없으므로 **비교하는 쪽이 기준을 정해야** 한다 | **UTC 자정** — 한국 사용자에게 오전 9시 전 기록이 "어제"가 된다. **시스템 기본 TZ** — 컨테이너에 `TZ` 를 안 넣으면 조용히 UTC 가 된다(아래 D5 와 같은 이유) |
| **D5** `now` 획득 | **`Clock` 빈 주입 — zone 은 `Asia/Seoul`** (`Clock.system(...)`, 2026-08-28 확정). `LocalDateTime.now()` 직접 호출을 없앤다.<br>zone 이 저장에 영향을 주지는 않는다(순간은 동일). 갈리는 곳은 **벽시계 파생** 하나뿐이고 — `LocalDate.now(clock)` · `LocalDateTime.now(clock)` — 그게 정확히 D4(달력 판정 = KST)의 자리다. UTC 로 두면 KST 00:00~09:00 에 "어제"가 나오는데 **에러 없이** 그렇다 | `LocalDateTime.now()` 는 **JVM 기본 TZ 에 암묵 의존**한다 — 배포 환경에 `TZ` 가 없으면 값이 9시간 어긋난 채 에러 없이 저장된다. 이 프로젝트가 반복해서 밟은 "조용한 실패"와 같은 얼굴(`.env` 빈 값 · `db.schema` 한쪽만 배선). 부수 이득으로 **테스트에서 시각을 고정**할 수 있다 | **`ZoneId` 상수만 두고 `now()` 유지** — 상수를 쓰는 것을 강제할 방법이 없어 새 코드가 그냥 `now()` 를 부른다 |
| **D6** 기존 행 변환 | **`USING <col> AT TIME ZONE 'UTC'`** — 앱이 UTC 로 썼다는 전제 그대로 | 앱 경로(JPA Auditing)로 들어간 행은 전부 UTC 다. **DB `default now()` 로 들어간 행은 세션 TZ 라 9시간 어긋나지만**(2026-07-30 실측), 로컬·dev 데이터뿐이고 `created_at` 은 「소스 구조」 §6 상 **앱이 SoT** 라 기본값은 안전망일 뿐이다 | **데이터를 비우고 재생성** — 마이그레이션이 환경마다 다르게 동작하게 된다. **`USING` 없이 타입 변경** — Postgres 가 세션 TZ 로 해석해 배포 환경에 따라 결과가 달라진다 |
| **D7** 마이그레이션 번호 | **`V3__time_to_timestamptz.sql`.** REQ-10 Phase 3 의 `food_size` 는 **`V4`** 로 민다 | 이 REQ 가 먼저 나간다(범위—제외). 적용된 마이그레이션은 되돌릴 수 없으므로 번호를 먼저 확정한다 | — |
| **D8** DB `default now()` | **손대지 않는다** | `now()` 는 `timestamptz` 컬럼에서 올바른 순간을 반환한다 — 타입을 바꾸면 07-30 함정이 **자동으로 사라진다**(세션 TZ 와 무관해진다). 이것이 D1 의 부수 효과 중 가장 큰 것이다 | **`default` 제거** — 앱이 SoT 이므로 제거해도 되지만 이 REQ 의 범위가 아니고, 안전망을 없애는 변경은 따로 판단할 일이다 |
| **D9** 오프셋 없는 요청 값 | **KST 로 해석한다.** `"2026-06-30T18:00:00"` 은 `2026-06-30T09:00:00Z` 와 같은 순간이다 | 2026-08-28 확정. 사용자가 전원 국내이고 노출·계산이 이미 KST 고정(D3·D4)이라, 오프셋이 빠진 값을 KST 벽시계로 읽는 것이 API 를 쓰는 쪽의 기대와 일치한다 | **400 으로 거부** — 엄격하지만 클라이언트가 오프셋을 빠뜨리는 흔한 실수를 오류로 만든다. **UTC 로 해석** — 저장 형식이라는 내부 사정을 요청 규약에 끌어들이고, 한국 사용자에게 9시간 어긋난다 |
| **D10** `LocalDateTimeUtil` 의 `now()` 2건 | **코드를 고친다** (2026-08-28 확정) — `isNowBetween` 삭제(사용처 0건) · `parseDateTime` 의 `now()` 폴백을 예외로. **ArchUnit 규칙(REQ-16-10)에는 예외를 두지 않는다** | 이식 유틸이지만 **사용처가 0건**이라 지금이 가장 싸다. `framework.util` 을 규칙에서 제외하면 **우회 경로가 열린 채 남는다** — 누가 `isNowBetween` 을 부르면 그 호출부는 `now()` 를 직접 부르지 않으므로 규칙이 못 잡는다 | **`framework.util` 을 규칙에서 제외** — 위 구멍. **파일 통째 삭제** — 30개 이식 유틸 중 하나를 "지금 안 쓴다"는 이유로 버리는 것이라 과하다 |

## 미결 질문

> ⚠️ 아래는 대화에서 답이 나오지 않았거나 **실측 없이는 고를 수 없는** 것들이다. Phase 0 프로브가 ①②③을 닫는다.

- [x] **① 엔티티 시각 타입 (D2)** → **`OffsetDateTime`.** `Instant` 는 네 설정 전부 `Z`(실측) 라 탈락. 나머지 둘은 동률이라 "저장되지 않는 정보를 담지 않는다"로 갈랐다.
      원래 질문: — `OffsetDateTime` vs `Instant` vs `ZonedDateTime`. 판정 기준: ⓐ `timestamptz` 읽기/쓰기가 정확한가 ⓑ Jackson 이 `+09:00` 으로 내보낼 수 있는가 ⓒ 기존 코드 변경량. **`Instant` 는 Jackson 기본이 항상 `Z` 라 D3 과 충돌할 수 있다** — 프로브에서 확인할 것
- [x] **② `hibernate.jdbc.time_zone: UTC` 를 유지하는가** → **유지한다.** `timestamptz` 3컬럼은 `UTC`/`Asia/Seoul` 에서 결과가 **완전히 같다**(무영향). 유일하게 작동하는 곳은 `timestamp`+`LocalDateTime` 쌍(`UTC` → `09:00` / `Asia/Seoul` → `18:00` 저장)이고, 지우면 훗날 `timestamp` 컬럼이 다시 생겼을 때 JVM 기본 TZ 의존이 살아난다. **주석의 근거만 교체한다** — "앱이 UTC 로 쓴다" → "남을지 모를 `timestamp` 컬럼을 JVM TZ 에서 떼어 놓는 안전망".
      원래 질문: — `timestamp` 시절엔 "앱이 UTC 로 쓴다"는 뜻이었지만 `timestamptz` 에서는 의미가 달라진다. 남겨야 하는지, 지워야 하는지, 지우면 무엇이 바뀌는지 실측 필요
- [x] **③ Jackson 이 KST 오프셋을 내는 정확한 설정** → **`ObjectMapper.setTimeZone(Asia/Seoul)` 한 줄.** `WRITE_DATES_AS_TIMESTAMPS`·`ADJUST_DATES_TO_CONTEXT_TIME_ZONE` 은 손댈 필요 없고, `WRITE_DATES_WITH_CONTEXT_TIME_ZONE` 은 **켜 둬야 한다**(끄면 `Z` 로 돌아간다). `ObjectMapper` 의 TZ 는 `UTC`·`hasExplicitTimeZone=false` 라 **JVM 기본 TZ 를 따라가지 않는다**.
      원래 질문: — `ObjectMapper.setTimeZone` · `WRITE_DATES_AS_TIMESTAMPS` · `ADJUST_DATES_TO_CONTEXT_TIME_ZONE` 의 조합. **추측하지 말고 실제 응답 문자열로 확인한다** (REQ-15 의 "`@Import` 를 빼면 조용히 틀린 계약을 고정한다"와 같은 자리)
- [x] **④ Notion 역반영의 범위** → **전수 조사 완료 (2026-08-28).** 아래가 Phase 4 의 작업 목록이다.
      **ⓐ `API I/F` 40행 중 시각 예시가 있는 행은 14개** (리터럴 20개). 나머지 26행은 시각 예시가 없다(DELETE 10건 + 응답을 "…객체"로만 적은 행 + `date` 필드만 쓰는 행).
      · **응답 12행** — 카카오 로그인 · 내 정보 조회 · 반려동물 목록 · 다이어리 목록 · 다이어리 상세 · 통합 타임라인(3) · 급여 목록(2) · 거식 스트릭 · 활동 목록(2) · 체중 목록 · 탈피 목록 · 갤러리 목록
      · **요청 2행** — 급여 기록(`fed_at`) · 활동 기록(`logged_at`)
      · ⚠️ **요청 예시는 고칠 의무가 없다.** D9 가 `Z`·`+09:00`·오프셋 없음을 모두 받으므로 `Z` 는 **여전히 유효한 요청**이다. 응답만 D3 에 걸린다. 일관성 때문에 바꿀지는 Phase 4 에서 판단할 일 — **"전부 바꾼다"고 뭉뚱그리면 계약이 아닌 것을 계약으로 만든다**
      · `date` 필드(`measured_at`·`shed_date`·`entry_date`·`taken_at`·`birthday`·`adoption_date`·`predicted_date`)는 전부 `"2026-06-30"` 형태로 균일하다 — 손대지 않는다
      **ⓑ 「소스 구조」 §6 에 시각 규약 절이 없다 → 신설 필요.** 함께 갱신할 곳:
      · §6 신규 행 — 저장 `timestamptz`(순간) · 노출 `+09:00` · 계산 `Asia/Seoul` · `Clock` 주입
      · §5 표의 `JacksonConfig` 행 — 현재 "SNAKE_CASE 전역 적용"만 적혀 있다. `timeZone(Asia/Seoul)` + `OffsetDateTimeDeserializer` 추가
      · §2 패키지 트리 — `processor/converter/ ⏸ (예정)` 이 실재하게 됐다. `db/migration` 도 `V1·V2` 까지만 적혀 있어 `V3` 추가
      **2026-08-31 실행** — ⓐ 응답 12행 완료(리터럴 20개, 12행 전부 재조회 확인) · 요청 2행은 **고치지 않았다**(D9 가 `Z` 를 받으므로 유효) · ⓑ 완료 · ⓒ 완료.
      ⚠️ **조사가 「테이블 정의서」를 빠뜨렸다** — `timestamp` 19컬럼이 남아 있었고(= `V3` 가 바꾼 19개) 함께 고쳤다. 레포 `db-schema.md` 가 0건인 것을 확인했으면서 **그 원본으로 거슬러 올라가지 않았다.** AGENTS §0 이 DDL 1차 출처로 지목한 곳이다.
      ⏸ **탭 본문 2곳은 API 로 수정 불가** — 작업 단계 Phase 4 참조.
      **ⓒ 레포 파생 요약** — `docs/specs/api-list.md` 의 "날짜 포맷: ISO 8601 (`2026-06-30`, `2026-06-30T15:00:00Z`)" 한 줄. `db-schema.md` 는 0건
- [x] **⑥ 엔티티 ↔ DB 실제 컬럼 타입 대조를 무엇으로 하는가** → **계약으로만 남긴다** (2026-08-28 확정). `범위 — 제외` 의 "자동 강제는 하지 않는다"와 같은 결정이다 — 뿌리가 같다(SQL 과 코드를 맞대볼 수단이 없다). Phase 4 에서 `CLAUDE.md` 에 올린다. **Testcontainers 도입은 별건**이고, 도입하면 REQ-10 의 keyset 경계·`@Transactional` 롤백까지 함께 닫히므로 그때 한꺼번에 다룬다.
      원래 질문: — `validate` 가 타입을 안 보는 것이 실측으로 드러나, 마이그레이션을 빠뜨려도 조용히 통과한다. DB 를 조회하는 케이스를 쓰면 막히지만 **DB 가 있는 환경에서만 돌아 CI 에서 깨진다.** Testcontainers 도입(의존성 추가 · 승인 대상)과 함께 판단할 일이다
- [x] **⑤ 로컬 DB 없이 어디까지 검증되는가** → **전제가 깨졌다.** 2026-08-28 이 머신에 Docker 로 Postgres 17 을 세웠다(`CLAUDE.local.md`). Phase 1 판정 가능. 다만 **`./gradlew test` 는 여전히 DB 를 쓰지 않는다**(Testcontainers 미도입) — DB 왕복은 수동 확인이다.
      원래 질문: — 이 세션 머신에는 `.env` 도 Postgres 도 없다. REQ-10 Phase 1·2 의 확인 2건도 같은 이유로 밀려 있다. **`ddl-auto: validate` 통과와 마이그레이션 실행은 DB 없이 확인할 수 없다** — Phase 1 완료 기준이 여기에 걸린다

- [ ] **⑦ `BaseSoftDeleteEntity.softDelete()` 는 어느 REQ 가 가져가는가** (2026-08-31 신설) — **이 계획서가 자기 자신과 어긋난다.** `범위 — 포함` 은 "`AuthService` 2곳", `제약·함정` 은 "3곳 — `Clock` 주입은 Phase 3 몫". Phase 3 은 **전자를 따랐다**(2026-08-31 결정): JPA 엔티티라 빈을 주입할 수 없고, `deleted_at` 은 벽시계 파생이 아니라 **순간**이라 D4 의 자리가 아니다 — TZ 위험은 Phase 1 의 `OffsetDateTime` 전환에서 이미 사라졌고 남는 이득은 테스트 고정 하나뿐이라 엔티티 시그니처를 바꿀 값을 못 했다. **REQ-16-10 규칙 범위도 `business`·`framework` 로 한정돼 `data` 는 안 걸린다.** 남은 질문은 "그 상태로 둘 것인가"이고, 답이 "아니다"면 `softDelete(OffsetDateTime now)` 로 바꾸는 별건이다
- [ ] **⑧ `JwtTokenProvider.create()` 의 `new Date()` 를 규약에 넣는가** (2026-08-31 신설) — 이 계획서 어디에도 없다. `java.time` 이 아니라 **REQ-16-10 에 안 걸리고**, 순간이라 TZ 위험도 없다. 다만 **발급 시각을 고정할 수 없어** 토큰 만료를 시각 독립적으로 재는 길이 막힌다(REQ-16-12·17 은 저장 행의 `expires_at` 을 쓰므로 영향 없다). 규약 밖으로 둘지, `Clock` 을 주입할지 결정 필요

## 작업 단계

> Phase 1 개 = 커밋 1 개. **Phase 0 을 건너뛰지 않는다** — ①②③이 안 닫힌 채 Phase 1 을 시작하면 엔티티 타입을 두 번 바꾸게 된다.

- [x] **Phase 0 — 프로브 (엔티티 타입 · Jackson 설정 확정)** — 완료 2026-08-28. 프로브 코드·`req16_probe` 스키마 삭제됨. **코드 변경 0건이라 커밋 없음** (결과는 PROGRESS 2026-08-28)
      `timestamptz` 컬럼 하나를 만든 임시 테이블에 세 타입(`OffsetDateTime`·`Instant`·`ZonedDateTime`)을 각각 매핑해 왕복시키고, `@WebMvcTest` 로 **응답 문자열**을 눈으로 확인한다. `hibernate.jdbc.time_zone` 을 켠 상태·끈 상태 둘 다.
      완료 기준: 미결 ①②③이 **실측값과 함께** 닫힘 · 고른 타입으로 `2026-06-30T18:00:00+09:00` 이 실제로 출력되는 것을 확인 · 프로브 코드는 삭제(REQ-10 Phase 0 과 같은 방식, 결과는 커밋 본문에)

- [x] **Phase 1 — V3 마이그레이션 + 엔티티 타입 전환** — 완료 2026-08-28 (`5730b5a`)
      `V3__time_to_timestamptz.sql` (19 컬럼 · D6 의 `USING`) · 엔티티 6필드 타입 변경 · DTO 시각 필드 타입 변경(6파일).
      완료 기준: 마이그레이션이 로컬 DB 에 적용됨 ✅(`petkok_local` v3 · `timestamptz` 19개) · ~~`ddl-auto: validate` 통과(엔티티 ↔ 스키마 대조)~~ **이 근거는 무효다 — validate 는 타입을 안 본다(제약·함정 참조). 통과는 했으나 대조의 근거가 못 된다** · `./gradlew test` 전건 통과 ✅(174/0)

- [x] **Phase 2 — 직렬화·역직렬화 규약 (`JacksonConfig`)** — 완료 2026-08-28 (`0465aec`)
      Phase 0 에서 고른 설정 적용. ~~기존 REQ 컨트롤러 테스트 갱신~~ **할 것이 없었다** — 시각 *값* 을 단언하는 케이스가 애초에 없고(`created_at` 은 `exists()` 만 본다), 타입 변경은 Phase 1 에서 끝났다.
      ⚠️ **"`JacksonConfig` 한 곳"이 아니라 두 곳이 됐다** — D9(오프셋 없으면 KST)가 착수 직전에 추가됐고, 그건 설정으로 얻을 수 없다. `processor/converter/OffsetDateTimeDeserializer` 를 함께 만들었다.
      완료 기준: 응답 시각이 전부 `+09:00` 표기 · `Z` 로 온 요청과 `+09:00` 으로 온 요청이 **같은 순간**으로 저장됨 · 오프셋 없는 요청 값의 동작이 케이스로 고정됨(거부인지 KST 해석인지는 Phase 0 에서 정한다)

- [x] **Phase 3 — 계산 기준 KST 고정 (`Clock` · `ZoneId` 상수)** — 완료 2026-08-31 (`1cf2d6f`)
      `framework/constant/TimeConstant.KST` · `framework/config/TimeConfig` 의 `Clock` 빈 · `AuthService` 2곳 교체 · D10(`LocalDateTimeUtil`) 처리.
      ⚠️ **범위가 하나 늘었다** — `Asia/Seoul` 하드코딩이 계획서가 센 2곳이 아니라 **3곳**이었다(`LocalDateTimeUtil.ZONE_ASIA_SEOUL` 누락). 상수 통합에 그 파일이 함께 들어갔다.
      완료 기준: ~~`LocalDateTime.now()` 직접 호출이~~ **무인자 `now()` 5종 호출이** `business`·`framework` 에 0건 ✅(REQ-16-10 — 문구 확장 근거는 검증 계약 절) · 고정 `Clock` 으로 refresh 만료 경계 테스트가 시각에 의존하지 않고 통과 ✅(REQ-16-12·17) · ~~KST 자정 전후 판정이 케이스로 고정됨~~ **이 REQ 의 범위보다 넓어 REQ-10 이 가져간다** — 판정 로직(당일·미래·일수)이 REQ-10 Phase 3 이후에 들어오므로 여기서 쓸 케이스가 없다. **넘긴 것이지 채운 것이 아니다**

- [ ] **Phase 4 — 문서 역반영** — 2026-08-31 **실행 완료. 체크는 켜지 않는다** (아래 ⏸ 2건)
      ✅ `API I/F` 응답 12행(리터럴 20개) · 「소스 구조」 §2·§5·§6 · **「테이블 정의서」 19컬럼**(조사 범위 밖이었다) · `docs/specs/api-list.md` · `CLAUDE.md` §시각 처리 신설(계약 ⓐ `timestamptz`+`OffsetDateTime` · ⓑ `validate` 는 타입을 안 본다 — **둘은 같이 적어야 뜻이 산다** · 존 상수 한 곳).
      ⏸ **API 로 수정 불가 — 사람 손 대기 2건.** ⓐ 「설계」→ API 탭 본문 "날짜 포맷" 행(아직 `…15:00:00Z`) ⓑ 「설계」→ DB 탭 DDL 코드블록(아직 `timestamp`). 실제 호출로 확인했다 — `validation_error: … is not a page or database`. ⓑ 의 **원본**인 「테이블 정의서」는 고쳤으므로 파생만 남았다.
      ⚠️ **표기는 "숫자 유지 + `Z`→`+09:00`" 으로 정했다**(2026-08-31). 리터럴을 그대로 변환하면 12행 중 9행에서 **날짜가 하루 밀려** `entry_date`·`shed_date`·`taken_at`·타임라인 `date` 와 어긋나고 "시간순 정렬" 예시가 깨진다 — 계획서의 "같은 순간의 다른 표기"를 곧이곧대로 따르면 계약 문서가 스스로를 반박한다.
      완료 기준: 미결 ④ 가 닫힘 ⏸(탭 2곳 남음) · 원본과 코드가 어긋나는 곳 0건 ⏸(같은 2곳)

## 검증 계약

> 작성: 2026-08-28 · 근거: 이 계획서 (원본은 Notion 「소스 구조」 · `API I/F`) · 검증: `/testrun REQ-16`
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-16-01]` 형태로 박는다.
> **Phase 0 의 01~03 은 프로브다** — 세 후보 타입을 심었다 지우는 확인이라 영구 테스트로 남지 않는다. `/implement REQ-16 0` 이 실행하고 결과를 커밋 본문에 남기며, `결과` 열은 `REQ-10-01` 처럼 `✅ 수동` 으로 채운다.
> **2026-08-28 2차 `/testgen` — Phase 1 케이스(04~07)의 코드를 썼다.** 미결 ①이 Phase 0 프로브로 닫혀 단언할 타입이 생겼기 때문이다. 08 이후는 각 Phase 착수 직전에 쓴다.
> ⚠️ (1차 기록) **테스트 코드는 이번에 쓰지 않았다 — 표만 넣는다.** 미결 ①(엔티티 시각 타입)이 안 닫혀 **단언할 타입이 없고**, Java 는 대상 타입이 없으면 테스트 소스 전체가 컴파일되지 않는다(REQ-08·09·10 실측). 04 이후의 코드는 **각 Phase 착수 직전 `/testgen` 재호출**로 쓴다. 지금 쓰면 `main` 이 빨간불이 되어 REQ-10 작업까지 막힌다.
> **`message` 를 단언하지 않는다** — `status` 와 `error.code` 만 본다(AGENTS §6).

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-16-01 | Jackson 직렬화 | `OffsetDateTime` · `Instant` · `ZonedDateTime` 중 어느 것이 `+09:00` 을 내는가 (DB 불필요) | 프로브 | Phase 0 완료 기준 — "고른 타입으로 `2026-06-30T18:00:00+09:00` 이 실제로 출력되는 것을 확인" | 0 | ✅ 수동 |
| REQ-16-02 | Hibernate 왕복 | 고른 타입으로 `timestamptz` 컬럼에 쓰고 읽었을 때 순간이 보존된다 (DB 필요) | 프로브 | 미결 ① — "ⓐ `timestamptz` 읽기/쓰기가 정확한가" | 0 | ✅ 수동 |
| REQ-16-03 | `hibernate.jdbc.time_zone` | 켠 상태와 끈 상태의 저장·조회 결과 차이 (DB 필요) | 프로브 | 미결 ② — "남겨야 하는지, 지워야 하는지, 지우면 무엇이 바뀌는지 실측 필요" | 0 | ✅ 수동 |
| REQ-16-04 | `V3__time_to_timestamptz.sql` | `timestamptz` 로 바꾸는 컬럼이 **19개**다 (빠뜨린 컬럼 없음) | 회귀 | 범위—포함 — "`timestamp` 19개 → `timestamptz`" | 1 | ✅ |
| REQ-16-05 | 〃 | 모든 타입 변환에 `USING ... AT TIME ZONE 'UTC'` 가 붙어 있다 | 회귀 | D6 — "`USING <col> AT TIME ZONE 'UTC'`" · 기각안 — "Postgres 가 세션 TZ 로 해석해 배포 환경에 따라 결과가 달라진다" | 1 | ✅ |
| REQ-16-06 | 엔티티 6필드 | 시각 필드에 `LocalDateTime` 이 남아 있지 않다 (타입 무관 단언) | 불변식 | 범위—포함 — "(6개 필드, `LocalDateTime` → D2 가 정하는 타입)" | 1 | ✅ |
| REQ-16-07 | 날짜 필드 5개 **중 3개** | `measuredAt` · `entryDate` · `shedDate` · `birthday` · `adoptionDate` 는 여전히 `LocalDate` 다 | 불변식 | 범위—제외 — "날짜만 있는 값이라 타임존 개념이 없다. 타입을 바꾸지 않는다" | 1 | ✅ |
| REQ-16-08 | 응답 (HTTP 왕복) | 시각 필드가 `+09:00` 오프셋을 달고 나간다 | 정상 | Phase 2 완료 기준 — "응답 시각이 전부 `+09:00` 표기" | 2 | ✅ |
| REQ-16-09 | 요청 (HTTP 왕복) | `...Z` 로 온 값과 `...+09:00` 으로 온 같은 순간이 동일하게 저장된다 | 회귀 | Phase 2 완료 기준 — "`Z` 로 온 요청과 `+09:00` 으로 온 요청이" | 2 | ✅ |
| REQ-16-10 | ArchUnit | 무인자 `now()` 호출이 `business`·`framework` 에 없다 (`LocalDateTime`·`OffsetDateTime`·`LocalDate`·`Instant`·`ZonedDateTime` 5종. `now(Clock)` 오버로드는 허용) | 불변식 | D5 — "`LocalDateTime.now()` 직접 호출을 없앤다" · Phase 3 완료 기준 — "`LocalDateTime.now()` 직접 호출이 `business`·`framework` 에 0건" | 3 | ✅ |
| REQ-16-11 | `ZoneId` 상수 | `framework/constant` 에 있고 값이 `Asia/Seoul` 이다 | 불변식 | 범위—포함 — "`ZoneId` 상수를 `framework/constant` 에 한 곳" | 3 | ✅ |
| REQ-16-12 | `AuthService` 만료 판정 | 고정 `Clock` 을 주입하면 실행 시각과 무관하게 만료 경계가 재현된다 | 회귀 | Phase 3 완료 기준 — "고정 `Clock` 으로 refresh 만료 경계 테스트가 시각에 의존하지 않고 통과" | 3 | ✅ |
| REQ-16-13 | 엔티티 6필드 | 타입이 정확히 `OffsetDateTime` 이다 | 불변식 | D2 — "(2026-08-28 Phase 0 프로브로 확정)" | 1 | ✅ |
| REQ-16-14 | `application.yml` | `hibernate.jdbc.time_zone` 이 `UTC` 로 남아 있다 | 회귀 | 미결 ② — "지우면 훗날 `timestamp` 컬럼이 다시 생겼을 때 JVM 기본 TZ 의존이 살아난다" | 1 | ✅ |
| REQ-16-15 | 요청 (HTTP 왕복) | 오프셋 없는 값은 KST 로 해석된다 | 정상 | D9 — "**KST 로 해석한다.** `"2026-06-30T18:00:00"` 은 `2026-06-30T09:00:00Z` 와 같은 순간이다" | 2 | ✅ |
| REQ-16-16 | 소스 텍스트 (`src/main/java`) | `Asia/Seoul` 리터럴이 `TimeConstant` 밖에 없다 | 회귀 | 제약·함정 — "`Asia/Seoul` 이 두 파일에 하드코딩돼 있다" · "늘리지 말 것" | 3 | ✅ |
| REQ-16-17 | `AuthService` 만료 판정 | 만료 시각 **이전**으로 고정한 `Clock` 에서는 정상 재발급된다 | 경계 | Phase 3 완료 기준 — "고정 `Clock` 으로 refresh 만료 경계 테스트가 시각에 의존하지 않고 통과" | 3 | ✅ |

> **결과 갱신: 2026-08-28 — 01~03 `✅ 수동` (Phase 0).** 프로브 3건 실행 · 실패 0 · 코드와 `req16_probe` 스키마는 삭제. 실측값은 PROGRESS 2026-08-28.
>
> ⚠️ **REQ-16-03 은 케이스 문구대로 재지 못했다 — 이탈.** 표는 "켠 상태와 **끈 상태**"라고 적었지만 실제로는 `UTC` vs `Asia/Seoul` 두 값으로 대조했다. Spring 에서 이 프로퍼티를 **깨끗하게 "없음"으로 만들 방법이 없어서다**(`=` 빈 값을 주면 Hibernate 가 `GMT` 로 읽어 "끈 상태"가 아니다). 두 값이 `timestamptz` 에서 **완전히 같은 결과**를 냈으므로 "무영향"이라는 ② 의 답은 그대로 성립하지만, **진짜 미설정 상태는 재지 않았다.** 되돌아올 여지가 있으면 여기다.
>
> ⚠️ **01 은 첫 시도가 무효였다.** 입력값에 이미 `+09:00` 을 달아 두어 **네 설정이 전부 같은 답**을 냈고, 그대로 읽었으면 "설정 불필요"라는 정반대 결론이 나왔다. 실제 응답 경로는 **DB 에서 읽은 `Z`** 다. 대조군(`America/New_York` → `-04:00`)까지 넣어 설정이 실제로 작동하는 것을 확인한 뒤에야 갈렸다.

> **2026-08-28 — 04~07 코드 작성.** `V3TimestamptzMigrationTest`(04·05) · `TimeFieldTypeContractTest`(06·07). 네 건 모두 **지금은 실패한다** — `V3` 도 엔티티 전환도 없기 때문이고, 이것이 정상이다(`SecurityConfigPublicPathsTest` 와 같은 자리).
>
> ⚠️ **REQ-16-07 은 6개 중 3개만 덮는다** (계획서가 처음에 `date` 컬럼을 5개로 셌으나 `photos.taken_at` 이 빠져 있었다 — 2026-08-28 실측). `entryDate`(diary) · `shedDate`(shed) 는 **엔티티 자체가 없다** — 그 도메인은 REQ-10 Phase 3~5 다. Java 는 없는 클래스를 참조하면 테스트 소스 전체가 컴파일되지 않으므로 뺐다. **diary·shed 가 들어올 때 `TimeFieldTypeContractTest.DATE_FIELDS` 에 두 줄을 추가해야 하고, 빠뜨려도 초록불이다** — REQ-10 Phase 3·4 완료 기준에 이 항목을 넣을 것.
>
> **04·05 는 SQL 을 파싱하지 않고 텍스트로 센다.** 여기서 잡을 사고는 문법 오류가 아니라 **빠뜨린 컬럼**과 **빠뜨린 `USING` 절**이고, 둘 다 문법적으로 올바른 SQL 이라 DB 가 알려주지 않는다. 대신 마이그레이션을 계획서가 정한 `alter column … type timestamptz using … at time zone 'UTC'` 형태로 쓰지 않으면 **(a) 테스트 결함으로 빨간불이 난다.**

> **2026-08-28 — 13·14 추가 승인.** 미결 ①②가 Phase 0 에서 닫혀 근거가 생긴 두 건이다.
> - **13 은 06 과 겹치지 않는다** — 06 은 *빠뜨린 필드*, 13 은 *잘못 고른 타입*을 잡는다. 06 만 있으면 누가 `Instant` 로 바꿔도 초록불이고, 13 만 있으면 한 필드를 안 바꿨을 때 원인이 "타입 틀림"으로 보인다. **실패 원인이 갈리도록** 나눴다.
> - ⚠️ **14 는 처음부터 통과한다.** 값이 이미 있기 때문이고, 회귀 방어라 정상이다. 성립 조건은 "**지웠을 때 빨개지는가**" 이므로 `/implement` 에서 한 번 지워 보고 확인한다. 이 설정은 `timestamptz` 에 무영향이라(Phase 0 실측) **"안 쓰는 설정"으로 보여 정리 대상이 되기 쉽고**, 지운 순간이 아니라 훗날 `timestamp` 컬럼이 생긴 순간에 터진다 — 그 시차를 잡는 케이스다.
> - 14 를 테스트로 두었으므로 `CLAUDE.md` 계약 승격은 하지 않는다. 계약은 사람이 읽어야 지켜지지만 테스트는 지운 즉시 빨간불이 난다.

> **2026-08-28 — 08·09 코드 작성.** `ActivityTimeSerializationWebMvcTest`. Phase 2 착수 직전 `/testgen` 재호출로 썼다.
>
> ⭐ **08 의 응답 픽스처는 오프셋을 `Z` 로 둔다.** `timestamptz` 는 원래 오프셋을 저장하지 않아 **DB 에서 읽으면 항상 `Z`** 이기 때문이다(Phase 0 실측). 픽스처에 `+09:00` 을 미리 달면 **설정이 없어도 통과해** 케이스가 아무것도 검증하지 못한다 — Phase 0 프로브에서 실제로 그렇게 재다가 "설정 불필요"라는 정반대 결론이 나올 뻔했다.
>
> ⚠️ **09 는 지금도 통과할 수 있다.** Jackson 이 두 표기를 이미 같은 순간으로 읽는다. 그래도 두는 이유는 회귀 방어다 — `ADJUST_DATES_TO_CONTEXT_TIME_ZONE` 을 건드리거나 타입을 `LocalDateTime` 으로 되돌리면 이 케이스가 먼저 깨진다. 14 와 같은 성격이라 **`/implement` 에서 역프로브로 성립 조건을 확인할 것.**
>
> ⚠️ **Phase 2 완료 기준은 08·09 로 다 덮이지 않는다.** 세 번째 항목 "오프셋 없는 요청 값의 동작이 케이스로 고정됨"이 **여전히 미결**이다 — Phase 0 프로브가 그것을 재지 않았다(계획서는 "Phase 0 에서 정한다"고 썼지만 정해지지 않았다). **정하기 전에는 Phase 2 를 완료로 체크할 수 없다.**

> **2026-08-31 — 10 문구 확장 · 16·17 추가 (Phase 3 착수 `/testgen`).**
>
> ⚠️ **10 을 계획서 문구 그대로 쓰면 통과하는 가짜 규칙이 된다.** 문구가 `LocalDateTime.now()` 인데 **Phase 1 이 대상을 전부 `OffsetDateTime.now()` 로 바꿨다.** 그대로 두면 `business`·`framework` 의 `LocalDateTime.now()` 는 `LocalDateTimeUtil` 2건(D10 대상)뿐이라 — **D10 만 하고 `Clock` 주입을 안 해도 초록불**이고, `AuthService` 2곳이 규칙 밖으로 빠진다. Phase 3 의 본 목표가 검사되지 않는다. 근거는 D5 의 "`now` 획득 경로 통일"이고, 문구가 Phase 1 의 타입 변경을 못 따라간 것을 되돌린 것이다 — **계획서 열거가 어긋난 네 번째 사례**(앞의 셋은 제약·함정 절).
>
> ⚠️ **`Asia/Seoul` 하드코딩은 두 곳이 아니라 세 곳이다** (2026-08-31 실측). 제약·함정 절이 `JacksonConfig`·`OffsetDateTimeDeserializer` 만 셌는데 **`framework/util/date/LocalDateTimeUtil` 의 `ZONE_ASIA_SEOUL` 이 빠져 있었다.** 하필 D10 이 어차피 건드리는 파일이다. 16 은 "늘리지 말 것"을 사람이 읽는 경고가 아니라 **빨간불**로 바꾼다.
>
> **16 은 `src/main/java` 만 훑는다** — `ArchitectureTest` 의 `DoNotIncludeTests` 와 같은 범위 판단이다. 테스트 픽스처까지 막으면 고정 `Clock` 테스트가 자기 자신에 걸린다. `V3__time_to_timestamptz.sql` 주석의 `Asia/Seoul` 도 이 범위 밖이다.
> ⚠️ **16 은 0건이 "깨끗함"인지 "스캐너 고장"인지 구별되지 않는 종류다**(CLAUDE.md — 빈 패턴은 전건 매치). 그래서 케이스 안에 **`TimeConstant.java` 자신은 리터럴을 갖고 있다**는 역프로브를 함께 넣었다.
>
> **17 은 12 의 반대쪽이다.** 12 가 만료 *이후*만 보므로 경계 한쪽만 덮인다 — 만료 판정을 통째로 `true` 로 뒤집어도 12 는 통과한다. 둘을 나눈 것은 "한 케이스에 단언을 몰지 않는다"를 지키면서 경계 양쪽을 덮기 위해서다.
>
> **`Clock` 빈의 zone 은 더 이상 미결이 아니다** — D5 가 2026-08-28 에 `Asia/Seoul` 로 확정했다. 아래 "근거가 없어 케이스를 쓰지 않았다" 목록의 해당 항목은 낡았다.

**아래 1건은 근거가 없어 케이스를 쓰지 않았다** (원래 2건 — 하나는 2026-08-28 에 닫혔다). 미결이 닫히면 행을 추가한다.

- ~~**오프셋 없는 요청 값의 동작**~~ **2026-08-28 닫힘 → D9(KST 해석).** 케이스 REQ-16-15 로 추가했다. **Phase 0 이 이것을 재지 않아** 계획서가 "Phase 0 에서 정한다"고 적어 둔 것이 지켜지지 않았고, Phase 2 착수 직전 대화에서 정했다
- **`Clock` 빈의 zone** (D5 가 "`Asia/Seoul` 또는 UTC — D2 와 함께 정한다"로 열어 두었다). 또한 Phase 3 완료 기준의 "KST 자정 전후 판정이 케이스로 고정됨"은 **이 REQ 의 범위보다 넓다** — 판정 로직(당일·미래·일수)은 REQ-10 Phase 3 이후에 들어온다. **여기서는 상수·`Clock` 까지만 고정하고, 자정 경계 케이스는 REQ-10 이 가져간다**

**Phase 4(문서 역반영)에는 케이스가 없다** — Notion·`CLAUDE.md` 편집이라 테스트로 고정할 대상이 아니다. 완료 판정은 미결 ④ 를 닫는 것으로 한다.

## 제약·함정

- ⚠️ **적용된 마이그레이션은 한 글자도 못 고친다** (`CLAUDE.md`). `V3` 를 로컬에 한 번 적용하면 수정이 아니라 `V4` 를 새로 써야 한다. **Phase 0 프로브가 끝나기 전에 `V3` 를 적용하지 말 것**
- ⚠️ **`ddl-auto: validate` 는 컬럼 *존재*만 보고 *타입*은 보지 않는다** (2026-08-28 실측 — 이 계획서가 처음에 정반대로 적어 두었던 항목이다). 엔티티를 `OffsetDateTime` 으로 두고 컬럼을 `timestamp` 로 남긴 채(`SPRING_FLYWAY_TARGET=2`) 기동해도 **그대로 뜬다.** 검사기 자체는 살아 있다 — `users.email` 을 지우면 `Schema-validation: missing column` 으로 막힌다.
	- **따라서 "엔티티 ↔ 스키마 대조"는 실제로 일어나지 않는다.** 엔티티만 바꾸고 마이그레이션을 빠뜨리면(또는 그 반대) **조용히 통과한다.** Phase 1 의 실제 방어선은 REQ-16-04·05(마이그레이션 텍스트 검사)이고, 그마저 DB 의 실제 컬럼 타입과는 맞대보지 않는다 → 미결 ⑥
- ⚠️ **`hibernate.jdbc.time_zone` 은 `timestamptz` 에서 의미가 달라진다.** `timestamp` 시절의 근거로 남겨 두면 안 된다(미결 ②)
- ⚠️ **이 계획서의 열거가 셋 어긋났다** (2026-08-28 Phase 1 실측). 동작은 전부 옳고 문서만 틀렸지만, **Phase 3 이 이걸 모르고 시작하면 안 된다.**
	- `date` 컬럼은 **5개가 아니라 6개** — `photos.taken_at` 이 빠졌다. 안 건드리는 것은 그대로 맞고, REQ-16-07 이 덮는 범위가 "5개 중 3개"가 아니라 **"6개 중 3개"** 다
	- `now()` 직접 호출은 **`AuthService` 2곳이 아니라 3곳** — `BaseSoftDeleteEntity.softDelete()` 가 빠졌다. Phase 1 에서 `OffsetDateTime.now()` 로만 바꿔 두었고 `Clock` 주입은 Phase 3 몫이다
	- ~~⭐ **`framework/util/date/LocalDateTimeUtil` 에 `LocalDateTime.now()` 가 2건 있다.**~~ **2026-08-31 닫힘 (D10 — 코드를 고쳤다).** "이식한 범용 유틸이라 없앨 수 없다"는 전제가 틀렸다 — **사용처가 0건**이었다. `isNowBetween` 삭제 · `parseDateTime` 의 `now()` 폴백을 예외로. 규칙에는 예외를 두지 않았다
- ⚠️ **`LocalDateTime.now()` 는 JVM 기본 TZ 에 암묵 의존한다** (D5). 배포 환경에 `TZ` 가 없으면 **에러 없이** 9시간 어긋난다. 이 REQ 가 끝난 뒤 새 코드가 다시 부르면 규약이 조용히 무너지므로 Phase 3 완료 기준에 `grep` 0건을 넣었다
- ⚠️ **DB `default now()` 로 심은 행과 앱이 쓴 행이 9시간 어긋난다** (2026-07-30 실측) — `timestamptz` 전환으로 **사라지는 함정**이다(D8). 전환 후에는 `now() at time zone 'UTC'` 픽스처 규칙도 함께 폐기해야 한다. 폐기를 빠뜨리면 이번엔 반대로 9시간 어긋난다
- ⚠️ **응답 쪽과 요청 쪽은 서로 다른 두 장치다.** `setTimeZone(Asia/Seoul)` 은 **직렬화의 렌더 기준일 뿐**이고, 오프셋 없는 입력을 해석해 주지 않는다 — Jackson 기본 역직렬화는 그런 값을 **아예 거부해** 컨트롤러에 도달조차 못 하고 400 이 된다(2026-08-28 실측: 서비스 호출 0회). **한쪽만 보고 "설정 하나로 끝났다"고 읽으면 다른 쪽이 조용히 400 이 된다**
- ~~⚠️ **`Asia/Seoul` 이 두 파일에 하드코딩돼 있다**~~ **2026-08-31 닫힘 — 세 곳이었다.** `LocalDateTimeUtil.ZONE_ASIA_SEOUL` 이 이 열거에서 빠져 있었다(계획서 열거가 어긋난 네 번째 사례). 셋을 `TimeConstant.KST` 한 곳으로 합쳤고, **"늘리지 말 것"이라는 사람이 읽는 경고를 REQ-16-16 이 빨간불로 바꿨다** — 다시 흩뿌리면 위반 파일명을 지목하며 실패한다
- ⚠️ **`JacksonConfig` 는 전역이라 이 파일을 고치면 전건을 돌려야 한다.** `@WebMvcTest` 슬라이스 전부가 `@Import` 하고 응답 시각 표기가 모든 도메인에서 바뀐다. REQ 필터만으로는 회귀가 안 보인다
- ⚠️ **응답 형태 변경은 클라이언트 계약 변경이다.** `created_at` 이 모든 도메인에서 바뀐다(auth · user · pet · weight · activity). 앱 구현 전이라 지금이 가장 싸다
- ⚠️ **`date` 컬럼은 타입을 바꾸지 않는다** — `measured_at`·`entry_date`·`shed_date` 를 `timestamptz` 로 만들면 "그 날짜"가 순간이 되어 커서 정렬(REQ-10 D8)과 파생 필드 정의(D3)가 전부 흔들린다
- ⚠️ **테스트가 시각을 단언하는 7파일** — 대부분 DTO 생성용 `LocalDateTime.now()` 라 타입만 바뀌면 되지만, `AuthServiceRefreshTest`·`RefreshTokenTest` 는 **만료 경계**를 다뤄 의미가 바뀔 수 있다. `/testrun` 의 (a)/(b) 분류를 그대로 적용할 것
