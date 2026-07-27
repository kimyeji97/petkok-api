# PLAN-REQ-07 · auth 도메인 + DB 환경 구성

> 출처: 2026-07-27 세션 · 작성: 2026-07-27 · 상태: 📝 초안

## 배경

도메인 구현이 하나도 없는 상태에서 auth가 첫 수직 슬라이스다. 그런데 그 앞에 두 가지가 막혀 있다.

**① 실행 가능한 DB가 없다.** `application-local.yml`은 `jdbc:postgresql://localhost:5432/petkok`를 기본값으로 두고 있지만, 이 값으로 실제 기동이 되는지 확인된 적이 없다. dev/prod는 인프라 자체가 없다. auth는 `users`·`user_social_accounts`·`refresh_tokens`를 모두 건드리므로 DB 없이는 한 줄도 검증할 수 없다.

**② 검증 수단이 없다.** `src/test`가 없어 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐이다. auth의 핵심 로직(토큰 만료, 로테이션, 재사용 감지)은 눈으로 확인하기 어렵고 조용히 깨지는 종류다.

세션 도중 DB 엔진을 MySQL로 바꾸는 안이 검토됐다가 기각됐다. 아래 "결정" 참조.

**③ 설계 1차 출처가 레포 밖에 있다.** 상위 설계(ADR-001·ADR-002, 테이블 정의서, API I/F, 소스 구조)는 Notion에 있고 레포의 `docs/specs/api-list.md`는 그것을 보지 않고 작성됐다. 2026-07-27 대조 결과 auth 계약에서 충돌이 나왔다 — 아래 "미결 질문" 1~3번.

## 범위

**포함**
- 로컬 PostgreSQL 기동 확인 + dev/prod 환경 분리 설계
- `V2__refresh_tokens.sql` (스키마는 [api-list.md](../specs/api-list.md)에서 이미 확정)
- 엔티티 3종: `User` · `UserSocialAccount` · `RefreshToken`
- Kakao 인가코드 로그인 / 자동가입
- refresh 로테이션 + 재사용 감지
- **RestTemplate 인터셉터의 응답 본문 마스킹** — REQ-05에서 "실제 연동 시점에 결정"으로 미뤄둔 항목이 이번 작업이다
- `src/test` 신설 · ArchUnit 활성화

**제외**
- **MySQL 전환** — 2026-07-27 대화에서 검토 후 기각. 아래 "결정" 참조
- **만료 refresh 행 정리 배치** — 스케줄러 도입 결정이 함께 필요하다. 운영 부담이 실제로 보이는 시점에 별도로 다룬다 *(2026-07-23 결정)*
- **user / pet 도메인** — REQ-08 · REQ-09로 분리. `PetAccessGuard` 소유권 앵커도 여기 포함되지 않는다
- **Google · Apple 소셜 로그인** — `user_social_accounts.provider`가 세 값을 허용하도록 설계돼 있으나, 이번엔 Kakao만 구현한다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| DB 엔진 | **PostgreSQL 유지** | **Notion ADR-002(Status: Accepted, 2026-06-29)가 Supabase PostgreSQL을 정식 채택**했다. 코드 변경도 0이다 — `V1__init.sql`이 PostgreSQL 전용 기능(부분 인덱스 7개, `uuid` 타입, `gen_random_uuid()`)에 이미 의존한다 | **MySQL/MariaDB 전환.** 스키마 전면 재작성 + 부분 인덱스 7개 포기 + 드라이버·Flyway 모듈 교체 + 문서 4곳 수정 + ADR이 따라온다. 얻는 것이 없다. *(전환 검토의 발단은 로컬 접속 정보 `localhost:3306`/`root` — MySQL 기본 포트·관례 계정이었다. 로컬에 깔린 다른 엔진 정보였던 것으로 확인)* |
| PK 전략 | **현행 유지** (`uuid` + `gen_random_uuid()`) | PostgreSQL은 heap 테이블이라 PK 순서가 물리 배치를 좌우하지 않는다. 랜덤 UUID를 그대로 써도 된다 | **`binary(16)` + 시간 정렬 UUID.** InnoDB 클러스터드 인덱스의 페이지 분할을 피하려던 것으로, MySQL 전제에서만 성립한다 |
| 계획 범위 | DB 환경 구성 + auth | 환경 없이는 auth를 검증할 수 없다. 반대로 auth 없이 환경만 잡으면 그 환경이 맞는지 알 수 없다 | auth 단독 / 환경 단독 / auth→user→pet 수직 슬라이스 전체 |

이전 세션에서 확정되어 [api-list.md](../specs/api-list.md)에 반영된 결정(refresh 토큰 DB 저장, 로테이션 적용, 공개 경로 범위, PATCH 통일)은 여기서 다시 다루지 않는다.

## 미결 질문

> 1~3번은 2026-07-27 Notion 설계 문서 대조에서 나왔다. **Notion API I/F가 계약의 1차 출처이므로 `api-list.md`가 아니라 Notion 쪽을 기준으로 정해야 한다.**

- [ ] **`/auth/refresh`·`/auth/logout`의 🔒 표기 의미.** *(경로·메서드는 2026-07-27에 Notion 기준으로 정합 완료 — `DELETE /auth/logout`)* Notion API I/F가 둘 다 **인증 필요 = YES**로 표시하는데, 그것이 **access 토큰**을 뜻하면 `SecurityConfig.PUBLIC_PATHS`를 `/api/v1/auth/kakao` 하나로 좁혀야 하고, **refresh 토큰 제시**를 뜻하면 현행 `/auth/**`가 맞다. **이걸 정하기 전에는 auth 구현을 시작하지 않는다** — 틀리면 두 엔드포인트가 무인증 노출된다
- [ ] **HTTP 클라이언트.** Notion 소스 구조 §7은 `KakaoOAuthClient`가 **RestClient**를 쓴다고 명시한다. 레포는 REQ-05에서 **RestTemplate** + 로깅 인터셉터를 만들어 뒀다. RestTemplate으로 가고 Notion을 고칠지, RestClient로 갈아탈지(그러면 REQ-05 인터셉터를 다시 만들어야 한다) 결정 필요
- [ ] **로컬 PostgreSQL 접속 정보.** 세션에서 받은 `localhost:3306` / `root`는 MySQL 것이었다. 실제 PostgreSQL 인스턴스가 로컬에 있는가? 없다면 준비 방법(Docker / Postgres.app / Supabase 무료 티어 직결) 중 무엇으로 갈지
- [ ] **PostgreSQL 버전.** `gen_random_uuid()`는 13+ 내장이고 그 미만은 `pgcrypto` 확장이 필요하다. README는 15+를 요구하지만 실제 로컬 버전은 확인되지 않았다
- [ ] **dev/prod 분리 방식.** "스키마로 분리"라고만 정해져 있다. 같은 데이터베이스 안의 schema로 나눌지, 데이터베이스 자체를 나눌지 미확정. Supabase 무료 티어는 DB가 하나뿐이라 이 선택이 인프라 형태에 묶인다
- [ ] **동시 refresh 요청 처리.** 앱 재시작·병렬 요청으로 같은 refresh가 거의 동시에 두 번 오면, 정상 사용자인데도 재사용 감지에 걸려 전 기기 로그아웃된다. 짧은 유예 윈도우를 둘지 감수할지 정해지지 않았다
- [ ] **Testcontainers 도입 여부.** 로컬 DB 상태에 의존하지 않고 마이그레이션·리포지토리를 검증할 수 있으나, 빌드 시간과 Docker 의존이 따라온다
- [ ] **Kakao 앱 등록 여부.** REST API 키·redirect URI가 있어야 연동 단계 검증이 가능하다

## 작업 단계

- [ ] **Phase 1 — 로컬 DB 기동**
      `V1__init.sql` 9개 테이블이 Flyway로 생성되고 `ddl-auto: validate`가 통과.
      완료 기준: `./gradlew bootRun`이 예외 없이 뜨고 `flyway_schema_history`에 V1 성공 행이 남는다

- [ ] **Phase 2 — 환경 분리와 시크릿**
      local/dev/prod 프로파일별 접속 분리, `.gitignore`에 로컬 override 패턴 추가.
      완료 기준: 레포에 실제 비밀번호가 없고, 프로파일 전환만으로 접속 대상이 바뀐다
      *(dev/prod 실제 구축은 인프라가 생긴 뒤 — 여기서는 설정 구조만)*

- [ ] **Phase 3 — V2 마이그레이션 + 엔티티**
      `refresh_tokens` 테이블, `User`/`UserSocialAccount`/`RefreshToken`.
      완료 기준: V2 적용 후 `validate` 통과 (엔티티 매핑과 DDL 불일치가 여기서 잡힌다)

- [ ] **Phase 4 — Kakao 로그인**
      인가코드 → 토큰 교환 → 사용자 정보 → 조회/자동가입 → access+refresh 발급.
      완료 기준: 로그인 왕복 성공 + **로그에 토큰 원문이 남지 않는다**

- [ ] **Phase 5 — 로테이션 / 로그아웃**
      완료 기준: refresh 재발급 시 이전 토큰이 즉시 무효 · revoke된 토큰 재제시 시 해당 사용자 전체 revoke + `INVALID_TOKEN`

- [ ] **Phase 6 — 검증 체계**
      `src/test` 신설, ArchUnit 활성화(AGENTS §6이 "feature 도메인 도입 시점까지 보류"라 한 그 시점).
      완료 기준: 토큰 만료·로테이션·재사용 감지 테스트 통과 + CI green

Phase 1~2와 3~6은 PR을 나눈다.

## 제약·함정

- **Kakao 토큰 응답 본문에 `access_token`이 평문으로 들어온다.** `RestTemplateLoggingInterceptor`는 **헤더만** 마스킹하고 본문은 `log.info`로 그대로 찍는다. Phase 4에서 본문 마스킹을 함께 처리하지 않으면 토큰이 로그에 남는다 (AGENTS §5 위반)
- **스키마를 분리하면 Flyway와 Hibernate 양쪽에 알려야 한다.** 한쪽만 설정하면 테이블은 지정 스키마에 생성되는데 조회는 `public`을 보는(또는 그 반대) 상태가 된다. `V1__init.sql`은 스키마를 명시하지 않아 현재 `search_path`에 의존한다
- **`refresh_tokens.token_hash`는 `varchar(64)`면 충분하다.** api-list.md의 제안 스키마는 `varchar(255)`지만 SHA-256 hex는 64자 고정이다. UNIQUE 인덱스가 걸리는 컬럼이다
- **엔티티 ID를 앱 코드가 직접 채우면 `save()`가 merge로 빠진다.** Spring Data JPA가 "ID가 있으니 기존 엔티티"로 판단해 INSERT 전에 SELECT를 한 번 더 날린다. `@GeneratedValue`로 Hibernate에 맡긴다
- **`JwtAuthenticationFilter`는 이미 `isAccessToken()`으로 refresh 토큰의 인증 사용을 막고 있다.** 확인 완료 — 이 검사를 제거하면 refresh 토큰으로 API 호출이 뚫린다
- **API 계약의 1차 출처는 Notion API I/F다.** `docs/specs/api-list.md`는 파생 요약이며 충돌하면 Notion이 이긴다 (AGENTS §0). 2026-07-27에 39개 엔드포인트 기준으로 정합을 맞췄으나, Notion이 바뀌면 이 문서가 아니라 Notion을 먼저 본다
- **`user_social_accounts` 조회/연결/해제 엔드포인트는 존재하지 않는다.** `api-list.md`가 독자 추가했던 것을 제거했다. 그런데 이것이 "공개 경로 범위" 결정(2026-07-23)에서 "인증 필요한 기능을 `/auth/` 밖에 둔 사례"로 인용됐던 엔드포인트다 — 근거가 사라졌으므로 위 미결 1번을 반드시 확인해야 한다
- **`ConditionTag` enum을 만들기 전에 허용값을 확정할 것.** Notion DDL(7개)과 `V1__init.sql`(4개)이 다르다 (REQ-10 범위)
- **`SecurityConfig.PUBLIC_PATHS`를 넓히지 않는다.** `/api/v1/auth/**`는 permitAll이라 인증이 필요한 기능을 그 아래 두면 무인증 노출된다 *(AGENTS §5에 이미 계약으로 등재됨)*
