# DB 스키마 (V1 + V2 + V3)

> ⚠️ **이 문서는 원본이 아니다.** 테이블 정의서·ERD의 1차 출처는 **Notion「PetKok」→ 설계 → 「테이블 정의서」와 DB 탭**이다.
> 이 문서는 구현 편의를 위한 파생 요약이며, 충돌하면 **언제나 Notion이 이긴다.**
>
> | 대상 | 링크 |
> | --- | --- |
> | 프로젝트 루트 (설계 → DB 탭 · DDL 블록) | <https://app.notion.com/p/yjkim97/PetKok-389b81b56e6080f6bfc2f7972108e778> |
> | 📋 테이블 정의서 (컬럼 단위 원본) | <https://app.notion.com/p/38fb81b56e60810d9ae0d4142e3fbfb2> |
> | 🔗 ERD 설계 | <https://app.notion.com/p/38eb81b56e6081119699fc8a600123d6> |
> | 🗂️ 소스 구조 / 아키텍처 설계 (enum·파생 로직 확정) | <https://app.notion.com/p/392b81b56e608185a9ddfa6cc9298b77> |
>
> **스키마 SoT는 Flyway다** → [`src/main/resources/db/migration/V1__init.sql`](../../src/main/resources/db/migration/V1__init.sql)
> Supabase 대시보드 수동 DDL 금지(drift 방지). 변경은 `V2__...` 이후 버전 파일로만.
>
> 최종 대조: **2026-08-31** — 시각 컬럼 19개를 `timestamp` → `timestamptz`로 반영 ([`V3__time_to_timestamptz.sql`](../../src/main/resources/db/migration/V3__time_to_timestamptz.sql), REQ-16 · [ADR-0002](../adr/ADR-0002-time-handling-timestamptz.md)). 이전 대조(2026-07-29, Notion DDL 블록 ↔ `V1__init.sql`, 9개 테이블 전부 일치)는 아래 「대조 이력」 참고.

## 개요

| 항목 | 내용 |
| --- | --- |
| DB | PostgreSQL **17** (Supabase, ADR-002) — 운영 17.6 / 로컬 17.10 (2026-07-29 실측, 메이저 통일) |
| 문자셋 | UTF-8 |
| PK | `uuid` / `default gen_random_uuid()` |
| 마이그레이션 | Flyway (`ddl-auto: validate`) |
| 소프트 딜리트 | `deleted_at timestamptz NULL` — `users`, `pets`만 |
| enum 검증 | 앱 레이어 (`@Enumerated(STRING)`), DB는 `varchar` + CHECK 없음 |
| `created_at` / `updated_at` | JPA Auditing (`@CreatedDate` / `@LastModifiedDate`) — **DB 트리거 없음** |
| FK | 명시적 `REFERENCES`, `ON DELETE`/`ON UPDATE` 미지정 |
| 시각 컬럼 타입 (순간) | `timestamptz` — 저장 = 순간 · 노출 = `+09:00` · 계산 = `Asia/Seoul` (REQ-16, ADR-0002). `created_at`·`updated_at`·`deleted_at`·`fed_at`·`logged_at`·`expires_at`·`revoked_at` 19곳 |
| 날짜만 있는 컬럼 | `date` 유지 — 타임존 개념 없음. `entry_date`·`shed_date`·`measured_at`·`birthday`·`adoption_date`·`taken_at` 6개 |

## 테이블 목록

| # | 테이블 | 설명 | 대상 종 | 베이스 엔티티 |
| :-: | --- | --- | --- | --- |
| 1 | `users` | 사용자 | 공통 | `BaseSoftDeleteEntity` |
| 2 | `user_social_accounts` | 소셜 로그인 인증 (멀티 provider) | 공통 | `BaseCreatedEntity` |
| 3 | `pets` | 반려동물 기본 정보 | 공통 | `BaseSoftDeleteEntity` |
| 4 | `diary_entries` | 다이어리 / 일지 | 공통 | `BaseTimeEntity` |
| 5 | `feeding_logs` | 먹이 급여 기록 | 공통 | `BaseCreatedEntity` |
| 6 | `activity_logs` | 활동 기록 | 개·고양이 + 게코 핸들링 | `BaseCreatedEntity` |
| 7 | `weight_logs` | 체중 기록 | 공통 | `BaseCreatedEntity` |
| 8 | `shed_records` | 탈피 기록 | 🦎 게코 전용 | `BaseCreatedEntity` |
| 9 | `photos` | 갤러리 사진 | 공통 | `BaseCreatedEntity` |
| 10 | `refresh_tokens` | refresh 토큰 저장소 (**V2**, 2026-07-29) | 공통 | `BaseCreatedEntity` |

> 베이스 엔티티 3단계: `BaseCreatedEntity`(created_at) → `BaseTimeEntity`(+updated_at) → `BaseSoftDeleteEntity`(+deleted_at).
> 위치는 `framework`가 아니라 `data/common/entity` — framework는 JPA 매핑 규약을 알지 않는다.

---

## 1. users

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `nickname` | varchar(100) | NOT NULL | — | |
| `email` | varchar(255) | NULL | — | 소셜 provider 제공 여부 불확실 |
| `profile_image_url` | varchar(500) | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |
| `updated_at` | timestamptz | NOT NULL | `now()` | |
| `deleted_at` | timestamptz | NULL | — | NULL=활성 |

| 인덱스 | 컬럼 | 조건 |
| --- | --- | --- |
| `idx_users_email` | `email` | `WHERE deleted_at IS NULL` |
| `idx_users_deleted_at` | `deleted_at` | `WHERE deleted_at IS NOT NULL` |

## 2. user_social_accounts

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `user_id` | uuid | NOT NULL | — | FK → `users.id` |
| `provider` | varchar(20) | NOT NULL | — | `KAKAO` \| `GOOGLE` \| `APPLE` |
| `provider_user_id` | varchar(255) | NOT NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

- 제약 `uq_social_provider` UNIQUE `(provider, provider_user_id)` — 중복 가입 방지, 위반 시 `SOCIAL_ALREADY_LINKED`(409)
- 인덱스 `idx_social_accounts_user_id` (`user_id`)

## 3. pets

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `user_id` | uuid | NOT NULL | — | FK → `users.id` |
| `name` | varchar(100) | NOT NULL | — | |
| `species` | varchar(50) | NOT NULL | — | `CRESTED_GECKO` \| `DOG` \| `CAT` |
| `breed` | varchar(100) | NULL | — | |
| `gender` | varchar(20) | NULL | — | `MALE` \| `FEMALE` \| `UNKNOWN` |
| `birthday` | date | NULL | — | |
| `adoption_date` | date | NULL | — | |
| `profile_image_url` | varchar(500) | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |
| `updated_at` | timestamptz | NOT NULL | `now()` | |
| `deleted_at` | timestamptz | NULL | — | |

| 인덱스 | 컬럼 | 조건 |
| --- | --- | --- |
| `idx_pets_user_id` | `user_id` | `WHERE deleted_at IS NULL` |
| `idx_pets_deleted_at` | `deleted_at` | `WHERE deleted_at IS NOT NULL` |

## 4. diary_entries

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `title` | varchar(200) | NULL | — | |
| `content` | text | NULL | — | |
| `condition_tag` | varchar(50) | NULL | — | **7종** ↓ |
| `entry_date` | date | NOT NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |
| `updated_at` | timestamptz | NOT NULL | `now()` | |

> ⚠️ **`condition_tag`는 7종이다** — 공통 `정상` / `활발` + 🦎 게코 전용 `거식` / `탈피도와줌` / `탈피완료` / `거꾸리` / `구토`.
> Notion 「소스 구조」 §8 확정값이며 `shed_records.is_assisted = true → '탈피도와줌'` 연계 규칙이 같은 값을 참조한다.
> **`V1__init.sql`의 주석은 4종(`정상 \| 활발 \| 거꾸리 \| 구토`)만 적혀 있다** — 주석이라 스키마 영향은 없지만, diary 도메인 enum을 만들 때 주석만 보면 게코 전용 3종이 누락된다. 정정은 「소스 구조」 '다음 단계'에 미결로 등재되어 있다.

| 인덱스 | 컬럼 | 조건 | 목적 |
| --- | --- | --- | --- |
| `idx_diary_pet_date` | `(pet_id, entry_date DESC)` | — | 타임라인 메인 피드 |
| `idx_diary_pet_condition` | `(pet_id, condition_tag, entry_date DESC)` | `WHERE condition_tag IS NOT NULL` | 거꾸리·거식 기간 집계 |

## 5. feeding_logs

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `food_type` | varchar(100) | NULL | — | 예: 귀뚜라미, 건식사료 |
| `amount` | decimal(8,2) | NULL | — | |
| `amount_unit` | varchar(20) | NULL | — | 예: `g`, `ml`, `마리` |
| `is_refused` | boolean | NOT NULL | `false` | 거식 스트릭 계산 입력 |
| `fed_at` | timestamptz | NOT NULL | — | |
| `memo` | text | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

- 비즈니스 규칙: `is_refused = true` 3일 연속 → ⚠️ CAUTION, 7일 연속 → 🚨 DANGER (저장하지 않고 조회 시 계산)

| 인덱스 | 컬럼 | 조건 |
| --- | --- | --- |
| `idx_feeding_pet_fed_at` | `(pet_id, fed_at DESC)` | — |
| `idx_feeding_pet_refused` | `(pet_id, fed_at DESC)` | `WHERE is_refused = true` |

## 6. activity_logs

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `activity_type` | varchar(50) | NOT NULL | — | `WALK` \| `PLAY` \| `GROOMING` \| `TRAINING` \| `HANDLING` |
| `duration_minutes` | int | NULL | — | |
| `distance_km` | decimal(6,2) | NULL | — | 실내 활동·게코는 사용하지 않음 |
| `memo` | text | NULL | — | |
| `logged_at` | timestamptz | NOT NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

- 종별 조건: 게코 = `HANDLING`만, 개·고양이 = 나머지 4종
- 인덱스 `idx_activity_pet_logged_at` (`pet_id, logged_at DESC`)

## 7. weight_logs

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `weight_g` | int | NOT NULL | — | **그램 단위 통일** (게코 수십g ~ 대형견 수십kg) |
| `measured_at` | date | NOT NULL | — | |
| `memo` | varchar(500) | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

- 인덱스 `idx_weight_pet_measured_at` (`pet_id, measured_at DESC`)

## 8. shed_records (🦎 게코 전용)

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `shed_date` | date | NOT NULL | — | |
| `is_complete` | boolean | NOT NULL | **`true`** | 완전 탈피 여부 |
| `is_assisted` | boolean | NOT NULL | `false` | 탈피 도와줌 여부 |
| `memo` | varchar(500) | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

- 비즈니스 규칙: 최근 3개 `shed_date` 간격 평균 → 다음 탈피 예상일. `is_assisted = true` ↔ `condition_tag = '탈피도와줌'` 연계
- `species = CRESTED_GECKO`가 아니면 Service에서 `BusinessException`
- 인덱스 `idx_shed_pet_date` (`pet_id, shed_date DESC`)

## 9. photos

| 컬럼 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | :-: | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK |
| `pet_id` | uuid | NOT NULL | — | FK → `pets.id` |
| `diary_entry_id` | uuid | NULL | — | FK → `diary_entries.id`. NULL = 단독 갤러리 |
| `image_url` | varchar(1000) | NOT NULL | — | Cloudflare R2 Custom Domain URL |
| `caption` | varchar(500) | NULL | — | |
| `taken_at` | date | NULL | — | |
| `created_at` | timestamptz | NOT NULL | `now()` | |

| 인덱스 | 컬럼 | 조건 |
| --- | --- | --- |
| `idx_photos_pet_created_at` | `(pet_id, created_at DESC)` | — |
| `idx_photos_diary_entry_id` | `diary_entry_id` | `WHERE diary_entry_id IS NOT NULL` |

---

## 10. refresh_tokens (V2)

> refresh 토큰 저장소. **저장소 = DB 확정(2026-07-23)** — Redis는 기각(로그아웃·탈퇴 시 즉시 무효화 필요, 인프라를 늘릴 이유 없음).
> 원본: [`V2__refresh_tokens.sql`](../../src/main/resources/db/migration/V2__refresh_tokens.sql) · 엔티티: `data/auth/entity/RefreshToken`

| 컬럼 | 타입 | NULL | 기본값 | 제약 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NOT NULL | `gen_random_uuid()` | PK | |
| `user_id` | uuid | NOT NULL | — | FK → `users.id` | 아래 ⚠️ 참고 — 엔티티는 연관관계가 아니다 |
| `token_hash` | varchar(64) | NOT NULL | — | UNIQUE | **토큰 원문 미저장.** SHA-256 hex는 **64자 고정** |
| `expires_at` | timestamptz | NOT NULL | — | | 만료 시각 |
| `revoked_at` | timestamptz | NULL | — | | 무효화 시각. NULL=유효 |
| `created_at` | timestamptz | NOT NULL | `now()` | | 발급일시 |

| 인덱스/제약 | 컬럼 | 목적 |
| --- | --- | --- |
| `uq_refresh_tokens_token_hash` | `(token_hash)` | 해시 중복 방지 |
| `idx_refresh_tokens_user_id` | `user_id` | 재사용 감지 시 "해당 사용자 전체 revoke" · 로그아웃 |

**비즈니스 규칙**

- **로테이션** — refresh 호출마다 새 토큰 발급 + 기존 토큰 즉시 revoke
- **재사용 감지** — `revoked_at`이 찍힌 토큰이 재제시되면 탈취로 간주, 해당 사용자 전체 revoke 후 `INVALID_TOKEN`(401). 전용 ErrorCode는 두지 않는다(탐지 여부 노출 방지)
- 만료 행 정리 배치는 auth 구현 범위 제외 — 스케줄러 도입 결정이 함께 필요하다

> ⚠️ **`user_id`는 엔티티에서 `@ManyToOne`이 아니라 생 `UUID` 컬럼이다.** `RefreshToken`은 `data/auth`, `User`는 `data/user`라 연관관계를 걸면 `data/auth → data/user` 도메인 간 참조가 되어 ArchUnit에 걸린다. 토큰 행에서 User로 탐색할 일이 없어 잃는 것도 없다. **DB의 FK 제약은 그대로 있다** — 객체 매핑만 끊은 것이다.

> `updated_at`이 없다. 토큰 행은 발급 후 `revoked_at`이 한 번 찍힐 뿐이라 `BaseCreatedEntity`를 상속한다.

---

## 공통 설계 원칙

### `updated_at` — JPA Auditing (2026-07-03 확정)

`updated_at`의 SoT는 **앱(Spring)**이다. `BaseTimeEntity`의 `@LastModifiedDate`로 갱신하며 **DB 트리거를 두지 않는다** — 모든 쓰기가 Spring 앱 단일 경로이기 때문. 이전 설계에 있던 `set_updated_at()` 함수와 `trg_*_updated_at` 트리거는 사용하지 않으며 `V1__init.sql`에도 없다.

⚠️ 앱 밖에서 DB를 직접 수정하면 갱신되지 않는다. 배치·외부 쓰기 도입 시 재검토.

### 소프트 딜리트

- 적용: `users`, `pets`만
- 삭제 `UPDATE SET deleted_at = now()` / 조회 `WHERE deleted_at IS NULL`
- 구현은 Hibernate `@SQLDelete` + `@SQLRestriction("deleted_at is null")`. ⚠️ 네이티브 쿼리·조인은 조건을 명시할 것
- 부분 인덱스로 활성 레코드만 인덱싱

### enum 검증

앱 레이어에서만 검증한다(Java Enum + `@Enumerated(STRING)`, DB는 varchar, CHECK 제약 없음). 신규 값 추가 시 `ALTER TABLE` 없이 앱 배포만으로 처리하기 위함.

| 컬럼 | 값 |
| --- | --- |
| `user_social_accounts.provider` | `KAKAO` \| `GOOGLE` \| `APPLE` |
| `pets.species` | `CRESTED_GECKO` \| `DOG` \| `CAT` |
| `pets.gender` | `MALE` \| `FEMALE` \| `UNKNOWN` |
| `activity_logs.activity_type` | `WALK` \| `PLAY` \| `GROOMING` \| `TRAINING` \| `HANDLING` |
| `diary_entries.condition_tag` | `정상` \| `활발` \| `거식` \| `탈피도와줌` \| `탈피완료` \| `거꾸리` \| `구토` |

### 파생 상태는 저장하지 않는다

거식 스트릭 · 탈피 예측 · 거꾸리 경고는 컬럼으로 두지 않고 조회 시 계산한다(I/O 없는 순수 `*Calculator`). 스키마에 파생 컬럼을 추가하려는 변경은 이 원칙과 먼저 대조할 것.

---

## 대조 이력 · 미결 사항

**2026-07-29 대조 결과** — Notion DDL 블록 ↔ `V1__init.sql`: 9개 테이블의 컬럼·타입·NULL·기본값·제약·인덱스(부분 인덱스 WHERE 조건 포함) **전부 일치**. 차이는 SQL 주석 1건뿐이다.

| 항목 | 상태 |
| --- | --- |
| `V1__init.sql:54` `condition_tag` 주석이 4종만 기재 (확정값은 7종) | ⬜ 미정정 — 아래 ⚠️ 참고. **주석이라도 그냥 못 고친다** |

> ⚠️ **적용이 끝난 마이그레이션 파일은 주석 한 글자도 고칠 수 없다.** Flyway는 `flyway_schema_history`에 저장한 체크섬을 기동 시 대조하므로(`validateOnMigrate` 기본 `true`), `V1__init.sql`을 수정하면 이미 V1을 적용한 모든 DB에서 **체크섬 불일치로 기동이 막힌다.** 로컬은 `flyway repair`로 풀 수 있지만 운영 DB에도 같은 조치가 필요하다.
> 따라서 위 주석 정정은 ① 그냥 두고 이 문서·정의서를 SoT로 삼거나 ② `V3__` 코멘트 마이그레이션으로 처리하거나 ③ 아직 V1을 적용한 환경이 로컬뿐인 지금 `repair`로 밀어붙이는 선택지가 있다. **"주석이니까 안전하다"는 판단만 하지 말 것** — Notion 「소스 구조」 '다음 단계'에 이 항목이 그대로 등재돼 있다.
| `feeding_logs.amount_unit` 단위 예시 주석이 V1에 없음 | ℹ️ 조치 불필요 (본 문서·정의서에 기재) |

**2026-08-31 대조 결과** — `V3__time_to_timestamptz.sql` 적용: 시각 컬럼 19개(`created_at`·`updated_at`·`deleted_at`·`fed_at`·`logged_at`·`expires_at`·`revoked_at`) `timestamp` → `timestamptz`. Notion 「테이블 정의서」(컬럼 단위 원본)에 같은 19컬럼을 역반영했다. `date` 컬럼 6개(`entry_date`·`shed_date`·`measured_at`·`birthday`·`adoption_date`·`taken_at`)는 대상 밖 — 타임존 개념이 없다(REQ-16, ADR-0002).

> ⚠️ **탭 본문 2곳은 API로 역반영이 안 돼 사람 손 대기 중이다** — 「설계」→ API 탭의 "날짜 포맷" 행(`2026-06-30T15:00:00Z` 그대로), 「설계」→ DB 탭의 DDL 코드블록(`timestamp` 그대로). `notion-update-page`가 `validation_error: ... is not a page or database`로 거부한다 — 탭 페이지 본문은 API로 못 고치는 Notion 객체 타입이다. 반면 「테이블 정의서」(행 단위 데이터베이스)와 ERD 페이지는 API로 고쳐진다.

> ℹ️ `V1__init.sql:54` `condition_tag` 주석 4종 미정정 건(아래)은 V3의 대상이 아니다 — 별개 미결로 남아 있다.

**`refresh_tokens` (V2) — 2026-07-29 구현 + Notion 역반영 완료**
「소스 구조」 §7에만 있던 결정(2026-07-23)을 `V2__refresh_tokens.sql`로 구현하고 **Notion 테이블 정의서에 §10으로 역반영했다**(저장소가 Notion보다 앞선 케이스가 닫혔다). 스키마는 아래 §10 참조.
