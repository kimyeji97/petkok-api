# API 목록 (구현 예정)

PetKok API의 구현 예정 엔드포인트 목록이다. `V1__init.sql`의 9개 테이블과 `ErrorCode`를 기준으로 작성했다.
구현 순서는 README "다음 단계"를 따른다: **auth → user → pet → 기록 도메인 → timeline**.

> 상태: **설계 초안.** 아직 구현된 엔드포인트는 없다. 구현하면서 확정된 내용은 이 문서에 반영한다.

## 공통 규약

- 모든 응답은 `ApiResponse<T>` 래퍼 (`{data, error}`), 전역 snake_case
- 목록 조회는 커서 기반 페이지네이션 (`cursor`, `limit` — 기본 20 / 최대 100, `CursorRequest`)
- 인증: `Authorization: Bearer <access_token>`. `/api/v1/auth/**`와 `/actuator/health`만 공개 (`SecurityConfig`)
- `/pets/{petId}` 하위 리소스는 모두 소유권 검증 대상 → `PET_FORBIDDEN` / `PET_NOT_FOUND`

---

## 1. Auth `/api/v1/auth` (공개)

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/auth/kakao` | 카카오 인가코드 → 로그인/자동가입, access+refresh 발급 |
| POST | `/auth/refresh` | refresh 토큰으로 access 재발급 |
| POST | `/auth/logout` | refresh 무효화 (저장된 토큰 revoke) |

`user_social_accounts.provider`는 `KAKAO | GOOGLE | APPLE`을 허용한다. 구글·애플은 동일 형태로 확장한다.

**refresh 토큰은 DB에 저장한다** (`refresh_tokens` 테이블, V2 마이그레이션으로 추가). stateless refresh 대신 저장소를 두는 이유는 로그아웃·탈퇴 시 즉시 무효화가 가능해야 하기 때문이다. 상세는 아래 "refresh 토큰 저장소" 참고.

## 2. User `/api/v1/users`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/users/me` | 내 프로필 |
| PATCH | `/users/me` | 닉네임·프로필 이미지 수정 |
| DELETE | `/users/me` | 회원 탈퇴 (soft delete) |
| GET | `/users/me/social-accounts` | 연결된 소셜 계정 목록 |
| POST | `/users/me/social-accounts` | 소셜 계정 추가 연결 → `SOCIAL_ALREADY_LINKED` |
| DELETE | `/users/me/social-accounts/{id}` | 연결 해제 |

소셜 연결은 인증이 필요하므로 `/auth/` 아래에 두지 않는다 (아래 "설계 미결정" 2번 참고).

## 3. Pet `/api/v1/pets`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/pets` | 등록 (`species`: `CRESTED_GECKO` / `DOG` / `CAT`) |
| GET | `/pets` | 내 반려동물 목록 |
| GET | `/pets/{petId}` | 상세 조회 |
| PATCH | `/pets/{petId}` | 수정 |
| DELETE | `/pets/{petId}` | 삭제 (soft delete) |

이후 모든 하위 리소스는 `PetAccessGuard`로 소유권을 검증한다 (README "다음 단계" 1번의 소유권 앵커).

## 4~8. 기록 도메인 `/api/v1/pets/{petId}/…`

다섯 도메인 모두 아래 5종이 기본이다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/{resource}` | 등록 |
| GET | `/{resource}` | 목록 (커서) |
| GET | `/{resource}/{id}` | 상세 |
| PATCH | `/{resource}/{id}` | 수정 |
| DELETE | `/{resource}/{id}` | 삭제 (hard delete — soft delete는 users·pets만) |

도메인별 정렬·필터와 추가 엔드포인트는 다음과 같다. 필터 조건은 `V1__init.sql`의 기존 인덱스와 일치시켰다.

| 리소스 | 경로 | 목록 정렬·필터 | 추가 엔드포인트 |
| --- | --- | --- | --- |
| 일기 | `/diaries` | `entry_date` desc, `condition_tag` 필터 | — |
| 급여 | `/feedings` | `fed_at` desc, `is_refused` 필터 | `GET /feedings/anorexia-streak` — 거식 연속일수 |
| 활동 | `/activities` | `logged_at` desc | — |
| 체중 | `/weights` | `measured_at` desc | `GET /weights/chart` — 기간별 추이 |
| 탈피 🦎 | `/sheds` | `shed_date` desc | `GET /sheds/prediction` — 다음 탈피 예측 |

**종별 제약**

- 활동: `activity_type`(`WALK` / `PLAY` / `GROOMING` / `TRAINING` / `HANDLING`)을 종에 따라 검증 → `INVALID_SPECIES_ACTIVITY`
- 탈피: 크레스티드 게코 전용 → 그 외 종은 `SHED_NOT_SUPPORTED_SPECIES`
- 파생 로직(`anorexia-streak`, `prediction`)은 순수 계산 클래스로 분리한다 — `AnorexiaStreakCalculator`, `ShedPredictionCalculator` (README "다음 단계" 3번)

## 9. Gallery `/api/v1/pets/{petId}/photos`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/photos/upload-url` | R2 presigned 업로드 URL 발급 |
| POST | `/photos` | 업로드 완료 후 메타데이터 등록 |
| GET | `/photos` | 목록 (커서, `created_at` desc) |
| GET | `/photos/{id}` | 상세 |
| PATCH | `/photos/{id}` | 캡션 수정 |
| DELETE | `/photos/{id}` | 삭제 |

`photos.diary_entry_id`가 `NULL`이면 단독 갤러리, 값이 있으면 일기 첨부다.

## 10. Timeline `/api/v1/pets/{petId}/timeline`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/timeline` | 일기·급여·활동·체중·탈피·사진 통합 시간순 (커서) |

여러 테이블 union이 필요하다. `build.gradle.kts`에 주석 처리된 QueryDSL 의존성을 활성화하는 시점이 이 API다.

---

## refresh 토큰 저장소 (결정됨)

`refresh_tokens` 테이블을 **V2 마이그레이션으로 추가**한다. stateless refresh를 쓰지 않는 이유는 로그아웃·회원 탈퇴 시 즉시 무효화가 가능해야 하기 때문이다.

제안 스키마 (auth 구현 시 확정):

```sql
create table refresh_tokens (
    id         uuid         primary key default gen_random_uuid(),
    user_id    uuid         not null references users (id),
    token_hash varchar(255) not null,   -- 원문 저장 금지. SHA256Util 로 해시
    expires_at timestamp    not null,
    revoked_at timestamp,
    created_at timestamp    not null default now()
);
create unique index uq_refresh_token_hash on refresh_tokens (token_hash);
create index idx_refresh_user_id on refresh_tokens (user_id) where revoked_at is null;
```

- 엔티티는 `BaseCreatedEntity` 상속 (`created_at`만 필요 — 무효화는 `revoked_at`으로 표현하며 소프트 딜리트가 아니다)
- 토큰 원문은 저장하지 않는다. DB 유출 시 그대로 재사용 가능해지기 때문 (`SHA256Util` 사용)
- `POST /auth/logout` → 해당 토큰 `revoked_at` 설정
- `DELETE /users/me` (탈퇴) → 해당 사용자 토큰 전체 revoke

**남은 결정**: refresh 로테이션 여부. 매 `/auth/refresh` 호출 시 새 refresh를 발급하고 기존 것을 revoke할지(재사용 감지 가능, 권장), 만료까지 동일 토큰을 재사용할지는 auth 구현 시 정한다. 만료된 행을 정리하는 배치도 함께 검토한다.

## 설계 미결정 (구현 전 확정 필요)

1. **`/api/v1/auth/**`는 permitAll이다** (`SecurityConfig`의 `PUBLIC_PATHS`). 인증이 필요한 엔드포인트를 이 prefix 아래 두면 무인증으로 노출된다. 소셜 계정 연결을 `/users/me/social-accounts`에 배치한 이유다.
2. **PATCH vs PUT.** 이 문서는 전부 PATCH(부분 수정)로 잡았다. 확정되면 AGENTS.md §5 코드 컨벤션에 반영한다.
