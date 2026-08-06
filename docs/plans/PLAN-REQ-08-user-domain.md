# PLAN-REQ-08 · user 도메인 (내 프로필 조회·수정·탈퇴)

> 출처: 2026-07-31 세션 · 작성: 2026-07-31 · 상태: 🟡 진행 (Phase 0 완료)

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
| **D7** 닉네임 검증 최소선 | **`@NotBlank` + `@Size(max = 100)`를 지금 박는다.** 최소 길이·트림·중복 허용 여부는 미결로 남긴다 | `varchar(100) NOT NULL`은 원본 근거가 필요 없는 **스키마에서 확정된 사실**이다(`V1__init.sql`). 검증이 없으면 101자 요청이 `DataIntegrityViolationException`으로 올라와 **400이 아니라 500**이 된다 — 클라이언트 입력 오류가 서버 오류로 보고되는 건 명백한 결함이다 | **전부 미결로 미룸**(2026-07-31 초안) — 미결의 근거는 "원본에 없다"인데 이 둘은 스키마에 있다. 근거 없는 규약을 만드는 것과 이미 확정된 제약을 표현하는 것은 다르다 |

## 미결 질문

- [ ] **프로필 이미지 제거를 어떻게 표현할 것인가.** D3으로 지금은 제거 수단이 없다. 요구가 확인되면 **Notion `API I/F`의 `PATCH /users/me` 행을 먼저 고친 뒤** 구현한다
- [ ] **닉네임 검증 규칙 중 스키마에 없는 것들.** 최소 길이·공백 트림·중복 허용 여부에 원본 근거가 없다. (스키마에 UNIQUE가 없으므로 **중복은 허용이 기본값**) — `@NotBlank`·`@Size(max = 100)`는 스키마에서 나오므로 **D7로 미결에서 빠졌다**
- [ ] **`DELETE /users/me`의 refresh revoke 여부가 문서 두 곳에서 어긋난다.** `docs/specs/api-list.md`(§refresh 토큰 저장소)는 "탈퇴 → 해당 사용자 토큰 전체 revoke"라고 적고 있는데 **D5는 정반대**다. api-list는 Notion 파생 요약이므로(AGENTS §0) **Phase 2 착수 전에 Notion `API I/F` 원본을 확인**해야 한다. 원본에도 revoke가 있으면 D5를 재검토하고, 없으면 api-list를 고치면서 Notion 역반영을 제안한다
- [ ] **필터의 사용자 조회 비용.** 모든 인증 요청에 DB 왕복 1회가 붙는다. 캐시 도입 여부는 실사용 트래픽을 보고 정한다 — 지금 정하면 근거 없는 수치가 굳는다
- [ ] **탈퇴 계정의 `/auth/refresh` 응답.** D5로 revoke를 안 하므로 탈퇴한 사용자도 `/auth/refresh`가 **200과 새 토큰을 반환**한다(그 토큰으로 API를 부르면 401). 클라이언트 입장에서 혼란스러울 수 있다 — `AuthService.refresh`에 활성 검사를 넣을지는 실제 클라이언트 동작을 보고 정한다

> **해소됨** — `business/user → data/auth` 참조를 어떻게 처리할지는 D5(revoke 생략)로 닫혔다.

## 작업 단계

- [x] **Phase 0 — ArchUnit 정리 (2026-07-31 결정 · 2026-08-03 코드 반영)**
      ⚠️ **07-31 에는 결정과 검증만 하고 코드가 커밋되지 않았다.** 계획서·PROGRESS 에는 "완료 ✅" 로 적혀 있었지만 `57f5ca1` 이 바꾼 것은 문서 3개뿐이었고, ArchUnit 파일의 마지막 변경은 `efef567`(07-29) 이었다. 08-03 에 재실행해 실제로 반영했다.
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

- [ ] **Phase 1 — 조회·수정**
      `UserController`(`@RequestMapping("/api/v1/users")`) + `UserService` + DTO 2종. `GET`은 `findByIdAndDeletedAtIsNull`로 조회하고 없으면 `USER_NOT_FOUND`. `PATCH`는 보낸 필드만 반영(D3), 병합은 **서비스에서**(D6). 요청 DTO에 `@NotBlank` + `@Size(max = 100)`(D7).
      ⚠️ **`User.updateProfile`이 이미 있고, 두 필드를 무조건 덮어쓴다.** 닉네임만 담긴 PATCH에 `updateProfile(nick, null)`로 호출하면 **`profile_image_url`이 지워진다** — 에러 없이 DB에 반영되는 데이터 손실이라 응답만 봐서는 모른다. 아래 완료 기준이 정확히 이걸 막는다. 현재 이 메서드의 호출처는 0곳이다(REQ-07에서 선반영만 됨).
      완료 기준: 응답 필드가 Notion `GET /users/me` 5개와 정확히 일치(`updated_at` 없음, 전역 snake_case) · **`PATCH`에 닉네임만 보내면 `profile_image_url`이 유지됨** · 101자 닉네임이 500이 아니라 400 · ArchUnit 8건 통과

- [ ] **Phase 2 — 탈퇴**
      **선행: Notion `API I/F` 원본에서 revoke 서술 확인**(미결 참고 — api-list와 D5가 어긋난다). 그다음 `users.deleted_at` 기록(`BaseSoftDeleteEntity.softDelete()`) + `user_social_accounts` 하드 삭제. 204 반환(본문 없음 — `AuthController.logout` 선례대로 `ApiResponse`를 씌우지 않는다). refresh는 revoke하지 않으며(D5) **그 이유를 주석으로 남긴다** — 근거가 없으면 다음 사람이 "빠뜨렸다"고 보고 채워 넣어 예외를 늘린다.
      `UserSocialAccountRepository`에 **삭제 메서드가 없다**(현재 `findByProviderAndProviderUserId` 하나뿐) — `deleteByUserId(UUID)` 추가가 필요하다.
      완료 기준: 탈퇴 → 같은 카카오 계정으로 재로그인 시 **새 `users.id`가 발급됨**(로컬 DB 왕복으로 실제 확인 — 목으로는 D1이 검증되지 않는다) · ArchUnit 8건 통과(예외가 늘지 않았음의 확인)

- [ ] **Phase 3 — 필터 활성 검사 (포트)**
      `framework/security/UserStatusChecker` 인터페이스 정의 → `UserService`가 구현 → `JwtAuthenticationFilter`가 인터페이스를 주입받아 검사. AGENTS.md §3에 이 패턴을 기록한다(framework가 인터페이스를 정의하고 business가 구현하는 첫 사례).
      **비활성 사용자일 때는 예외를 던지지 않고 `SecurityContext`를 세팅하지 않은 채 통과시킨다.** 그러면 `SecurityConfig`의 `authenticationEntryPoint`가 기존 규격대로 `ApiResponse.error(UNAUTHORIZED)` + 401을 내려준다 — 필터에서 던지면 `GlobalExceptionHandler`에 닿지 않아(필터는 DispatcherServlet 앞이다) 응답 형태가 갈린다.
      완료 기준: 탈퇴 직후 기존 access 토큰으로 `GET /users/me` 호출 시 **401 + 기존 에러 본문 형태** · **규칙이 살아 있는지 프로브로 확인**(필터에 `UserRepository` 직참조를 일부러 심어 빨간불이 되는지 — CLAUDE.md 계약. 2026-08-03 실측상 `FRAMEWORK_MUST_NOT_KNOW_DOMAIN`과 `LAYER_DIRECTION` **둘 다** 발화한다) · `spotlessApply` + `build -x test` + `checkstyleMain -PciStrict` 통과

## 제약·함정

- **`@Transactional` 롤백** (AGENTS §5) — 탈퇴는 소셜 행 삭제·revoke·`deleted_at`을 한 트랜잭션에서 쓴다. 여기서 예외를 던지면 **셋 다 사라진다.** 남겨야 하는 쓰기가 생기면 `noRollbackFor`를 명시할 것. 2026-07-30 `AuthService.refresh`에서 같은 함정이 실제로 터졌고 **목 기반 테스트는 통과했다**
- **`User.updateProfile`은 부분 반영용이 아니다** (D6) — 두 필드를 무조건 덮어쓴다. 서비스가 병합하지 않고 그대로 부르면 PATCH가 다른 필드를 지운다. **응답은 200으로 정상이고 DB만 조용히 손상된다** — 07-31에 잡은 함정 2건(유령 계정·잔존 토큰)과 같은 종류다
- **`api-list.md`와 D5가 정반대다** — api-list는 탈퇴 시 토큰 전체 revoke라고 적고 있다. Notion 원본 확인 전에는 어느 쪽도 구현 근거가 아니다(AGENTS §0). Phase 2 선행 과제
- **D1은 목으로 검증되지 않는다.** 유령 계정 경로는 `UNIQUE (provider, provider_user_id)`와 실제 조회 결과가 만드는 현상이라 **로컬 DB 왕복이 유일한 확인 수단**이다
- **ArchUnit 규칙을 고치면 프로브를 심는다** (CLAUDE.md) — 규칙이 조용히 공허해진 전례가 있다. 통과/실패만 봐서는 알 수 없다
- **`allowEmptyShould`를 다시 `true`로 되돌리지 말 것** (Phase 0) — 새 규칙을 추가했는데 대상이 0개라 실패하면, 완화가 아니라 **규칙이 시기상조라는 신호**다
- **포트 인터페이스는 `framework`에 둔다.** `business/user`에 두면 필터가 그걸 참조하게 되어 규칙 #4 위반이다 — 인터페이스를 어디 두느냐가 이 설계의 전부다
- **삭제 순서** — FK는 `user_social_accounts.user_id → users.id` 한 방향이다. 소셜 행을 먼저 지우므로 제약 위반은 없지만, 순서를 뒤집을 이유도 없다
- **Entity는 Service 밖으로 나가지 않는다** (AGENTS §5) — ArchUnit 규칙 #2가 잡는다
- **`PUBLIC_PATHS`는 건드리지 않는다.** `/users/**`는 전부 인증 대상이고, 필터는 토큰이 있을 때만 동작하므로 공개 경로에는 DB 조회가 붙지 않는다
