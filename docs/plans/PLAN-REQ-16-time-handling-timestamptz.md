# PLAN-REQ-16 · 시각 처리 규약 — `timestamptz` 전환 (저장 = 순간 · 노출·계산 = KST 고정)

> 출처: 2026-08-28 세션 (REQ-10 Phase 2 머지 직후) · 작성: 2026-08-28 · 상태: 📝 초안

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
- **직렬화 규약** — 응답 시각은 KST 오프셋을 붙여 내보낸다(`2026-06-30T18:00:00+09:00`). `framework/config/JacksonConfig` 한 곳
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
| **D2** 엔티티 시각 타입 | **미결 ①** — `OffsetDateTime` / `Instant` / `ZonedDateTime` 중 프로브로 정한다 | 셋 다 `timestamptz` 에 매핑되지만 **Jackson 출력 형태와 `hibernate.jdbc.time_zone` 의 상호작용이 다르다.** 추측으로 고르면 Phase 2 에서 되돌아온다 | — |
| **D3** 응답 오프셋 표기 | **`+09:00`** (`2026-06-30T18:00:00+09:00`). `Z` 로 내보내지 않는다 | "KST 고정" 결정과 응답이 일치해야 한다 — 클라이언트가 오프셋을 그대로 렌더할 수 있다. 원본 예시의 `Z` 는 **역반영 대상**(같은 순간의 다른 표기라 계약 위반은 아니다) | **`Z` 유지** — 저장 형식이 UTC 라는 내부 사정을 API 계약에 노출한다. 클라이언트가 KST 변환을 각자 구현하게 되어 목록 소비자마다 갈린다 |
| **D4** 달력 판정 기준 | **`Asia/Seoul` 고정.** "오늘"·"당일"·"미래"·"일수"는 전부 KST 자정 경계 | 사용자가 전원 국내라는 전제(D1 과 같은 근거). `date` 컬럼은 타임존이 없으므로 **비교하는 쪽이 기준을 정해야** 한다 | **UTC 자정** — 한국 사용자에게 오전 9시 전 기록이 "어제"가 된다. **시스템 기본 TZ** — 컨테이너에 `TZ` 를 안 넣으면 조용히 UTC 가 된다(아래 D5 와 같은 이유) |
| **D5** `now` 획득 | **`Clock` 빈 주입** (`Clock.system(ZoneId.of("Asia/Seoul"))` 또는 UTC — D2 와 함께 정한다). `LocalDateTime.now()` 직접 호출을 없앤다 | `LocalDateTime.now()` 는 **JVM 기본 TZ 에 암묵 의존**한다 — 배포 환경에 `TZ` 가 없으면 값이 9시간 어긋난 채 에러 없이 저장된다. 이 프로젝트가 반복해서 밟은 "조용한 실패"와 같은 얼굴(`.env` 빈 값 · `db.schema` 한쪽만 배선). 부수 이득으로 **테스트에서 시각을 고정**할 수 있다 | **`ZoneId` 상수만 두고 `now()` 유지** — 상수를 쓰는 것을 강제할 방법이 없어 새 코드가 그냥 `now()` 를 부른다 |
| **D6** 기존 행 변환 | **`USING <col> AT TIME ZONE 'UTC'`** — 앱이 UTC 로 썼다는 전제 그대로 | 앱 경로(JPA Auditing)로 들어간 행은 전부 UTC 다. **DB `default now()` 로 들어간 행은 세션 TZ 라 9시간 어긋나지만**(2026-07-30 실측), 로컬·dev 데이터뿐이고 `created_at` 은 「소스 구조」 §6 상 **앱이 SoT** 라 기본값은 안전망일 뿐이다 | **데이터를 비우고 재생성** — 마이그레이션이 환경마다 다르게 동작하게 된다. **`USING` 없이 타입 변경** — Postgres 가 세션 TZ 로 해석해 배포 환경에 따라 결과가 달라진다 |
| **D7** 마이그레이션 번호 | **`V3__time_to_timestamptz.sql`.** REQ-10 Phase 3 의 `food_size` 는 **`V4`** 로 민다 | 이 REQ 가 먼저 나간다(범위—제외). 적용된 마이그레이션은 되돌릴 수 없으므로 번호를 먼저 확정한다 | — |
| **D8** DB `default now()` | **손대지 않는다** | `now()` 는 `timestamptz` 컬럼에서 올바른 순간을 반환한다 — 타입을 바꾸면 07-30 함정이 **자동으로 사라진다**(세션 TZ 와 무관해진다). 이것이 D1 의 부수 효과 중 가장 큰 것이다 | **`default` 제거** — 앱이 SoT 이므로 제거해도 되지만 이 REQ 의 범위가 아니고, 안전망을 없애는 변경은 따로 판단할 일이다 |

## 미결 질문

> ⚠️ 아래는 대화에서 답이 나오지 않았거나 **실측 없이는 고를 수 없는** 것들이다. Phase 0 프로브가 ①②③을 닫는다.

- [ ] **① 엔티티 시각 타입 (D2)** — `OffsetDateTime` vs `Instant` vs `ZonedDateTime`. 판정 기준: ⓐ `timestamptz` 읽기/쓰기가 정확한가 ⓑ Jackson 이 `+09:00` 으로 내보낼 수 있는가 ⓒ 기존 코드 변경량. **`Instant` 는 Jackson 기본이 항상 `Z` 라 D3 과 충돌할 수 있다** — 프로브에서 확인할 것
- [ ] **② `hibernate.jdbc.time_zone: UTC` 를 유지하는가** — `timestamp` 시절엔 "앱이 UTC 로 쓴다"는 뜻이었지만 `timestamptz` 에서는 의미가 달라진다. 남겨야 하는지, 지워야 하는지, 지우면 무엇이 바뀌는지 실측 필요
- [ ] **③ Jackson 이 KST 오프셋을 내는 정확한 설정** — `ObjectMapper.setTimeZone` · `WRITE_DATES_AS_TIMESTAMPS` · `ADJUST_DATES_TO_CONTEXT_TIME_ZONE` 의 조합. **추측하지 말고 실제 응답 문자열로 확인한다** (REQ-15 의 "`@Import` 를 빼면 조용히 틀린 계약을 고정한다"와 같은 자리)
- [ ] **④ Notion 역반영의 범위** — `API I/F` 시각 예시가 `…Z` 로 적힌 행이 몇 개인지 세지 않았다. 「소스 구조」 §6 에 시각 규약 절이 없어 신설이 필요한지도 미확인
- [ ] **⑤ 로컬 DB 없이 어디까지 검증되는가** — 이 세션 머신에는 `.env` 도 Postgres 도 없다. REQ-10 Phase 1·2 의 확인 2건도 같은 이유로 밀려 있다. **`ddl-auto: validate` 통과와 마이그레이션 실행은 DB 없이 확인할 수 없다** — Phase 1 완료 기준이 여기에 걸린다

## 작업 단계

> Phase 1 개 = 커밋 1 개. **Phase 0 을 건너뛰지 않는다** — ①②③이 안 닫힌 채 Phase 1 을 시작하면 엔티티 타입을 두 번 바꾸게 된다.

- [ ] **Phase 0 — 프로브 (엔티티 타입 · Jackson 설정 확정)**
      `timestamptz` 컬럼 하나를 만든 임시 테이블에 세 타입(`OffsetDateTime`·`Instant`·`ZonedDateTime`)을 각각 매핑해 왕복시키고, `@WebMvcTest` 로 **응답 문자열**을 눈으로 확인한다. `hibernate.jdbc.time_zone` 을 켠 상태·끈 상태 둘 다.
      완료 기준: 미결 ①②③이 **실측값과 함께** 닫힘 · 고른 타입으로 `2026-06-30T18:00:00+09:00` 이 실제로 출력되는 것을 확인 · 프로브 코드는 삭제(REQ-10 Phase 0 과 같은 방식, 결과는 커밋 본문에)

- [ ] **Phase 1 — V3 마이그레이션 + 엔티티 타입 전환**
      `V3__time_to_timestamptz.sql` (19 컬럼 · D6 의 `USING`) · 엔티티 6필드 타입 변경 · DTO 시각 필드 타입 변경(6파일).
      완료 기준: 마이그레이션이 로컬 DB 에 적용됨 · `ddl-auto: validate` 통과(엔티티 ↔ 스키마 대조) · `./gradlew test` 전건 통과 · **미결 ⑤ 때문에 DB 있는 환경에서만 판정 가능**

- [ ] **Phase 2 — 직렬화·역직렬화 규약 (`JacksonConfig`)**
      Phase 0 에서 고른 설정 적용. 기존 REQ 컨트롤러 테스트 갱신.
      완료 기준: 응답 시각이 전부 `+09:00` 표기 · `Z` 로 온 요청과 `+09:00` 으로 온 요청이 **같은 순간**으로 저장됨 · 오프셋 없는 요청 값의 동작이 케이스로 고정됨(거부인지 KST 해석인지는 Phase 0 에서 정한다)

- [ ] **Phase 3 — 계산 기준 KST 고정 (`Clock` · `ZoneId` 상수)**
      `framework/constant` 에 `ZoneId` 상수 · `Clock` 빈 · `AuthService` 2곳의 `now()` 교체.
      완료 기준: `LocalDateTime.now()` 직접 호출이 `business`·`framework` 에 0건(`grep`) · 고정 `Clock` 으로 refresh 만료 경계 테스트가 시각에 의존하지 않고 통과 · KST 자정 전후 판정이 케이스로 고정됨

- [ ] **Phase 4 — 문서 역반영**
      Notion 「소스 구조」 시각 규약 절 · `API I/F` 의 `…Z` 예시 · `CLAUDE.md` 계약 승격(신규 시각 컬럼은 `timestamptz`).
      완료 기준: 미결 ④ 가 닫힘 · 원본과 코드가 어긋나는 곳 0건

## 제약·함정

- ⚠️ **적용된 마이그레이션은 한 글자도 못 고친다** (`CLAUDE.md`). `V3` 를 로컬에 한 번 적용하면 수정이 아니라 `V4` 를 새로 써야 한다. **Phase 0 프로브가 끝나기 전에 `V3` 를 적용하지 말 것**
- ⚠️ **`ddl-auto: validate` 는 타입까지 본다.** 엔티티만 바꾸고 마이그레이션을 빠뜨리면(또는 그 반대) **기동 시점에 터진다.** 이건 좋은 실패다 — 조용하지 않다
- ⚠️ **`hibernate.jdbc.time_zone` 은 `timestamptz` 에서 의미가 달라진다.** `timestamp` 시절의 근거로 남겨 두면 안 된다(미결 ②)
- ⚠️ **`LocalDateTime.now()` 는 JVM 기본 TZ 에 암묵 의존한다** (D5). 배포 환경에 `TZ` 가 없으면 **에러 없이** 9시간 어긋난다. 이 REQ 가 끝난 뒤 새 코드가 다시 부르면 규약이 조용히 무너지므로 Phase 3 완료 기준에 `grep` 0건을 넣었다
- ⚠️ **DB `default now()` 로 심은 행과 앱이 쓴 행이 9시간 어긋난다** (2026-07-30 실측) — `timestamptz` 전환으로 **사라지는 함정**이다(D8). 전환 후에는 `now() at time zone 'UTC'` 픽스처 규칙도 함께 폐기해야 한다. 폐기를 빠뜨리면 이번엔 반대로 9시간 어긋난다
- ⚠️ **응답 형태 변경은 클라이언트 계약 변경이다.** `created_at` 이 모든 도메인에서 바뀐다(auth · user · pet · weight · activity). 앱 구현 전이라 지금이 가장 싸다
- ⚠️ **`date` 컬럼은 타입을 바꾸지 않는다** — `measured_at`·`entry_date`·`shed_date` 를 `timestamptz` 로 만들면 "그 날짜"가 순간이 되어 커서 정렬(REQ-10 D8)과 파생 필드 정의(D3)가 전부 흔들린다
- ⚠️ **테스트가 시각을 단언하는 7파일** — 대부분 DTO 생성용 `LocalDateTime.now()` 라 타입만 바뀌면 되지만, `AuthServiceRefreshTest`·`RefreshTokenTest` 는 **만료 경계**를 다뤄 의미가 바뀔 수 있다. `/testrun` 의 (a)/(b) 분류를 그대로 적용할 것
