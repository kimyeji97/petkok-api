# PLAN-REQ-07 · auth 도메인 + DB 환경 구성

> 출처: 2026-07-27 · 2026-07-29 세션 · 작성: 2026-07-27 · 최종 갱신: 2026-07-29 · 상태: 🟡 진행 (Phase 1~2 완료)

## 배경

도메인 구현이 하나도 없는 상태에서 auth가 첫 수직 슬라이스다. 그런데 그 앞에 두 가지가 막혀 있다.

**① 실행 가능한 DB가 없다.** `application-local.yml`은 `jdbc:postgresql://localhost:5432/petkok`를 기본값으로 두고 있지만, 이 값으로 실제 기동이 되는지 확인된 적이 없다. dev/prod는 인프라 자체가 없다. auth는 `users`·`user_social_accounts`·`refresh_tokens`를 모두 건드리므로 DB 없이는 한 줄도 검증할 수 없다.

**② 검증 수단이 없다.** `src/test`가 없어 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐이다. auth의 핵심 로직(토큰 만료, 로테이션, 재사용 감지)은 눈으로 확인하기 어렵고 조용히 깨지는 종류다.

> **①②의 현재 상태 (2026-07-29 갱신)** — ①은 해소됐다(Phase 1 완료). ②는 절반만 해소됐다: `src/test`가 생기고 ArchUnit 구조 규칙 8개가 CI에서 돌지만 **도메인 로직 테스트는 여전히 0개**다. 위 문단은 작성 시점의 문제 인식으로 남겨 둔다.

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
- ~~`src/test` 신설 · ArchUnit 활성화~~ → **REQ-14에서 선처리됨(2026-07-29).** 남은 것은 auth 로직 테스트뿐

**제외**
- **Testcontainers** — 2026-07-29 기각. 순수 로직은 DB 없이 검증되고, 리포지토리·마이그레이션은 로컬 DB로 확인한다. 빌드 시간과 Docker 의존을 지금 떠안지 않는다
- **Supabase 프로젝트 분리** — 2026-07-29 기각. 무료 플랜이 활성 2개를 허용해 가능하지만, 스키마 분리 방침을 유지한다
- **`local` 소켓 scram 전환** — `yjkim` 롤에 비밀번호가 없어 지금 바꾸면 관리 접속이 잠긴다. 앱이 쓰는 TCP 경로는 이미 scram이라 실질 보호는 걸려 있다
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
| **HTTP 클라이언트** (2026-07-29) | **`framework/util/http/RestClientBase` 상속.** `KakaoOAuthClient extends RestClientBase` | REQ-05에서 만든 자산을 그대로 쓴다. `@Autowired RestTemplate` + get/post/exchange 래퍼가 이미 있고, 로깅 인터셉터와 거기서 얻은 계약 2건(응답 버퍼링 필수 · `getStatusCode()` 원본 위임)이 살아 있다 | **RestClient 전환.** Notion 소스 구조 §7이 RestClient로 적고 있으나, 갈아타면 REQ-05 로깅 인터셉터를 다시 만들고 버퍼링·비표준 상태코드 대응을 처음부터 재현해야 한다 → **Notion §7을 RestTemplate 기준으로 역반영한다** |
| **dev/prod 분리** (2026-07-29) | **한 Supabase 프로젝트 안에서 스키마로 분리** | 2026-07-27 방침 유지. 프로젝트가 하나라 접속 정보·대시보드·백업이 한 곳에 모인다 | **Supabase 프로젝트 2개.** 무료 플랜이 활성 프로젝트 2개를 허용해 물리적 분리가 가능하고 Flyway·Hibernate 스키마 설정을 건드릴 필요도 없지만, 채택하지 않았다 |
| **스키마 이름** (2026-07-29) | **`petkok_dev` · `petkok_prod`**, 로컬은 **`petkok_local`** | 로컬까지 이름 있는 스키마를 쓰는 이유는 **설정 경로를 매일 실행시키기 위해서**다. 로컬만 `public`으로 두면 Flyway·Hibernate 스키마 지정이 로컬에서 한 번도 검증되지 않고 **dev 배포에서 처음 갈린다** | **로컬만 `public` 유지.** 이미 V1이 적용돼 있어 손이 덜 가지만, 위 이유로 기각. 로컬 `petkok` DB의 `public` 스키마에 있는 기존 테이블은 버린다(행 0건) |
| **Testcontainers** (2026-07-29) | **도입하지 않는다** | auth 핵심 로직(토큰 만료·로테이션·재사용 감지)은 I/O 없는 순수 로직이라 DB 없이 단위테스트로 검증된다. 리포지토리·마이그레이션은 로컬 DB로 확인한다 | **Testcontainers.** 로컬 DB 상태에 의존하지 않고 CI에서 실제 PostgreSQL 17에 마이그레이션을 적용해볼 수 있으나, 빌드 시간과 Docker 의존이 따라온다 |

이전 세션에서 확정되어 [api-list.md](../specs/api-list.md)에 반영된 결정(refresh 토큰 DB 저장, 로테이션 적용, 공개 경로 범위, PATCH 통일)은 여기서 다시 다루지 않는다.

> ⚠️ **스키마 분리를 택했으므로 아래 두 가지가 이 계획의 최대 리스크다.**
> ① Flyway와 Hibernate **양쪽 모두**에 스키마를 알려야 한다. 한쪽만 설정하면 테이블은 지정 스키마에 생기는데 조회는 `public`을 보는(또는 그 반대) 상태가 된다. **에러 없이 "테이블이 없다"로 나타난다.**
> ② `V1__init.sql`은 스키마를 명시하지 않아 `search_path`에 의존한다. **이 의존은 유지한다** — Flyway `default-schema`가 `search_path`를 잡아 주므로 마이그레이션 파일에 스키마를 하드코딩하면 오히려 환경별로 못 쓰게 된다.
>
> **로컬까지 `petkok_local`을 쓰기로 한 것이 이 리스크의 완화책이다.** 로컬만 `public`이면 스키마 지정 경로가 한 번도 실행되지 않아 dev 배포에서 처음 갈린다.

## 미결 질문

> 2026-07-27 Notion 대조에서 나온 항목 중 **공개 경로**와 **`condition_tag`** 는 해소됐다. 아래 "해소된 질문" 참고.

- [x] **로컬 PostgreSQL 접속 정보** — **Postgres.app으로 확정(2026-07-29).** `localhost:5433` / DB `petkok` / 롤 `root`(비슈퍼유저, DB 소유자). Docker는 쓰지 않는다. `public` 스키마 소유자가 `pg_database_owner`라 **DB 소유권만 넘기면** Flyway의 `CREATE` 권한이 따라온다
- [x] **PostgreSQL 버전** — **17로 확정.** 로컬 17.10, 운영 Supabase 17.6. `gen_random_uuid()`는 13+ 코어 제공이라 `pgcrypto` 불필요하고 `V1__init.sql`에도 `CREATE EXTENSION`이 없다. **Supabase는 18을 아직 지원하지 않는다**(2026-01 목표를 넘겼고 "eventually in 2026") — 로컬을 18로 두면 운영보다 앞서므로 선택지가 아니었다
- [x] **스키마 이름과 적용 범위** — **`petkok_local` / `petkok_dev` / `petkok_prod` 확정(2026-07-29).** 로컬까지 이름 있는 스키마를 쓴다. 기존 로컬 `public` 테이블은 버린다(행 0건이라 잃을 게 없다)
- [ ] **동시 refresh 요청 처리.** 앱 재시작·병렬 요청으로 같은 refresh가 거의 동시에 두 번 오면, 정상 사용자인데도 재사용 감지에 걸려 전 기기 로그아웃된다. 짧은 유예 윈도우를 둘지 감수할지 정해지지 않았다 *(Phase 5에서 결정해도 늦지 않다)*
- [x] **Kakao 앱 등록 여부** — **등록 완료·키 보유(2026-07-29).** Phase 4 착수 조건이 풀렸다. 설정 골격(`kakao.client-id`/`client-secret`/`redirect-uri`)도 선배치했다. 콘솔 설정 검증은 별도 진행 중
- [ ] **응답 본문 마스킹 범위.** `RestTemplateLoggingInterceptor`가 본문을 `log.info`로 그대로 찍는다. 카카오 토큰 응답에 `access_token`이 평문으로 온다. 전체 본문을 끌지, 키 단위로 마스킹할지, 특정 URL만 예외로 둘지 미정

### 해소된 질문 (2026-07-27)

- [x] **공개 경로 범위.** Notion API I/F 행 본문에 답이 있었다 — `/auth/refresh`는 "🔓 인증 불필요"(refresh 토큰을 body로 수령), `/auth/logout`은 "🔒 인증 필요, **Request Body 없음**"이다. body가 없으니 access 토큰으로만 사용자를 식별할 수 있다. → **`PUBLIC_PATHS`는 `/auth/kakao` + `/auth/refresh` + `/actuator/health`를 개별 경로로 나열하고, `/auth/logout`은 제외한다.** 와일드카드 `/auth/**`는 쓰지 않는다 (AGENTS §5 갱신 완료)
- [x] **`condition_tag` 허용값 → 7개.** Notion ERD 설계가 "공통(정상/활발) + 게코 전용(거식/탈피도와줌/탈피완료/거꾸리/구토)"으로 명시한다. `V1__init.sql` 주석의 4개가 누락이다 (컬럼은 `varchar(50)`이라 스키마 변경은 불필요)

## 작업 단계

- [x] **Phase 1 — 로컬 DB 기동** — **완료 (2026-07-29)**
      완료 기준 충족: `bootRun` 정상 기동, `flyway_schema_history`에 `v1/init/success=true`, 테이블 9개 생성.
      실제 구성은 **PostgreSQL 17.10 / 포트 5433**(Postgres.app). 운영 Supabase가 17.6이라 메이저를 맞췄다.
      **계획에 없던 작업 2건** — ① `pg_hba`가 전부 `trust`라 비밀번호가 검증되지 않아 `host` 라인을 `scram-sha-256`으로 전환했다 ② Flyway가 PostgreSQL 16까지만 검증돼 경고를 내서 10.10.0 → 10.22.0으로 상향했다(Spring Boot BOM 고정값 덮어쓰기).
      ⚠️ **`ddl-auto: validate`는 사실상 아무것도 검증하지 않았다** — `@Entity` 클래스가 0개다. 실효는 Phase 3에서 생긴다

- [x] **Phase 2 — 환경 분리와 스키마 지정** — **완료 (2026-07-29)**
      완료 기준 4개 모두 충족.
      ① **세 프로파일 전부 실제로 띄워 확인했다.** 같은 로컬 DB에 프로파일만 바꿔 붙였더니 `petkok_local`/`petkok_dev`/`petkok_prod`가 각각 생성되고 V1이 적용됐다(dev·prod 스키마는 확인 후 삭제). prod까지 띄운 이유는 `application-prod.yml`의 오타가 **배포 시점에야 드러나기 때문**이다
      ② `petkok_local`을 통째로 지우고 새 설정 경로로 재생성했다 — `Creating schema "petkok_local"` → `Successfully applied 1 migration`, 테이블 9개 + `flyway_schema_history`. `public` 스키마는 존재하지 않아 fallback 위험도 없다
      ③ 세 프로파일 모두 `/actuator/health` 200
      ④ 레포에서 실제 비밀번호를 제거했다 — 아래 "계획에 없던 작업" 참조
      **설계**: 스키마 값의 출처를 `db.schema` **한 곳**으로 모으고 Flyway·Hibernate가 함께 참조하게 했다. "한쪽만 지정" 사고(위 최대 리스크 ①)가 구조적으로 불가능해진다. `application.yml`에는 기본값을 두지 않아 프로파일이 값을 빠뜨리면 기동 즉시 실패한다.
      **계획에 없던 작업 3건** —
      ① **`docs/PROGRESS.md`에 로컬 DB 실제 비밀번호가 평문으로 커밋돼 있었다**(2026-07-29 `5c7313b`, public 레포에 이미 푸시됨). URI 파싱 함정을 설명하며 실제 값을 예시로 쓴 것이다. 문서는 더미 값으로 교체했으나 **git 이력에는 남아 있다** — 로컬 비밀번호 교체 여부는 별도 판단 필요
      ② `application-local.yml` 기본값이 Phase 1 실측과 어긋나 있었다(`5432`/`postgres`/`postgres` → `5433`/`root`). `DB_PASSWORD`는 기본값을 **제거**했다 — scram 환경에서 더미 값은 "비밀번호 틀림"으로만 보여 오진을 부른다
      ③ `.env`의 `KEY=`(빈 값)은 미설정이 아니라 **빈 문자열**이라 `${VAR:기본값}`의 기본값을 무력화한다. README가 권하는 `set -a && . ./.env` 경로에서 실제로 밟는 함정이라 `.env.example`·README에 경고를 넣었다
      *(dev/prod 인스턴스 실제 구축은 인프라가 생긴 뒤 — 여기서는 설정 구조와 로컬 검증까지)*
      ⚠️ **Hibernate가 같은 스키마를 보는지는 아직 검증되지 않았다.** `@Entity` 0개라 `validate`가 아무것도 안 한다 — Phase 3에서 처음 실증된다

- [ ] **Phase 3 — V2 마이그레이션 + 엔티티** ← **다음 작업**
      `refresh_tokens` 테이블, `User`/`UserSocialAccount`/`RefreshToken`.
      완료 기준: V2 적용 후 `validate` 통과 (엔티티 매핑과 DDL 불일치가 여기서 잡힌다)

- [ ] **Phase 4 — Kakao 로그인** *(앱 등록 완료 — 선행 조건 해제됨)*
      인가코드 → 토큰 교환 → 사용자 정보 → 조회/자동가입 → access+refresh 발급.
      `KakaoOAuthClient extends RestClientBase` (`business/auth/service/oauth/`).
      설정 3개(`kakao.client-id`/`client-secret`/`redirect-uri`)는 2026-07-29에 선배치됐다 — 바인딩 클래스(`KakaoProperties`)는 이 Phase에서 만든다.
      완료 기준: 로그인 왕복 성공 + **로그에 토큰 원문이 남지 않는다**(실제 로그를 눈으로 확인)

- [ ] **Phase 5 — 로테이션 / 로그아웃**
      완료 기준: refresh 재발급 시 이전 토큰이 즉시 무효 · revoke된 토큰 재제시 시 해당 사용자 전체 revoke + `INVALID_TOKEN`

- [ ] **Phase 6 — 검증 체계** *(선행 절반은 2026-07-29에 이미 끝났다)*
      ~~`src/test` 신설, ArchUnit 활성화~~ → **REQ-14에서 완료.** `src/test/java/com/petkok/architecture/`에 구조 규칙 8개가 CI 게이트로 동작한다. 따라서 이 Phase에 남은 것은 **auth 로직 테스트뿐**이다.
      Testcontainers는 쓰지 않는다 — 순수 로직은 단위테스트, 리포지토리·마이그레이션은 로컬 DB로 확인.
      완료 기준: 토큰 만료·로테이션·재사용 감지 테스트 통과 + CI green
      ※ **`business/auth`·`data/auth`가 생기는 순간 ArchUnit 규칙이 처음으로 실제 검사를 시작한다.** 두 패키지는 반드시 같은 이름이어야 한다(AGENTS §3)

Phase 1~2와 3~6은 PR을 나눈다.

## 제약·함정

- **Kakao 토큰 응답 본문에 `access_token`이 평문으로 들어온다.** `RestTemplateLoggingInterceptor`는 **헤더만** 마스킹하고 본문은 `log.info`로 그대로 찍는다. Phase 4에서 본문 마스킹을 함께 처리하지 않으면 토큰이 로그에 남는다 (AGENTS §5 위반)
- **스키마를 분리하면 Flyway와 Hibernate 양쪽에 알려야 한다.** 한쪽만 설정하면 테이블은 지정 스키마에 생성되는데 조회는 `public`을 보는(또는 그 반대) 상태가 된다. `V1__init.sql`은 스키마를 명시하지 않아 현재 `search_path`에 의존한다
- **`kakao.client-secret`이 비어 있으면 토큰 교환 요청에서 파라미터 자체를 생략해야 한다.** 빈 값을 실어 보내면 카카오가 거부한다. 콘솔에서 "사용함"으로 켠 경우에만 필요한 값이라 비어 있는 상태가 정상 시나리오다
- **`redirect-uri`는 콘솔 등록값·클라이언트가 인가 요청에 쓴 값과 문자 단위로 같아야 한다.** 서버가 리다이렉트를 받지는 않지만 토큰 교환 요청에 들어간다. 다르면 `KOE006`/`invalid_grant`로 떨어진다
- **Kakao 앱 키는 4종이다 — 서버가 쓰는 것은 REST API 키.** 네이티브·JavaScript 키와 혼동하기 쉽고, **Admin 키는 전권이라 서버에도 두지 않는다**
- **`refresh_tokens.token_hash`는 `varchar(64)`면 충분하다.** api-list.md의 제안 스키마는 `varchar(255)`지만 SHA-256 hex는 64자 고정이다. UNIQUE 인덱스가 걸리는 컬럼이다
- **엔티티 ID를 앱 코드가 직접 채우면 `save()`가 merge로 빠진다.** Spring Data JPA가 "ID가 있으니 기존 엔티티"로 판단해 INSERT 전에 SELECT를 한 번 더 날린다. `@GeneratedValue`로 Hibernate에 맡긴다
- **`JwtAuthenticationFilter`는 이미 `isAccessToken()`으로 refresh 토큰의 인증 사용을 막고 있다.** 확인 완료 — 이 검사를 제거하면 refresh 토큰으로 API 호출이 뚫린다
- **API 계약의 1차 출처는 Notion API I/F다.** `docs/specs/api-list.md`는 파생 요약이며 충돌하면 Notion이 이긴다 (AGENTS §0). 2026-07-27에 39개 엔드포인트 기준으로 정합을 맞췄으나, Notion이 바뀌면 이 문서가 아니라 Notion을 먼저 본다
- **`user_social_accounts` 조회/연결/해제 엔드포인트는 존재하지 않는다.** `api-list.md`가 독자 추가했던 것을 제거했다. 그런데 이것이 "공개 경로 범위" 결정(2026-07-23)에서 "인증 필요한 기능을 `/auth/` 밖에 둔 사례"로 인용됐던 엔드포인트다 — 근거가 사라졌으므로 위 미결 1번을 반드시 확인해야 한다
- **`SecurityConfig.PUBLIC_PATHS`에 와일드카드를 쓰지 않는다.** `/api/v1/auth/**`로 두면 `DELETE /auth/logout`이 무인증 노출된다 — 이 엔드포인트는 Request Body가 없어 access 토큰이 유일한 사용자 식별 수단이다. Phase 5에서 반드시 개별 경로로 좁힐 것 (AGENTS §5 계약)
- **`ConditionTag` enum은 7개다.** `V1__init.sql` 주석이 4개로 빠져 있으니 함께 고칠 것 (REQ-10 범위)
- **`SecurityConfig.PUBLIC_PATHS`를 넓히지 않는다.** `/api/v1/auth/**`는 permitAll이라 인증이 필요한 기능을 그 아래 두면 무인증 노출된다 *(AGENTS §5에 이미 계약으로 등재됨)*
