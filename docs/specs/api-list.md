# API 목록 (구현 예정)

> ⚠️ **이 문서는 원본이 아니다.** API 계약의 1차 출처는 **Notion「PetKok」→ 설계 → API 탭 → `API I/F` 데이터베이스**다.
> 이 문서는 구현 편의를 위한 파생 요약이며, 충돌하면 **언제나 Notion이 이긴다.**
> 원본: <https://app.notion.com/p/yjkim97/PetKok-389b81b56e6080f6bfc2f7972108e778>
>
> 최종 대조: 2026-07-27 (Notion API I/F 39개 엔드포인트 기준)

구현 순서는 Notion 소스 구조 문서를 따른다: **auth → user → pet → 기록 도메인 → timeline**.
상태: **설계 초안.** 아직 구현된 엔드포인트는 없다.

## 공통 규약

- Base URL `/api/v1`, 응답 필드는 전역 snake_case
- 모든 응답은 `ApiResponse<T>` 래퍼 (`{data, error}`)
- 목록 조회는 커서 기반 페이지네이션 — 응답은 `{items, next_cursor, has_next}` (`CursorPage`). `next_cursor`가 `null`이면 마지막 페이지
- 요청 파라미터: `cursor`, `limit` (기본 20 / 최대 100, `CursorRequest`)
- 인증: `Authorization: Bearer <access_token>`
- 날짜 포맷: ISO 8601 (`2026-06-30`, `2026-06-30T15:00:00Z`)
- `/pets/{pet_id}` 하위 리소스는 모두 소유권 검증 대상 → `PetAccessGuard.getOwnedPet(petId, userId)` → `PET_FORBIDDEN` / `PET_NOT_FOUND`
- 리소스 수정은 `PATCH`로 통일한다 (Notion API I/F에도 `PUT` 사용처가 없다)

---

## 1. Auth `/api/v1/auth`

| Method | Path | access 토큰 | 설명 |
| --- | --- | :---: | --- |
| POST | `/auth/kakao` | 불필요 | 카카오 인가코드 → 로그인/자동가입, access+refresh 발급 |
| POST | `/auth/refresh` | 불필요 | refresh 토큰을 **body로** 받아 access+refresh 재발급 (로테이션) |
| DELETE | `/auth/logout` | **필요** 🔒 | **Request Body 없음** → access 토큰으로 사용자를 식별해 refresh revoke. 204 |

> ⚠️ **`SecurityConfig.PUBLIC_PATHS` 수정이 필요하다.** 현재 값 `/api/v1/auth/**`(전체 permitAll)를 그대로 두면 **`/auth/logout`이 무인증 노출된다.** 아래 "공개 경로 범위" 참고.

`/auth/refresh` 응답은 로테이션에 따라 `access_token` + **새 `refresh_token`** 을 함께 반환한다. 클라이언트는 저장된 refresh 토큰을 교체해야 한다.

`user_social_accounts.provider`는 `KAKAO | GOOGLE | APPLE`을 허용한다. 구글·애플은 동일 형태로 확장 예정이며 이번 범위가 아니다.

**Kakao는 커스텀 플로우다.** 클라이언트가 받은 `authorization_code`를 서버가 넘겨받아 카카오 토큰·프로필 엔드포인트를 호출한다 — spring-security-oauth2-client의 리다이렉트 로그인 플로우가 아니다(그건 서버렌더링용).

**자동가입**: 프로필 수령 후 `(provider, provider_user_id)`로 조회, 없으면 `users` + `user_social_accounts` 생성. `UNIQUE (provider, provider_user_id)`로 중복을 막고 위반 시 `SOCIAL_ALREADY_LINKED`(409).

## 2. User `/api/v1/users`

| Method | Path | 인증 | 설명 |
| --- | --- | :---: | --- |
| GET | `/users/me` | 🔒 | 내 프로필 |
| PATCH | `/users/me` | 🔒 | 닉네임·프로필 이미지 수정. **Validation(2026-08-27 Notion 명시)**: `nickname` 트림 후 1~100자(`""`·공백만 400, 중복 허용) · `profile_image_url` ≤500자 · 누락·`null` = 변경 없음 |
| DELETE | `/users/me` | 🔒 | 회원 탈퇴 (soft delete) |
| DELETE | `/users/me/profile-image` | 🔒 | 프로필 이미지 제거 (`profile_image_url` = null). 이미 없어도 204 (멱등). **2026-08-27 Notion 행 추가** — `PATCH /users/me` 가 누락·`null` 을 모두 "변경 없음"으로 두므로(REQ-08 D3) 제거 신호를 실을 자리가 없어 분리했다 |

> Notion API I/F에 **소셜 계정 목록·연결·해제 엔드포인트는 없다.** 이전 판의 이 문서가 `/users/me/social-accounts` 3종을 임의로 추가했었으나 원본에 근거가 없어 제거했다. 필요하다면 Notion에 먼저 추가한 뒤 이 문서에 반영한다.

## 3. Pet `/api/v1/pets`

| Method | Path | 인증 | 설명 |
| --- | --- | :---: | --- |
| POST | `/pets` | 🔒 | 등록 (`species`: `CRESTED_GECKO` / `DOG` / `CAT`) |
| GET | `/pets` | 🔒 | 내 반려동물 목록 |
| GET | `/pets/{pet_id}` | 🔒 | 상세 조회 |
| PATCH | `/pets/{pet_id}` | 🔒 | 수정 |
| DELETE | `/pets/{pet_id}` | 🔒 | 삭제 (soft delete) |

이후 모든 하위 리소스는 `PetAccessGuard`로 소유권을 검증한다 (소유권 앵커).

## 4. Diary `/api/v1/pets/{pet_id}/diary`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/diary` | 목록 (커서, `entry_date` desc) |
| POST | `/diary` | 작성 |
| GET | `/diary/{entry_id}` | 상세 |
| PATCH | `/diary/{entry_id}` | 수정 |
| DELETE | `/diary/{entry_id}` | 삭제 |

`condition_tag`는 관찰 기록만 저장하고 경고는 조회 시 계산한다 (거꾸리 경고 → `DiaryService`).

**`condition_tag` 허용값은 7개다** (확정 — 2026-07-27):

`정상` · `활발` · `거식` · `탈피도와줌` · `탈피완료` · `거꾸리` · `구토`

- 공통(`정상`·`활발`) + 게코 전용(나머지 5개) 구분은 Notion ERD 설계에 명시돼 있다
- 테이블 정의서의 `shed_records` 규칙도 `is_assisted = true → condition_tag '탈피도와줌'과 연계`로 같은 값을 참조한다

> ⚠️ **`V1__init.sql`의 주석은 4개(`정상 | 활발 | 거꾸리 | 구토`)로 3개가 빠져 있다.** 컬럼이 `varchar(50)`이고 DB CHECK가 없어 스키마 변경은 불필요하지만, **`ConditionTag` enum은 7개로 만들고 V1 주석도 함께 고쳐야 한다.**

## 5. Feeding `/api/v1/pets/{pet_id}/feeding`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/feeding` | 목록 (커서, `fed_at` desc) |
| POST | `/feeding` | 기록 |
| PATCH | `/feeding/{log_id}` | 수정 |
| DELETE | `/feeding/{log_id}` | 삭제 |
| GET | `/feeding/anorexia-streak` | 🦎 거식 스트릭 조회 |

**거식 스트릭** (`AnorexiaStreakCalculator` — I/O 없는 순수 클래스)
- 출력: `{current_streak_days, level, last_eaten_at}`
- `level`: `NONE` / `CAUTION`(3일+) / `DANGER`(7일+)

급여 크기(소/중/대) UI 칩은 `amount` + `amount_unit`으로 매핑한다. 엔티티는 그대로 둔다.

## 6. Activity `/api/v1/pets/{pet_id}/activity`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/activity` | 목록 (커서, `logged_at` desc) |
| POST | `/activity` | 기록 |
| PATCH | `/activity/{log_id}` | 수정 |
| DELETE | `/activity/{log_id}` | 삭제 |

**종별 제약**: `activity_type` = `WALK | PLAY | GROOMING | TRAINING | HANDLING`
- 게코 → `HANDLING`만 허용, `distance_km` 미사용
- 개·고양이 → `WALK | PLAY | GROOMING | TRAINING`
- 위반 시 `INVALID_SPECIES_ACTIVITY`

## 7. Weight `/api/v1/pets/{pet_id}/weight`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/weight` | 목록 (커서, `measured_at` desc · `id` desc) |
| POST | `/weight` | 기록 |
| PATCH | `/weight/{log_id}` | 수정 |
| DELETE | `/weight/{log_id}` | 삭제 |

단위는 그램(g)으로 통일한다 (게코 수십g ~ 대형견 수십kg).

> 이전 판에 있던 `GET /weights/chart`(기간별 추이)는 Notion API I/F에 없어 제거했다.
> **파생 필드 (2026-08-28, Notion 「체중 목록」 행 확정)** — 목록 항목·201 응답에 `weight_change_rate`(직전 대비 %, 소수 1자리, 첫 기록 `null`) · `is_weight_warning`(`|변화율| >= 20`, 첫 기록 `false`). 직전 = `measured_at` desc, `id` desc 정렬의 바로 다음 1건. 저장하지 않고 조회 시 계산.

## 8. Shed `/api/v1/pets/{pet_id}/shed` 🦎

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/shed` | 목록 (커서, `shed_date` desc) |
| POST | `/shed` | 기록 |
| PATCH | `/shed/{record_id}` | 수정 |
| DELETE | `/shed/{record_id}` | 삭제 |
| GET | `/shed/prediction` | 🦎 다음 탈피 예측 |

**종별 제약**: 크레스티드 게코 전용 → 그 외 종은 `SHED_NOT_SUPPORTED_SPECIES`

**탈피 예측** (`ShedPredictionCalculator` — I/O 없는 순수 클래스)
- 출력: `{predicted_date, average_cycle_days, based_on_records, confidence}`
- 최근 3개 기록의 간격 평균으로 산출
- `confidence`: `LOW`(기록 1개) / `MEDIUM`(2개) / `HIGH`(3개 이상)

## 9. Photos `/api/v1`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/photos/presigned-url` | R2 업로드 URL 발급 (**펫 경로 밖**) |
| GET | `/pets/{pet_id}/photos` | 갤러리 목록 (커서, `created_at` desc) |
| POST | `/pets/{pet_id}/photos` | 업로드 완료 후 메타데이터 저장 |
| DELETE | `/pets/{pet_id}/photos/{photo_id}` | 삭제 |

R2 2단계 업로드다 — presigned URL로 클라이언트가 직접 올린 뒤 메타데이터만 서버에 저장한다.
`photos.diary_entry_id`가 `NULL`이면 단독 갤러리, 값이 있으면 일기 첨부다.

> 이전 판의 `/photos/upload-url`(경로 오기), `GET /photos/{id}`(상세), `PATCH /photos/{id}`(캡션 수정)는 Notion API I/F에 없어 제거했다.

## 10. Timeline `/api/v1/pets/{pet_id}/timeline`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/timeline` | 일기·급여·활동·체중·탈피 통합 시간순 (커서) |

자체 테이블·엔티티·리포지토리가 없는 **read 전용 모델**이다. 각 도메인 리포지토리의 기간 조회를 조합한다.

- **옵션 A (Notion 추천): 앱 레벨 병합** — 각 리포지토리 조회 후 Service에서 날짜순 merge/정렬. 도메인 경계 유지, 타입 안전
- 옵션 B: 네이티브 `UNION ALL` — 성능은 낫지만 매핑 복잡도가 오른다. 무한스크롤 대량 조회에서 병목이 확인되면 도입

> 이전 판은 "QueryDSL 활성화 시점이 이 API"라고 적었으나, Notion은 옵션 A를 추천하고 옵션 B를 병목 시 대안으로 둔다. **QueryDSL 도입은 옵션 B를 택할 때의 이야기다.**

**캘린더 도트**(월 단위, 날짜별 status 요약 + 최대 3 dot + `+N`)는 Notion 소스 구조 §9에 설계만 있고 **API I/F에 엔드포인트가 정의되지 않았다.** 구현 전 Notion에 추가가 필요하다.

---

## refresh 토큰 저장소 (결정됨 — 2026-07-23)

> Notion 소스 구조 §7은 이 항목을 "이후 결정 — DB/Redis"로 열어두고 있다. 아래 결정이 그보다 앞서므로 **Notion에 역반영이 필요하다.**

`refresh_tokens` 테이블을 **V2 마이그레이션으로 추가**한다. stateless refresh를 쓰지 않는 이유는 로그아웃·회원 탈퇴 시 즉시 무효화가 가능해야 하기 때문이다.

제안 스키마 (auth 구현 시 확정):

```sql
create table refresh_tokens (
    id         uuid         primary key default gen_random_uuid(),
    user_id    uuid         not null references users (id),
    token_hash varchar(64)  not null,   -- 원문 저장 금지. SHA256Util 해시 (hex 64자 고정)
    expires_at timestamp    not null,
    revoked_at timestamp,
    created_at timestamp    not null default now()
);
create unique index uq_refresh_token_hash on refresh_tokens (token_hash);
create index idx_refresh_user_id on refresh_tokens (user_id) where revoked_at is null;
```

- 엔티티는 `BaseCreatedEntity` 상속 (`created_at`만 필요 — 무효화는 `revoked_at`으로 표현하며 소프트 딜리트가 아니다)
- 토큰 원문은 저장하지 않는다. DB 유출 시 그대로 재사용 가능해지기 때문 (`SHA256Util` 사용)
- `DELETE /auth/logout` → **해당 사용자의 refresh 토큰 전체** `revoked_at` 설정. 원본(Notion `API I/F`)은 "Refresh Token 무효화. Request Body 없음"만 규정해 **범위를 말하지 않는다** — Body 가 없어 특정 토큰을 지목할 수 없으므로 전체 revoke 가 유일한 구현 해석이다(2026-08-27 확정, REQ-07-18). 대가는 기기별 로그아웃 불가. 이 문서 이전 판의 "해당 토큰"은 원본에 없는 문구였다
- `DELETE /users/me` (탈퇴) → **revoke하지 않는다** (REQ-08 D5). 필터의 활성 사용자 검사가 탈퇴 계정의 access 토큰을 즉시 차단하므로, refresh로 새 토큰을 받아도 결국 막힌다. revoke하면 `business/user → data/auth` 참조가 생겨 ArchUnit 예외가 4→5로 는다
	- ⚠️ 이 문서의 이전 판은 "해당 사용자 토큰 전체 revoke"라고 적고 있었으나 **어느 원본에도 근거가 없었다**(2026-08-04 Notion 대조). `API I/F` → 회원 탈퇴는 "소프트 딜리트 / 204"만 규정하고, 테이블 정의서 §10의 `revoked_at`은 "로테이션·로그아웃·재사용 감지로 찍힌다"로 **탈퇴를 빼고** 있다
	- 테이블 정의서 §10의 저장소 선택 근거에는 "로그아웃·**탈퇴** 시 즉시 무효화가 필요"라는 문장이 남아 있다(2026-07-23). Redis 기각 논거이지 탈퇴 동작 명세가 아니며 위 서술보다 오래됐다 — **Notion 역반영 대상**
	- 대가: `refresh_tokens`에 `revoked_at IS NULL` 행이 남고, 탈퇴한 사용자도 `/auth/refresh`가 200을 반환한다(그 토큰으로 API를 부르면 401)

**로테이션을 적용한다.** `POST /auth/refresh` 호출 시마다 새 refresh 토큰을 발급하고 기존 토큰은 즉시 revoke한다. 응답에는 access·refresh를 함께 담아 클라이언트가 저장된 refresh를 교체하도록 한다.

- **재사용 감지**: 이미 `revoked_at`이 찍힌 토큰이 제시되면 탈취로 간주하고 해당 사용자의 모든 refresh 토큰을 revoke한 뒤 `INVALID_TOKEN`(401)을 반환한다. 정상 클라이언트라면 revoke된 토큰을 다시 보낼 일이 없다
- 기존 `ErrorCode.INVALID_TOKEN`으로 충분하다 — 재사용 감지용 코드를 따로 만들지 않는다. 공격자에게 탐지 여부를 알려줄 이유가 없다
- **만료 행 정리**: `expires_at`이 지난 행은 누적되기만 한다. 정리 배치는 auth 구현 범위에 넣지 않고, 운영 부담이 실제로 보이는 시점에 별도로 다룬다 (스케줄러 도입 결정이 함께 필요하므로)

## 공개 경로 범위 (확정 — 2026-07-27)

**원칙**: `SecurityConfig.PUBLIC_PATHS`에는 **토큰 없이 호출되는 엔드포인트만** 둔다. 인증이 필요한 기능을 permitAll 범위에 두면 무인증으로 노출된다 (AGENTS §5 계약).

**적용**: 와일드카드 `/api/v1/auth/**`를 쓰지 않고 **개별 경로로 나열한다.**

```java
PUBLIC_PATHS = { "/api/v1/auth/kakao", "/api/v1/auth/refresh", "/actuator/health" }
```

`DELETE /api/v1/auth/logout`은 **인증이 필요하므로 permitAll에서 제외**한다.

근거 — Notion API I/F 각 행의 본문:

| 엔드포인트 | 본문 기재 | 판정 |
| --- | --- | --- |
| `POST /auth/refresh` | "🔓 인증 불필요", Request Body `{refresh_token}` | Authorization 헤더 없이 동작 → **공개** |
| `DELETE /auth/logout` | "🔒 인증 필요. **Request Body 없음**", 204 | body가 없어 access 토큰으로만 사용자 식별 가능 → **인증 필요** |

> 2026-07-23 결정은 "`/auth/logout`은 refresh 토큰을 body로 받으므로 access 토큰 없이 동작한다"는 전제로 `/auth/**` 전체를 permitAll로 두었다. **그 전제가 틀렸다** — 원본 스펙의 logout은 body가 없다. 원칙은 그대로고 적용만 바뀐다.
>
> API I/F 그리드의 `🔒 인증 필요` 체크박스는 `/auth/refresh`에서 본문과 어긋나 있었다(2026-07-27 Notion에서 해제 완료). **체크박스보다 행 본문이 정확하다.**

## 부분 수정은 PATCH (결정됨 — 2026-07-23)

리소스 수정은 PATCH(부분 수정)로 통일한다. PUT(전체 교체)은 사용하지 않는다. 클라이언트가 수정 화면에서 일부 필드만 보내는 경우가 대부분이고, PUT으로 받으면 누락 필드를 `null` 덮어쓰기와 구분할 수 없기 때문이다.

Notion API I/F의 39개 엔드포인트에도 `PUT` 사용처가 없어 원본과 일치한다. 이 규칙은 AGENTS.md §5에도 반영했다.
