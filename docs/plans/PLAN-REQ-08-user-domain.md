# PLAN-REQ-08 · user 도메인 (내 프로필 조회·수정·탈퇴)

> 출처: 2026-07-31 세션 · 작성: 2026-07-31 · 최종 갱신: 2026-08-07 · 최종 갱신: 2026-08-27 · 상태: ✅ **완료 (Phase 0~5)** — Phase 4·5 는 2026-08-27 계획·구현·검증. 미결 2건 잔존(필터 조회 비용 · 탈퇴 계정 refresh — 둘 다 관찰 후, 트리거 명시)

## 배경

`data/user`(엔티티·리포지토리)는 REQ-07 자동가입을 만들면서 이미 들어왔지만 **`business/user`가 없어 사용자가 자기 정보를 다룰 수단이 하나도 없다.** 조회·수정·탈퇴 세 엔드포인트가 전부 비어 있다.

착수 전 코드를 읽어 **탈퇴가 조용히 깨지는 경로 2건**을 확인했다. 둘 다 에러 없이 "정상 동작처럼" 보이는 종류다.

1. **탈퇴한 계정으로 다시 로그인하면 유령 계정에 들어간다.** `AuthService.findOrCreateUser`는 `(provider, provider_user_id)`로 소셜 행을 찾아 `linked.get().getUser()`를 **소프트 딜리트 여부를 보지 않고** 반환한다. 탈퇴가 `users.deleted_at`만 찍고 소셜 행을 남기면, 재로그인이 그 행을 찾아 탈퇴한 유저로 토큰을 발급한다. `UNIQUE (provider, provider_user_id)` 때문에 새 계정 생성도 막혀 있어 빠져나갈 길이 없다.
2. **탈퇴해도 access 토큰이 최대 30분 살아 있다.** `JwtAuthenticationFilter`는 DB를 전혀 보지 않고 서명·타입만 검증한다(`JWT_ACCESS_TTL` 기본 30분).

## 범위

**포함**

- `business/user/controller/UserController` · `business/user/service/UserService` 신설
- `data/user/dto/` — 응답 DTO 1종 + 수정 요청 DTO 1종
- `GET /users/me` · `PATCH /users/me` · `DELETE /users/me` (Notion `API I/F` 3행 = user 도메인 전부)
- 탈퇴 처리 2종 — `users.deleted_at` 기록 · `user_social_accounts` 하드 삭제
- `JwtAuthenticationFilter`에 활성 사용자 검사 추가 — `framework/security/UserStatusChecker` 포트를 두고 `UserService`가 구현
- AGENTS.md §3에 포트 패턴 기록 — **framework가 인터페이스를 정의하고 business가 구현하는 첫 사례**다

**제외**

- **소셜 계정 목록·연결·해제 엔드포인트** — Notion `API I/F`에 없다. 이전 판 `api-list.md`가 `/users/me/social-accounts` 3종을 임의로 추가했다가 근거가 없어 제거된 이력이 있다. 필요하면 Notion에 먼저 추가한 뒤 반영한다. **PROGRESS.md의 REQ-08 제목 "(프로필 · 소셜 계정 연결)"도 이 범위와 어긋나므로 함께 고친다**
- **프로필 이미지 제거 수단** — 원본(`PATCH /users/me`)은 "변경할 필드만 포함"만 규정하고 `null`의 의미를 정의하지 않는다. 근거 없이 만들면 그대로 계약이 되어 굳는다 → 미결
- **이미지 업로드** — R2 presigned 업로드는 REQ-11. 이번에는 `profile_image_url`을 문자열로 받기만 한다
- **탈퇴 시 pets·기록 데이터 처리** — pet 도메인이 REQ-09라 대상 테이블 자체가 아직 없다
- **탈퇴 유예기간·계정 복구** — 하드 삭제 결정(D1)이 재가입을 새 계정으로 못박으므로 복구는 성립하지 않는다. 요구가 생기면 별도 REQ
- **탈퇴 시 refresh 토큰 revoke** — 필터가 탈퇴 계정을 차단하므로 생략한다(D5). 대가는 `refresh_tokens`에 `revoked_at IS NULL` 행이 남는 것이다
- **ArchUnit 규칙·예외 변경** — 2026-07-31 정리에서 규칙 8건·예외 4건이 확정됐다(아래 「선행 정리」). **REQ-08은 예외를 하나도 늘리지 않는다** — 포트 방식을 고른 이유가 이것이다
	- ⚠️ **이 제외를 한 번 넘었다** (2026-08-04, 승인 후). `@AnalyzeClasses` 에 `DoNotIncludeTests` 를 추가해 **분석 범위를 프로덕션 코드로 한정**했다. Phase 1 의 `UserUpdateRequestTest` 가 `DTO_NAMING` 에 걸려 진행이 막혔고, 원인이 구현이 아니라 **규칙이 테스트 파일까지 보고 있던 것**이었다. **예외는 여전히 4건이고 규칙도 8건 그대로다** — 늘어난 것은 없다. 프로브로 프로덕션 위반 3종에 규칙 4개가 발화하는 것을 확인했다
- **`updated_at` 응답 노출** — 원본 `GET /users/me` 응답이 `id`·`nickname`·`email`·`profile_image_url`·`created_at` 5개다. 엔티티는 갖고 있지만 DTO에 넣지 않는다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|---|---|---|---|
| **D1** 탈퇴 후 같은 소셜 계정 재로그인 | 탈퇴 시 `user_social_accounts` 행을 **하드 삭제**. 재로그인하면 조회가 비어 완전히 새 `users` 행이 생긴다 | 소셜 행에는 `deleted_at`이 없다(`V1__init.sql` 실제 확인 — `BaseCreatedEntity`). 소프트 딜리트가 애초에 불가능하다. 탈퇴 계정은 `users`에 `deleted_at`으로 남아 감사 이력은 보존된다 | **재활성화**(soft-deleted 유저를 찾아 `deleted_at`을 되돌림) — 이전 반려동물·기록이 그대로 부활해 "탈퇴"의 의미가 사라진다. **재가입 거부**(409) — 유예기간·복구 정책이 통째로 딸려온다 |
| **D2** 탈퇴 계정의 잔존 access 토큰 | `JwtAuthenticationFilter`가 매 인증 요청마다 사용자 활성 여부를 조회한다. **`framework → business·data` 참조 금지(규칙 #4)는 그대로 두고**, `framework/security`에 `UserStatusChecker` 포트를 정의해 `UserService`가 구현한다 | 탈퇴 즉시 차단이 30분 창보다 중요하다. 규칙 #4를 계층 단위로 축소하는 안(entity·repository 금지)을 검토한 결과 **둘 다 막는 쪽이 옳고, 그러면 직참조가 성립하지 않는다** — 규칙을 지키는 방향으로 돌아왔다(2026-07-31). 구현체를 따로 만들지 않고 `UserService`가 구현하므로 **새 클래스는 인터페이스 1개뿐**이다 | **필터가 `UserRepository` 직참조**(2026-07-31 일시 채택 후 철회) — 규칙 #4를 열어야 하고, 예외가 한 번 열리면 다음엔 더 쉽게 열린다. **`entity`만 금지하고 `repository`는 허용** — `boolean` 반환 메서드를 쓰면 성립하지만 framework가 영속 계층에 닿는 것 자체를 남긴다. **감수**(refresh만 revoke) — 탈퇴가 최대 30분간 무력 |
| **D3** `PATCH /users/me`의 `null` 의미 | **누락·`null` 모두 "변경 없음"**. 보낸 필드만 덮어쓴다 | Notion 원본이 "변경할 필드만 포함"만 규정하고 `null` 규약이 없다(2026-07-31 `API I/F` 직접 확인). 원본에 없는 의미를 만들지 않는다 | **`null` = 제거**(JsonNullable 또는 `Optional` 래핑) — 의존성·패턴이 늘고 무엇보다 원본에 근거가 없다. AGENTS §5가 PATCH를 고른 이유(누락과 `null` 구분)와는 어긋나므로 **의도적 예외임을 명시**한다 |
| **D4** `business/auth → data/user` 예외 | **설계 결정으로 승격**(2026-07-31). "임시" 딱지를 떼고 나머지 3건과 같은 지위로 올렸다 | 소셜 자동가입은 본질적으로 user 프로비저닝이다. 이 참조를 없애려면 `business/user`에 진입점을 두어야 하는데 그러면 `business/auth → business/user` 예외가 대신 생겨 **개수는 그대로인 채 간접층만 는다** | **프로비저닝 진입점 신설** — 위 이유로 기각. **패키지 축소**(`business.auth.service`로 한정) — 지금도 `AuthService` 하나만 쓰므로 실효 차이가 없다 |
| **D5** 탈퇴 시 refresh revoke | **하지 않는다.** 필터 차단(D2)을 신뢰한다. **왜 revoke하지 않는지 코드 주석 필수** | revoke하면 `business/user → data/auth` 참조가 생겨 예외가 4→5로 는다. D2로 탈퇴 계정은 어떤 access 토큰을 들고 와도 막히므로, refresh로 새 토큰을 받아도 결국 차단된다 | **예외 1건 추가** · **`AuthService.logout(userId)` 재사용** — 둘 다 예외가 늘어난다. 셋 중 무엇을 골라도 차단 자체는 되므로 예외를 안 늘리는 쪽을 택했다 |
| **D6** PATCH 부분 반영을 어디서 병합할까 | **`UserService`에서 병합한 뒤 엔티티에 넘긴다.** 기존 `User.updateProfile(nickname, profileImageUrl)`의 "두 필드를 통째로 덮어쓴다"는 계약은 그대로 두고, 서비스가 `req.nickname() != null ? req.nickname() : user.getNickname()` 꼴로 채워서 호출한다 | 엔티티가 `null`을 "변경 없음"으로 해석하기 시작하면 **`null`에 도메인 의미가 붙어** D3(원본에 없는 규약을 만들지 않는다)와 정면으로 어긋난다. 부분 반영은 HTTP PATCH의 관심사이지 엔티티의 관심사가 아니다. 병합을 서비스에 두면 엔티티는 "받은 값으로 바꾼다"는 한 가지 뜻만 갖는다 | **엔티티가 `null`이면 유지** — 위 이유로 기각. 호출부마다 `null` 의미가 달라질 여지도 생긴다. **`updateNickname`/`updateProfileImage` 분리** — 필드가 늘 때마다 메서드가 늘고, 한 요청이 두 번의 상태 변경이 된다 |
| **D7** 닉네임 검증 최소선 | **`@Size(max = 100)` 하나만 박는다.** 최소 길이·트림·빈 문자열·중복 허용 여부는 미결로 남긴다 | `varchar(100)`은 원본 근거가 필요 없는 **스키마에서 확정된 사실**이다(`V1__init.sql`). 검증이 없으면 101자 요청이 `DataIntegrityViolationException`으로 올라와 **400이 아니라 500**이 된다 — 클라이언트 입력 오류가 서버 오류로 보고되는 건 명백한 결함이다. `@Size`는 **`null`을 통과시키므로** D3(누락 = 변경 없음)과 충돌하지 않는다 | ⚠️ **`@NotBlank` 병기**(2026-08-03 초안, 즉시 철회) — `@NotBlank`는 `null`을 거부하는데 **PATCH에 닉네임을 안 보내는 것이 D3의 정상 경로**다. 그대로 두면 부분 수정이 통째로 400이 된다. `NOT NULL` 제약은 엔티티 불변식이지 PATCH 요청 DTO의 불변식이 아니다 — 두 층의 제약을 같은 것으로 착각한 실수다. **전부 미결로 미룸**(2026-07-31 초안) — 미결의 근거는 "원본에 없다"인데 길이 제한은 스키마에 있다 |
| **D8** 프로필 이미지 제거 (2026-08-27) | **전용 엔드포인트 `DELETE /users/me/profile-image`** → `profile_image_url = NULL`, 204, 멱등. Notion `API I/F` 에 행을 먼저 추가했다 | D3 이 누락·`null` 을 모두 "변경 없음"으로 두므로 `PATCH` 에는 제거 신호를 실을 자리가 없다. 분리하면 D3 과 기존 검증 계약(REQ-08-06·07)이 그대로 유지되고 의존성이 늘지 않으며 의도가 URL 에 드러난다 | **`null` = 제거(`JsonNullable`)** — D3 을 뒤집고 `jackson-databind-nullable` 이 늘며 닉네임에도 같은 의미론이 번져 `null` 닉네임 거부 로직이 따로 필요해진다. **`""` = 제거** — 원본 없는 규약(D3 위반)이고 빈 닉네임 미결과 얽히며 클라이언트 실수로 삭제된다 |
| **D9** 닉네임 검증 규칙 (2026-08-27) | **트림 후 1~100자, 중복 허용.** `@Size(min = 1, max = 100)` + 서비스에서 `strip()` 후 저장. 공백만·`""` 은 400. `null` 은 D3 대로 "변경 없음" | 원본에 규칙이 없어 **Notion 행에 Validation 을 먼저 명시**한 뒤 옮겨 적었다(REQ-09 와 같은 이유 — `/testrun` 인용 검사는 파일만 본다). UNIQUE 가 스키마에 없으므로 중복 금지를 만들면 인덱스·409 코드가 따라와 범위가 커진다 | **`@NotBlank`** — `null` 을 거부해 PATCH 정상 경로가 400 이 된다(AGENTS §5 금지). **중복 금지** — 스키마 변경(UNIQUE) + 마이그레이션 + `NICKNAME_DUPLICATED` 필요, 원본 근거 없음 |

## 미결 질문

- [x] **프로필 이미지 제거 — 전용 엔드포인트 `DELETE /users/me/profile-image` (2026-08-27 확정, D8).** 원본에 제거 의미론이 없어 **Notion `API I/F` 에 행을 먼저 추가했다**(`create-pages` 로 가능했다 — DB 행 생성은 「설계」 탭 본문 수정 불가 함정과 별개). `null` = 제거(`JsonNullable`)는 D3 을 뒤집고 의존성이 늘어 기각, `""` = 제거는 원본 없는 규약이라 기각. → **Phase 4**
- [x] **닉네임 검증 규칙 — 트림 후 1~100자 · 중복 허용 (2026-08-27 확정, D9).** 원본에 규칙이 없어 **Notion `API I/F` `PATCH /users/me` 행에 Validation 을 먼저 명시했다.** 중복은 스키마에 UNIQUE 가 없으므로 허용. 카카오 자동가입 닉네임(외부 값)은 적용 대상이 아니다 → **Phase 5**
- [x] **빈 문자열 닉네임 — 거부(400) (2026-08-27 확정, D9 로 함께 닫힘).** 트림 후 1자 미만은 전부 거부하므로 `""`·공백만 도 400. `null` 은 여전히 "변경 없음"(D3) — `@Size(min = 1)` 은 `null` 을 통과시키므로 충돌하지 않는다 → **Phase 5**
- [x] **Notion 테이블 정의서 §10 역반영 완료 (2026-08-10).** Redis 기각 근거 문장에 "**이것은 Redis 기각 논거이지 무효화 시점의 명세가 아니다**" 와 "**회원 탈퇴는 revoke 하지 않는다**(REQ-08 D5)" 를 붙였다. `revoked_at` 이 찍히는 경로가 로테이션·로그아웃·재사용 감지 셋뿐이라는 것도 함께 명시해 §10 안에서 서술이 어긋나지 않게 했다.
	<details><summary>등록 당시 기록 (2026-08-07)</summary>

	**Notion 테이블 정의서 §10의 저장소 선택 근거 문장을 고칠 것** (역반영 대기). "Redis는 기각했다(로그아웃·**탈퇴** 시 즉시 무효화가 필요하고…)"가 남아 있어 D5와 어긋나 보인다. Redis 기각 논거이지 탈퇴 동작 명세가 아니므로 **문장을 다듬거나 D5 링크를 붙이는 정도**면 된다 — 사람이 Notion에서 수정해야 한다

	</details>
- [x] **`@WebMvcTest` 를 도입할 것인가** — **REQ-15 로 분리해 도입했다 (2026-08-07 해소).** 관례는 AGENTS §6, 케이스는 [PLAN-REQ-15](PLAN-REQ-15-controller-test-convention.md) `REQ-15-01~08` 이다. **공백 3건이 실제로 닫혔다** — snake_case 직렬화·101자→400 매핑·탈퇴 토큰→401 이 이제 테스트로 고정된다. 권고대로 REQ-08 에 끼워 넣지 않고 별도 REQ 로 잡았다(대상이 user 도메인이 아니라 컨트롤러 테스트 관례 전체이기 때문).
	<details><summary>등록 당시 기록 (2026-08-07)</summary>

	**`@WebMvcTest` 를 도입할 것인가 (REQ-08 범위 밖 · 2026-08-07 등록).** REQ-08 에는 **HTTP 왕복이 있어야만 검증되는 지점이 3건**인데 셋 다 지금은 **사람이 한 번 본 것**뿐이라 회귀를 막지 못한다.
	| 지점 | 현재 테스트가 덮는 곳 | 안 덮는 곳 |
	|---|---|---|
	| `GET /users/me` 전역 snake_case | `REQ-08-01` — record 컴포넌트 이름(camelCase) | 직렬화된 JSON 키. `JacksonConfig` 가 사라져도 초록불 |
	| 101자 → 400 | `REQ-08-06` — Bean Validation 위반 생성 | `MethodArgumentNotValidException` → `INVALID_INPUT` 매핑 |
	| 탈퇴 토큰 → 401 | `REQ-08-16` — 인증 미설정 + 체인 계속 | `authenticationEntryPoint` 가 내는 401 과 본문 형태 |
	- 셋 다 **깨져도 조용하다** — 200 이 나가거나 500 이 나가거나 404 가 나가는데 단위 테스트는 초록불이다
	- 실측은 2026-08-07 로컬 왕복에서 마쳤다(각각 5필드 snake_case · 400 `INVALID_INPUT` · 401 `UNAUTHORIZED`). **한 번 봤다는 것과 고정됐다는 것은 다르다**
	- 이 레포에 없던 패턴이라 도입은 AGENTS §7상 제안·승인 대상이다. **REQ-08 에 끼워 넣지 않고 별도 REQ 로 잡는 쪽**을 권한다 — pet·diary 등 다음 도메인이 전부 같은 공백을 갖게 되므로, 컨트롤러 테스트 관례를 한 번 정하는 작업으로 다루는 편이 낫다

	</details>
- [ ] **필터의 사용자 조회 비용.** 모든 인증 요청에 DB 왕복 1회가 붙는다. 캐시 도입 여부는 실사용 트래픽을 보고 정한다 — 지금 정하면 근거 없는 수치가 굳는다. **재검토 트리거(2026-08-27): 첫 배포 후 실측 1회 — 인증 요청 p95 지연과 `users` 조회 QPS 를 보고 판단.** 코드 작업 아님
- [ ] **탈퇴 계정의 `/auth/refresh` 응답.** D5로 revoke를 안 하므로 탈퇴한 사용자도 `/auth/refresh`가 **200과 새 토큰을 반환**한다(그 토큰으로 API 를 부르면 401). **재검토 트리거(2026-08-27): 클라이언트가 탈퇴 직후 refresh 를 호출하는 흐름이 실제로 있는지 앱 구현에서 확인한 뒤 — 있으면 `AuthService.refresh` 에 활성 검사(401) 추가를 REQ-07 후속 Phase 로.** 지금은 코드 작업 아님
	- ⚠️ **정정 (2026-08-07 실측)** — 원래 "그 토큰으로 API를 부르면 401"이라 적혀 있었으나 **지금은 404 `USER_NOT_FOUND`** 다. Phase 3 필터가 없어 서비스의 `findByIdAndDeletedAtIsNull` 이 먼저 걸리기 때문이다. 즉 그 문구는 **Phase 3 완료를 전제한 서술**이었고, Phase 3 이후 필터가 앞에서 잡아 401이 된다. **Phase 3 검증 시 이 전환을 함께 확인할 것**

> **해소됨** — `business/user → data/auth` 참조를 어떻게 처리할지는 D5(revoke 생략)로 닫혔다.

## 작업 단계

- [x] **Phase 0 — ArchUnit 정리 (2026-07-31 결정 · 2026-08-03 코드 반영)**
      ⚠️ **08-03 에 재실행했다** — `main` 기준으로 규칙이 완화된 상태 그대로였기 때문이다. 당시엔 "07-31 에 커밋되지 않고 유실됐다"고 판단했으나 **그 진단은 틀렸다**(08-07 정정). 07-31 작업은 `chore/archunit-tighten-empty-allowance` 브랜치의 `f6f66c7` 로 커밋돼 있었고 푸시·머지만 안 된 상태였다. `git log -- <경로>` 가 HEAD 도달 가능 커밋만 보여 놓친 것이다 — **`--all` 을 붙였어야 했다.** 따라서 `d9016e3` 은 `f6f66c7` 의 중복 재구현이다(다만 프로브 3건은 08-03 판에만 있다).
      REQ-08 착수 전에 구조 규칙의 완화 지점을 전부 확정했다.
      ① **`allowEmptyShould`를 8개 규칙 전부 `false`로** — REQ-07로 controller·service·repository·entity·dto가 모두 들어와 더 이상 빈 집합이 아니다. 껐는데도 통과하는 것을 확인했다(`tests=7`+`tests=1`, failures 0). `LAYER_DIRECTION`의 `withOptionalLayers`도 같이 껐다.
      ② **예외 #4 `business/auth → data/user` 승격** — "임시" 주석을 근거 주석으로 교체. REQ-07 미결 1건이 닫혔다.
      ③ **예외 #1~#3은 유지** — `data.common`(도메인 공용) · `framework`(슬라이스 오발 방지) · `business.timeline`(§3 명시). #3은 대상 코드가 아직 0개라 REQ-12까지 공허하다.
      ④ **프로브 3건으로 규칙이 공허하지 않음을 확인** (2026-08-03, CLAUDE.md 계약). 심은 위반이 전부 빨간불이 됐다 —
       · `JwtAuthenticationFilter` 에 `UserRepository` 직참조 → `FRAMEWORK_MUST_NOT_KNOW_DOMAIN` 발화
       · **같은 위반에 `LAYER_DIRECTION` 도 함께 발화** — 예상 밖 수확이다(아래)
       · 대상 0개짜리 임시 규칙 → `PROBE_EMPTY` 발화, `allowEmptyShould(false)` 가 실제로 동작함
      완료 기준: 규칙 8건 통과 ✅ (`tests=7` + `tests=1`, failures 0) · 프로브 3건 발화 확인 ✅

      > **Phase 3 의 근거가 하나 늘었다.** 필터가 `data..repository..` 를 직참조하면 규칙 #4 뿐 아니라 `LAYER_DIRECTION` 에도 걸린다 — 필터는 정의된 세 레이어 어디에도 속하지 않는데 `Repository` 레이어는 `mayOnlyBeAccessedByLayers("Service")` 이기 때문이다. 즉 **규칙 #4 를 열어도 직참조는 여전히 통과하지 못한다.** D2 가 검토했던 "규칙 #4 를 연다" 안은 애초에 성립하지 않았던 셈이고, 포트 방식은 두 규칙을 동시에 만족시키는 유일한 길이다.

- [x] **Phase 1 — 조회·수정 (2026-08-04 구현 · 2026-08-07 검증)**
      `UserController`(`@RequestMapping("/api/v1/users")`) + `UserService` + DTO 2종. `GET`은 `findByIdAndDeletedAtIsNull`로 조회하고 없으면 `USER_NOT_FOUND`. `PATCH`는 보낸 필드만 반영(D3), 병합은 **서비스에서**(D6). 요청 DTO에 `@Size(max = 100)`(D7 — `@NotBlank`는 쓰지 않는다. `null` = 변경 없음).
      ⚠️ **`User.updateProfile`이 이미 있고, 두 필드를 무조건 덮어쓴다.** 닉네임만 담긴 PATCH에 `updateProfile(nick, null)`로 호출하면 **`profile_image_url`이 지워진다** — 에러 없이 DB에 반영되는 데이터 손실이라 응답만 봐서는 모른다. 아래 완료 기준이 정확히 이걸 막는다. 현재 이 메서드의 호출처는 0곳이다(REQ-07에서 선반영만 됨).
      완료 기준: 응답 필드가 Notion `GET /users/me` 5개와 정확히 일치(`updated_at` 없음, 전역 snake_case) · **`PATCH`에 닉네임만 보내면 `profile_image_url`이 유지됨** · 101자 닉네임이 500이 아니라 400 · ArchUnit 8건 통과
      → **케이스 8건 전부 `✅`** (2026-08-07 `/testrun`). 다만 **완료 기준 2건이 케이스보다 넓다** — 아래를 알고 체크한 것이다.
      · **"전역 snake_case"는 검증되지 않는다.** `REQ-08-01`은 record 컴포넌트 이름(camelCase)만 본다. 직렬화된 JSON이 `profile_image_url`로 나가는지는 아무도 확인하지 않으며, `JacksonConfig`가 사라져도 초록불을 유지한다
      · **"101자 → 400"의 마지막 한 칸이 비어 있다.** `REQ-08-06`은 Bean Validation이 위반을 만드는 것까지다. 400으로 이어지는 것은 `GlobalExceptionHandler`의 `MethodArgumentNotValidException` → `INVALID_INPUT` 매핑을 **코드로 읽어 확인**했을 뿐 테스트가 보증하지 않는다
      → ✅ **둘 다 2026-08-07 로컬 왕복에서 실측으로 확인됐다** — `GET /users/me` 가 `profile_image_url`·`created_at` 5필드로 나갔고(`updated_at` 없음), 101자 닉네임은 **400 `INVALID_INPUT`** 이었다(500 아님). 다만 **테스트가 아니라 사람이 한 번 본 것**이라 회귀는 여전히 막지 못한다 — `@WebMvcTest` 도입 시 자동화 대상이다(AGENTS §7상 새 패턴이라 별도 판단 필요).

- [x] **Phase 2 — 탈퇴 (2026-08-07 구현·검증)**
      `users.deleted_at` 기록(`BaseSoftDeleteEntity.softDelete()`) + `user_social_accounts` 하드 삭제. 204 반환(본문 없음 — `AuthController.logout` 선례대로 `ApiResponse`를 씌우지 않는다). refresh는 revoke하지 않으며(D5) **그 이유를 주석으로 남긴다** — 근거가 없으면 다음 사람이 "빠뜨렸다"고 보고 채워 넣어 예외를 늘린다.
      `UserSocialAccountRepository`에 **삭제 메서드가 없다**(현재 `findByProviderAndProviderUserId` 하나뿐) — `deleteByUserId(UUID)` 추가가 필요하다.
      완료 기준: 탈퇴 → 같은 카카오 계정으로 재로그인 시 **새 `users.id`가 발급됨**(로컬 DB 왕복으로 실제 확인 — 목으로는 D1이 검증되지 않는다) · ArchUnit 8건 통과(예외가 늘지 않았음의 확인)

- [x] **Phase 3 — 필터 활성 검사 (포트) (2026-08-07 구현·검증)**
      `framework/security/UserStatusChecker` 인터페이스 정의 → `UserService`가 구현 → `JwtAuthenticationFilter`가 인터페이스를 주입받아 검사. AGENTS.md §3에 이 패턴을 기록한다(framework가 인터페이스를 정의하고 business가 구현하는 첫 사례).
      **비활성 사용자일 때는 예외를 던지지 않고 `SecurityContext`를 세팅하지 않은 채 통과시킨다.** 그러면 `SecurityConfig`의 `authenticationEntryPoint`가 기존 규격대로 `ApiResponse.error(UNAUTHORIZED)` + 401을 내려준다 — 필터에서 던지면 `GlobalExceptionHandler`에 닿지 않아(필터는 DispatcherServlet 앞이다) 응답 형태가 갈린다.
      완료 기준: 탈퇴 직후 기존 access 토큰으로 `GET /users/me` 호출 시 **401 + 기존 에러 본문 형태** · **규칙이 살아 있는지 프로브로 확인**(필터에 `UserRepository` 직참조를 일부러 심어 빨간불이 되는지 — CLAUDE.md 계약. 2026-08-03 실측상 `FRAMEWORK_MUST_NOT_KNOW_DOMAIN`과 `LAYER_DIRECTION` **둘 다** 발화한다) · `spotlessApply` + `build -x test` + `checkstyleMain -PciStrict` 통과

- [x] **Phase 4 — 프로필 이미지 제거 (2026-08-27 계획·구현·검증, D8)**
      `DELETE /users/me/profile-image` → `UserService.removeProfileImage(userId)` → `user.updateProfile(user.getNickname(), null)` (기존 "받은 값을 그대로 쓴다" 계약 유지 — 서비스가 닉네임을 채워 넘긴다). 컨트롤러 테스트는 AGENTS §6 관례.
      완료 기준: 204 · 본문 없음 · 저장값 `profile_image_url = NULL` · 이미지가 이미 없는 상태에서도 204(멱등) · `PATCH /users/me` 의 D3 의미론(누락·`null` = 변경 없음)이 그대로다 · 미인증 401

- [x] **Phase 5 — 닉네임 검증 규칙 (2026-08-27 계획·구현·검증, D9)**
      `UserUpdateRequest.nickname` 에 `@Size(min = 1, max = 100)` · 서비스가 `strip()` 한 값으로 병합 · 공백만인 값은 트림 후 빈 문자열이 되므로 서비스에서 거부(`INVALID_INPUT` 계열 400 — 기존 `ErrorCode` 확인 후 결정). 카카오 자동가입 경로는 건드리지 않는다.
      완료 기준: `""` → 400 · 공백만(`"   "`) → 400 · `" 마당이 "` → `"마당이"` 로 저장 · 101자 → 400(기존) · `null` → 변경 없음(기존 REQ-08-06 유지) · 같은 닉네임 두 사용자 허용

## 검증 계약

> 작성: 2026-08-03 · 근거: 이 계획서 (스펙 원본은 Notion `API I/F`) · 검증: `/testrun REQ-08`
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-08-01]` 형태로 박는다.
> **결과 갱신: 2026-08-27 — 21~29 전부 `✅` (Phase 4·5 완료).** `/testrun REQ-08` 42건 실행 · 실패 0 · 표 29건 ↔ 코드 28건(+ `REQ-08-11` 수동) · 근거 인용 9건 원문 존재. **완료 기준 중 "미인증 401"(Phase 4)은 케이스가 없어 테스트로는 고정되지 않았다** — `PUBLIC_PATHS` 에 없어 기본 보호될 뿐이다.
> **결과 갱신: 2026-08-07 — 20건 전부 `✅` (Phase 0~3 완료).** `REQ-08-11` 은 자동화하지 않고 **로컬 DB 왕복으로 사람이 확인**했다(2026-08-07: 탈퇴 전 `a02016c0…` → 재로그인 후 **`94eef1f2…`**, 소셜 행은 새 `user_id` 로 재생성). `/testrun` 에는 잡히지 않으므로 **"14건 통과"를 "15건 검증됨"으로 읽지 말 것.** Phase 3(`16~20`)도 같은 날 구현·검증했다.
> (이전) Phase 1(`01~08`) 8건 `✅` (`/testrun REQ-08`, 18건 실행 · 실패 0 · 근거 인용 8건 전부 유효 · 고아 ID 없음). `09~20`은 해당 Phase 미착수라 `—`.
> **테스트 코드는 Phase별로 들어온다** — Java는 대상 클래스가 없으면 테스트 소스가 컴파일되지 않아 `./gradlew test`가 통째로 죽는다(ArchUnit 8건까지 같이 못 돈다). 그래서 "실패하는 테스트를 미리 남긴다"를 여기서는 쓰지 않고, **표가 미검증 상태를 대신 드러낸다.** 오늘 작성한 것은 대상이 이미 있는 `REQ-08-08` 하나뿐이다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-08-01 | 응답 DTO | 필드가 정확히 5개 (`updated_at` 없음) | 불변식 | 범위—제외 — "원본 `GET /users/me` 응답이 `id`·`nickname`·`email`·`profile_image_url`·`created_at` 5개다" | 1 | ✅ |
| REQ-08-02 | `UserService` 조회 | 소프트 딜리트된 사용자 → `USER_NOT_FOUND` | 예외 | Phase 1 — "`findByIdAndDeletedAtIsNull`로 조회하고 없으면 `USER_NOT_FOUND`" | 1 | ✅ |
| REQ-08-03 | `UserService` 수정 | 닉네임만 보내면 `profile_image_url` 유지 | 회귀 | Phase 1 완료 기준 — "`PATCH`에 닉네임만 보내면 `profile_image_url`이 유지됨" | 1 | ✅ |
| REQ-08-04 | 〃 | 이미지만 보내면 `nickname` 유지 | 회귀 | D3 — "보낸 필드만 덮어쓴다" | 1 | ✅ |
| REQ-08-05 | 〃 | 둘 다 `null` → 아무것도 안 바뀐다 | 경계 | D3 — "누락·`null` 모두 "변경 없음"" | 1 | ✅ |
| REQ-08-06 | 수정 요청 DTO | 101자 닉네임 → 검증 위반 | 경계 | D7 — "101자 요청이 `DataIntegrityViolationException`으로 올라와 **400이 아니라 500**이 된다" | 1 | ✅ |
| REQ-08-07 | 〃 | 100자 통과 · `null` 통과 | 경계 | D7 — "`@Size`는 **`null`을 통과시키므로**" | 1 | ✅ |
| REQ-08-08 | `User.updateProfile` | 두 필드를 통째로 덮어쓴다(엔티티가 `null`을 "유지"로 읽지 않는다) | 불변식 | D6 — "엔티티가 `null`을 "변경 없음"으로 해석하기 시작하면" | 1 | ✅ |
| REQ-08-09 | `UserService` 탈퇴 | `users.deleted_at`이 채워진다 | 정상 | Phase 2 — "`users.deleted_at` 기록" | 2 | ✅ |
| REQ-08-10 | 〃 | 소셜 행이 하드 삭제된다 | 정상 | D1 — "탈퇴 시 `user_social_accounts` 행을 **하드 삭제**" | 2 | ✅ |
| REQ-08-11 | 탈퇴 → 재로그인 | 새 `users.id`가 발급된다 | 회귀 | Phase 2 완료 기준 — "새 `users.id`가 발급됨" | 2 | ✅ 수동 |
| REQ-08-12 | `business.user` | `data.auth`를 참조하지 않는다 | 불변식 | D5 — "revoke하면 `business/user → data/auth` 참조가 생겨 예외가 4→5로 는다" | 2 | ✅ |
| REQ-08-13 | `UserService` 탈퇴 | `@Transactional`이 붙어 있다 | 불변식 | 제약·함정 — "탈퇴는 소셜 행 삭제·revoke·`deleted_at`을 한 트랜잭션에서 쓴다" | 2 | ✅ |
| REQ-08-14 | 〃 | 소셜 행을 먼저 지운다 | 불변식 | 제약·함정 — "소셜 행을 먼저 지우므로 제약 위반은 없지만, 순서를 뒤집을 이유도 없다" | 2 | ✅ |
| REQ-08-15 | `UserController` | `DELETE`는 204 · 본문 없음 | 정상 | Phase 2 — "204 반환(본문 없음" | 2 | ✅ |
| REQ-08-16 | `JwtAuthenticationFilter` | 탈퇴 사용자 토큰 → 인증이 설정되지 않는다 | 예외 | Phase 3 — "`SecurityContext`를 세팅하지 않은 채 통과시킨다" | 3 | ✅ |
| REQ-08-17 | 〃 | 활성 사용자 토큰 → 인증이 설정된다 | 정상 | Phase 3 완료 기준 (REQ-08-16의 대조군) | 3 | ✅ |
| REQ-08-18 | 〃 | 비활성이어도 예외를 던지지 않는다 | 회귀 | Phase 3 — "필터에서 던지면 `GlobalExceptionHandler`에 닿지 않아 응답 형태가 갈린다" | 3 | ✅ |
| REQ-08-19 | 〃 | 토큰이 없으면 `UserStatusChecker`를 호출하지 않는다 | 경계 | 제약·함정 — "공개 경로에는 DB 조회가 붙지 않는다" | 3 | ✅ |
| REQ-08-20 | `UserStatusChecker` | `framework.security` 패키지에 있다 | 불변식 | 제약·함정 — "포트 인터페이스는 `framework`에 둔다" | 3 | ✅ |
| REQ-08-21 | `DELETE /users/me/profile-image` | **HTTP 왕복** 204 · 본문 없음 | 정상 | Phase 4 완료 기준 — "204 · 본문 없음" | 4 | ✅ |
| REQ-08-22 | `UserService.removeProfileImage` | 호출 후 `profileImageUrl` 이 `null` | 정상 | Phase 4 완료 기준 — "저장값 `profile_image_url = NULL`" | 4 | ✅ |
| REQ-08-23 | 〃 | 닉네임은 유지된다 | 회귀 | 제약·함정 — "`User.updateProfile`은 부분 반영용이 아니다" | 4 | ✅ |
| REQ-08-24 | 〃 | 이미지가 없는 상태에서 호출해도 예외 없이 끝난다 | 경계 | Phase 4 완료 기준 — "이미지가 이미 없는 상태에서도 204(멱등)" | 4 | ✅ |
| REQ-08-25 | `PATCH /users/me` | `profile_image_url: null` 을 보내도 기존 값이 유지된다 | 불변식 | Phase 4 완료 기준 — "D3 의미론(누락·`null` = 변경 없음)이 그대로다" | 4 | ✅ |
| REQ-08-26 | `PATCH /users/me` | `nickname: ""` → 400 | 경계 | Phase 5 완료 기준 — "`""` → 400" | 5 | ✅ |
| REQ-08-27 | 〃 | 공백만인 닉네임 → 400 | 경계 | Phase 5 완료 기준 — "공백만(`"   "`) → 400" | 5 | ✅ |
| REQ-08-28 | `UserService.update` | 앞뒤 공백이 트림되어 저장된다 | 정상 | Phase 5 완료 기준 — "`" 마당이 "` → `"마당이"` 로 저장" | 5 | ✅ |
| REQ-08-29 | 〃 | 같은 닉네임을 두 사용자가 가질 수 있다 | 불변식 | D9 — "트림 후 1~100자, 중복 허용" | 5 | ✅ |

**REQ-08-11은 자동화하지 않는다.** 계획서가 "목으로는 D1이 검증되지 않는다"고 못 박았다 — 유령 계정 경로는 `UNIQUE (provider, provider_user_id)`와 실제 조회 결과가 만드는 현상이다. **로컬 DB 왕복으로 사람이 확인**하고 결과 열에 근거(날짜·관찰한 `users.id`)를 남긴다. `/testrun`의 REQ 필터에는 잡히지 않으므로 **"19건 통과"를 "20건 검증됨"으로 읽지 말 것.**

## 제약·함정

- **`@Transactional` 롤백** (AGENTS §5) — 탈퇴는 소셜 행 삭제·revoke·`deleted_at`을 한 트랜잭션에서 쓴다. 여기서 예외를 던지면 **셋 다 사라진다.** 남겨야 하는 쓰기가 생기면 `noRollbackFor`를 명시할 것. 2026-07-30 `AuthService.refresh`에서 같은 함정이 실제로 터졌고 **목 기반 테스트는 통과했다**
- **`User.updateProfile`은 부분 반영용이 아니다** (D6) — 두 필드를 무조건 덮어쓴다. 서비스가 병합하지 않고 그대로 부르면 PATCH가 다른 필드를 지운다. **응답은 200으로 정상이고 DB만 조용히 손상된다** — 07-31에 잡은 함정 2건(유령 계정·잔존 토큰)과 같은 종류다
- ✅ **`api-list.md`와 D5의 충돌은 해소됐다** (2026-08-04 Notion 대조) — `API I/F` → 회원 탈퇴 원본은 "소프트 딜리트 / 204"만 규정하고 revoke를 말하지 않는다. 테이블 정의서 §10의 `revoked_at`도 "로테이션·로그아웃·재사용 감지로 찍힌다"로 **탈퇴를 빼고** 있다(2026-07-29 역반영, 최신). api-list의 revoke 서술은 **어느 원본에서도 나오지 않은 파생 문서의 자체 생성**이었고 D5에 맞춰 고쳤다. Notion 쪽 잔여 문장 1건은 미결로 이관
- **D1은 목으로 검증되지 않는다.** 유령 계정 경로는 `UNIQUE (provider, provider_user_id)`와 실제 조회 결과가 만드는 현상이라 **로컬 DB 왕복이 유일한 확인 수단**이다
- **ArchUnit 규칙을 고치면 프로브를 심는다** (CLAUDE.md) — 규칙이 조용히 공허해진 전례가 있다. 통과/실패만 봐서는 알 수 없다
- **`allowEmptyShould`를 다시 `true`로 되돌리지 말 것** (Phase 0) — 새 규칙을 추가했는데 대상이 0개라 실패하면, 완화가 아니라 **규칙이 시기상조라는 신호**다
- **포트 인터페이스는 `framework`에 둔다.** `business/user`에 두면 필터가 그걸 참조하게 되어 규칙 #4 위반이다 — 인터페이스를 어디 두느냐가 이 설계의 전부다
- **삭제 순서** — FK는 `user_social_accounts.user_id → users.id` 한 방향이다. 소셜 행을 먼저 지우므로 제약 위반은 없지만, 순서를 뒤집을 이유도 없다
- **Entity는 Service 밖으로 나가지 않는다** (AGENTS §5) — ArchUnit 규칙 #2가 잡는다
- **`PUBLIC_PATHS`는 건드리지 않는다.** `/users/**`는 전부 인증 대상이고, 필터는 토큰이 있을 때만 동작하므로 공개 경로에는 DB 조회가 붙지 않는다
