# PLAN-REQ-07 · auth 도메인 + DB 환경 구성

> 출처: 2026-07-27 · 2026-07-29 세션 · 작성: 2026-07-27 · 최종 갱신: 2026-07-30 · 상태: 🟡 진행 (Phase 1~6 완료 — **카카오 로그인 왕복 수동 확인만 남음**)

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
| **HTTP 클라이언트** (2026-07-29) | **`framework/util/http/RestClientBase` 상속.** `KakaoOAuthClient extends RestClientBase` | REQ-05에서 만든 자산을 그대로 쓴다. `@Autowired RestTemplate` + get/post/exchange 래퍼가 이미 있고, 로깅 인터셉터와 거기서 얻은 계약 2건(응답 버퍼링 필수 · `getStatusCode()` 원본 위임)이 살아 있다 | **RestClient 전환.** 갈아타면 REQ-05 로깅 인터셉터를 다시 만들고 버퍼링·비표준 상태코드 대응을 처음부터 재현해야 한다. ~~Notion §7을 RestTemplate 기준으로 역반영한다~~ → **2026-07-29 확인 결과 Notion 「소스 구조」에는 `RestClient` 표기가 없고 §5·§12·구현 노트가 전부 RestTemplate 기준이다. 역반영할 것이 없었다 — 이 칸의 기술이 낡았던 것이다** |
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
- [ ] **동시 refresh 요청 처리.** 앱 재시작·병렬 요청으로 같은 refresh가 거의 동시에 두 번 오면, 정상 사용자인데도 재사용 감지에 걸려 전 기기 로그아웃된다. 짧은 유예 윈도우를 둘지 감수할지 정해지지 않았다 ~~*(Phase 5에서 결정해도 늦지 않다)*~~
      → **Phase 5가 끝났는데도 미결이다 (2026-07-30).** 판단 근거가 될 실사용 데이터가 없어 미뤘다 — 유예 윈도우는 "탈취 감지를 얼마나 늦출 것인가"와 맞바꾸는 값이라 추측으로 정하면 보안 쪽이 조용히 깎인다. **현재 구현은 유예 없음**이고, 클라이언트가 재발급 응답의 새 토큰으로 교체하지 못하면 즉시 전 기기 로그아웃이다. 실제 로그인 트래픽이 생긴 뒤 재사용 감지 로그(`Revoked refresh token reused`) 빈도를 보고 정한다
- [x] **Kakao 앱 등록 여부** — **등록·왕복 검증 완료(2026-07-29).** 설정 골격(`kakao.client-id`/`client-secret`/`redirect-uri`)을 선배치하고 `.env`에 실값을 채운 뒤, **인가코드 → 토큰 교환 → `/v2/user/me` 왕복을 실제로 성공**시켰다. Client Secret은 콘솔에서 "사용함" 상태이고 그대로 동작한다. Phase 4 착수 조건이 풀렸다
      *(검증 스크립트는 `.env`를 읽어 실키를 다루므로 레포에 두지 않았다 — 필요하면 재작성한다)*
- [x] **응답 본문 마스킹 범위** — **키 단위 마스킹으로 확정 (2026-07-29).** 전체 본문을 끄는 안은 기각했다: 카카오 오류 응답(`invalid_grant`/`KOE320`/`ip mismatched!`)이 진단의 거의 전부인데 그걸 같이 잃는다. URL별 예외도 기각 — 대상이 늘 때마다 빠뜨린다. `MaskingUtil.maskingCredentialsInBody`가 **키 이름으로** 판단하며 JSON·form 양쪽을 처리한다(REQ-07-09~11).
      **실측으로 결함 1건이 나왔다** — `client_id`(카카오 REST API 키)가 form 본문에 평문으로 찍히고 있었다. 잘못된 인가코드로 실제 왕복을 태워 로그를 눈으로 보다가 발견했고, 마스킹 대상에 추가했다. **`client_secret`만 챙기고 `client_id`는 넘길 뻔했다.**
- [x] **거부 경로 2건의 기대값 — `INVALID_TOKEN` 으로 확정 (2026-07-30).** ① 만료된 refresh 토큰 ② 저장소에 없는 해시. 스펙이 `INVALID_TOKEN`(401)을 명시한 것은 재사용 감지 한 건뿐이라 미결로 두었던 항목이고, Phase 5 구현 시점에 같은 코드로 통일했다 — **거부 사유를 구분해 알려 주면 공격자에게 "이 토큰은 존재하긴 한다"는 정보가 새기 때문**이다. REQ-07-21·22 로 승격
      *(만료 판정은 **저장된 행의 `expires_at`** 으로 한다. JWT 의 `exp` 를 다시 파싱하지 않는 이유는 두 값이 같고 — Phase 4 결정 — 만료 토큰은 파싱 자체가 `ExpiredJwtException` 으로 터지기 때문이다)*
- [ ] **`DELETE /auth/logout` 의 revoke 범위 — api-list 두 줄이 어긋난다 (2026-07-30 등록).** § 1. Auth 는 "access 토큰으로 사용자를 식별해 refresh revoke", § refresh 토큰 저장소는 "해당 토큰 `revoked_at` 설정"이다. **Request Body 가 없어 특정 토큰을 지목할 수단이 없으므로** 검증 계약(REQ-07-18)은 **사용자 전체 revoke** 로 고정했다 — 기기별 로그아웃이 불가능해지는 것이 이 선택의 대가다. Notion API I/F 원본으로 확인이 필요하고, 다르면 § 저장소 문장을 고쳐야 한다
- [ ] **`business/auth` → `data/user` ArchUnit 예외 — 임시. 개선 방향 논의 필요 (2026-07-29 등록).** 자동가입이 `users` 행을 만들어 생긴 참조다. 지금은 `DomainBoundaryTest`에 예외 1건으로 열어 두고 Phase 4를 완주했으나, `data.common`·`timeline`·`framework` 세 예외와 달리 **"설계상 옳다"고 확정된 것이 아니다.** 선택지: 예외 유지 / auth·user 도메인 병합 / user 쪽에 프로비저닝 진입점을 두고 참조 방향만 바꾸기(예외 개수는 그대로)

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

- [x] **Phase 3 — V2 마이그레이션 + 엔티티** — **완료 (2026-07-29)**
      `V2__refresh_tokens.sql` + `User`/`UserSocialAccount`/`RefreshToken` + `SocialProvider` enum.
      완료 기준 충족: `Successfully applied 1 migration ... now at version v2` → `Started PetKokApplication`. 재기동 시 `up to date`로 멱등.
      **`validate`가 실제로 무는지 프로브로 확인했다** — 존재하지 않는 컬럼을 엔티티에 심었더니 `Schema-validation: missing column [probe_column_does_not_exist] in table [users]`로 기동이 막혔다. Phase 1·2에서 두 번 "아무것도 검증하지 않는다"고 적어 둔 항목이라 통과만 보고 넘기지 않았다.
      **Phase 2의 미검증 항목도 여기서 해소됐다** — Hibernate가 Flyway와 같은 스키마(`petkok_local`)를 본다. 다른 스키마를 봤다면 `missing table`로 떨어졌을 것이다.
      **도메인 경계 결정 (2026-07-29)** — 엔티티 배치로 ArchUnit 예외를 0건으로 유지했다.
      | 클래스 | 위치 | 근거 |
      |---|---|---|
      | `User` | `data/user/entity` | users 테이블 소유는 user 도메인 |
      | `UserSocialAccount` | `data/user/entity` | `User`를 `@ManyToOne`으로 참조한다. `data/auth`에 두면 `data/auth → data/user` 위반. 쓰는 쪽이 auth인 것과 별개다 |
      | `RefreshToken` | `data/auth/entity` | `user_id`를 **연관관계 없이 생 `UUID` 컬럼**으로 매핑. 토큰 행에서 User로 탐색할 일이 없어 잃는 것이 없다 |
      ⚠️ **미룬 결정 — `business/auth` → `data/user` 허용 여부.** 자동가입이 `users` 행을 만들므로 **Phase 4에서 반드시 부딪힌다.** 선택지는 ① `DomainBoundaryTest`에 예외 1건 추가(timeline 선례와 같은 방식) ② auth·user 도메인 병합. 검증할 코드가 없는 상태에서 규칙을 먼저 헐겁게 만들지 않으려고 미뤘다.
      ⚠️ **엔티티에 Lombok `@Getter`·`@NoArgsConstructor(PROTECTED)`를 썼다.** `data/common/entity`의 베이스 3종은 수동 getter라 선례가 갈린다. `build.gradle.kts`가 Lombok을 "보일러플레이트 제거"용으로 명시하고 있어 따랐으나, 되돌리려면 지금이 가장 싸다.
      *(Repository는 만들지 않았다 — 이 Phase 범위가 아니고 쓰는 쪽이 없다. Phase 4에서 추가한다)*

- [x] **Phase 4 — Kakao 로그인** — **완료 (2026-07-29)**
      `KakaoProperties` · `KakaoOAuthClient` · `AuthService`(자동가입 + 토큰 발급) · `AuthController`(`POST /auth/kakao`) · user/auth Repository 3종 · 본문 마스킹.
      **완료 기준 ②(로그에 토큰 원문이 남지 않는다) 충족** — 잘못된 인가코드로 **실제 카카오 왕복을 태워** 로그를 눈으로 확인했다: form 본문의 `client_id`·`code`·`client_secret`이 모두 `앞4자+***` 형태로 찍힌다(값은 더미로 옮겨 적지 않는다). 성공 응답의 `access_token`·`refresh_token` 경로는 유효한 1회용 코드가 필요해 실물로 못 태우므로 REQ-07-09로 고정했다.
      ⚠️ **완료 기준 ①(로그인 왕복 성공)은 수동 확인이 남았다.** 유효한 인가코드는 사람이 브라우저로 로그인해야 나온다. 코드 경로는 전부 배선됐고 카카오까지 실제로 요청이 나가는 것(`KOE320` 수신)까지 확인했다.
      **계획에 없던 작업 2건** —
      ① **ArchUnit `NO_CROSS_DOMAIN_DEPENDENCY`에 결함이 있었다.** 슬라이스 패턴이 *의존 대상*에도 적용돼 `framework.config`가 "config" 도메인으로 잡히는 바람에 **`business/auth` → `framework/config`가 위반**으로 떨어졌다. `@AnalyzeClasses` 범위를 좁히는 것으로는 못 막는다(문제는 의존 방향의 끝이다). AGENTS §5가 허용하는 방향이므로 `framework`를 대상에서 제외했고, **프로브로 교차 도메인은 여전히 잡히는 것을 확인**했다. 도메인 코드가 0개라 그동안 드러나지 않았다
      ② **`SecurityConfig.PUBLIC_PATHS` 와일드카드 제거** — 원래 Phase 5 항목인데 이 Phase가 해당 엔드포인트를 만드는 시점이라 함께 처리했다(위 「검증 계약」)
      **결정** — `profile_image_url`은 **저장 전 `https`로 정규화**한다(`KakaoOAuthClient.toHttps`). 클라이언트가 그대로 쓰면 iOS ATS·Android cleartext에 막히고, 같은 경로가 `https`로 200인 것은 2026-07-29에 확인했다. 정규화 지점을 클라이언트로 둔 이유는 provider별 차이를 그 계층에서 흡수하기 위해서다.

- [x] **Phase 5 — 로테이션 / 로그아웃** — **완료 (2026-07-30)**
      `POST /auth/refresh`(로테이션) · `DELETE /auth/logout`.
      Phase 4에서 깔린 것 — `RefreshTokenRepository.findByTokenHash`(revoke된 행도 반환해야 재사용 감지가 성립한다) · `revokeAllByUserId` · `RefreshToken.revoke()`(이미 revoke면 최초 시각 유지). **호출부만 없다.**
      `PUBLIC_PATHS`에 `/auth/refresh`는 이미 들어가 있고 `/auth/logout`은 **의도적으로 빠져 있다** — Request Body가 없어 access 토큰이 유일한 식별 수단이다.
      완료 기준: refresh 재발급 시 이전 토큰이 즉시 무효 · revoke된 토큰 재제시 시 해당 사용자 전체 revoke + `INVALID_TOKEN`
      **구현 완료 (2026-07-30) — 단, 앱 기동 확인이 남았다.** `AuthService.refresh`/`logout` · `RefreshRequest` · `POST /auth/refresh` · `DELETE /auth/logout`(204). 검증 계약 REQ-07-12~22 가 전부 통과하고 CI 게이트(spotless · build · checkstyle `-PciStrict` · test)도 통과한다.
      **계획에 없던 작업 2건** —
      ① **`createRefreshToken` 에 `jti` 를 추가했다.** 없으면 같은 초 재발급 시 이전 토큰과 **완전히 같은 문자열**이 나와 로테이션이 깨진다(위 검증 계약 절 참고). 검증 계약을 먼저 쓰지 않았으면 운영에서야 드러났을 결함이다
      ② **재사용 감지의 전체 revoke 가 롤백되고 있었다 — 아래 「로컬 왕복 검증」 참고**
      **로컬 왕복 검증 (2026-07-30, `petkok_local`).** 사용자 1명 + refresh 행을 심고 실제 HTTP 로 태웠다. 확인한 것 —
      ① `POST /auth/refresh` 200, 옛 행 `revoked_at` 설정(**더티체킹 실증**) + 새 행 INSERT, 새 행의 `token_hash` 가 응답 refresh 토큰의 SHA-256 과 일치
      ② revoke 된 토큰 재제시 → 401 `INVALID_TOKEN` + **해당 사용자 전체 revoke**
      ③ `DELETE /auth/logout` 무토큰 → 401 `UNAUTHORIZED`(PUBLIC_PATHS 실효 확인) · 유효 access 토큰 → **204 본문 0바이트** · refresh 토큰을 access 자리에 → 401(`isAccessToken` 방어 실효)
      ④ 로그에 토큰 원문 0건
      ⚠️ **②는 처음에 실패했다 — 401 은 정상인데 다른 토큰이 살아 있었다.** `@Transactional` 기본 설정에서 예외가 `revokeAllByUserId` 를 **함께 롤백**했기 때문이다. **응답만 보면 완전히 정상으로 보이고, 저장소를 목으로 대체하는 단위 테스트로는 원리적으로 잡히지 않는다**(목은 롤백되지 않는다). `noRollbackFor = BusinessException.class` 로 고쳤고 REQ-07-23 이 애노테이션을 고정한다.
      *(검증 데이터는 확인 후 삭제했다 — `users` 0행)*

- [x] **Phase 6 — 검증 체계** — **완료 (2026-07-30). 단, 별도 단계로 수행하지 않았다** *(선행 절반은 2026-07-29)*
      ⚠️ **계획-실제 이탈.** 남아 있던 "auth 로직 테스트"는 `/testgen` 이 REQ-07-12~23 을 **Phase 5 착수 전에** 작성하면서 처리됐다. 완료 기준("토큰 만료·로테이션·재사용 감지 테스트 통과 + CI green")은 그대로 충족한다 — 만료 REQ-07-06·21, 로테이션 12~15, 재사용 감지 16·17, CI green. **단계를 쪼갠 전제(구현 후 테스트)가 `/testgen` 도입으로 뒤집힌 것**이지 건너뛴 것이 아니다.
      ~~`src/test` 신설, ArchUnit 활성화~~ → **REQ-14에서 완료.** `src/test/java/com/petkok/architecture/`에 구조 규칙 8개가 CI 게이트로 동작한다. 따라서 이 Phase에 남은 것은 **auth 로직 테스트뿐**이다.
      Testcontainers는 쓰지 않는다 — 순수 로직은 단위테스트, 리포지토리·마이그레이션은 로컬 DB로 확인.
      완료 기준: 토큰 만료·로테이션·재사용 감지 테스트 통과 + CI green
      ※ **`business/auth`·`data/auth`가 생기는 순간 ArchUnit 규칙이 처음으로 실제 검사를 시작한다.** 두 패키지는 반드시 같은 이름이어야 한다(AGENTS §3)

~~Phase 1~2와 3~6은 PR을 나눈다.~~ → **실제로는 Phase 3·4·5를 각각 나눴다**(PR #18 · #19 · #20). Phase 마다 검증 가능한 단위가 나와 묶을 이유가 없었다.

## 검증 계약

> 작성: 2026-07-29 · 보강: 2026-07-30 (REQ-07-12~23, Phase 5) · 근거: 이 계획서 + [api-list.md](../specs/api-list.md) · 검증: `/testrun REQ-07`
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-07-01]` 형태로 박혀 있다.
> **결과 갱신: 2026-07-30 — 23건 전부 `✅`** (`./gradlew test` 전체 통과, CI green @ `efde1f5`).

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:----:|------|:----:|:----:|
| REQ-07-01 | `SecurityConfig.PUBLIC_PATHS` | 정확히 3경로만 허용 | 불변식 | api-list § 공개 경로 — "와일드카드 `/api/v1/auth/**`를 쓰지 않고 **개별 경로로 나열한다**" | 5 | ✅ |
| REQ-07-02 | `SecurityConfig.PUBLIC_PATHS` | `/auth/logout`이 어떤 공개 경로에도 매칭되지 않는다 | 예외 | 제약·함정 — "`/api/v1/auth/**`로 두면 `DELETE /auth/logout`이 무인증 노출된다" | 5 | ✅ |
| REQ-07-03 | `SecurityConfig.PUBLIC_PATHS` | 와일드카드 문자 미사용 | 불변식 | 제약·함정 — "Phase 5에서 반드시 개별 경로로 좁힐 것" | 5 | ✅ |
| REQ-07-04 | `JwtTokenProvider.isAccessToken` | refresh 토큰 → `false` | 회귀 | 제약·함정 — "이 검사를 제거하면 refresh 토큰으로 API 호출이 뚫린다" | 6 | ✅ |
| REQ-07-05 | `JwtTokenProvider.isAccessToken` | access 토큰 → `true` | 정상 | 제약·함정 — "`isAccessToken()`으로 refresh 토큰의 인증 사용을 막고 있다" | 6 | ✅ |
| REQ-07-06 | `JwtTokenProvider.validate` | 만료 토큰 → `false` | 경계 | Phase 6 완료 기준 — "토큰 만료·로테이션·재사용 감지 테스트 통과" | 6 | ✅ |
| REQ-07-07 | `SHA256Util.encrypt` | hex 64자 고정 | 경계 | api-list § refresh 토큰 저장소 — "SHA256Util 해시 (hex 64자 고정)" | 5 | ✅ |
| REQ-07-08 | `RefreshToken.tokenHash` | 컬럼 길이 64 | 회귀 | 제약·함정 — "`refresh_tokens.token_hash`는 `varchar(64)`면 충분하다" | 3 | ✅ |
| REQ-07-09 | `MaskingUtil.maskingCredentialsInBody` | 토큰 응답 본문의 access/refresh 원문 미노출 | 예외 | Phase 4 완료 기준 — "로그에 토큰 원문이 남지 않는다" | 4 | ✅ |
| REQ-07-10 | 〃 | 토큰 교환 form 본문의 `client_secret`·`client_id`·`code` 마스킹 | 예외 | 〃 + 2026-07-29 실측(아래) | 4 | ✅ |
| REQ-07-11 | 〃 | 카카오 오류 응답의 진단 정보는 보존 | 경계 | 제약·함정 — "토큰이 발급됐다면 키 3개는 정상"(IP 오진 방지) | 4 | ✅ |
| REQ-07-12 | `AuthService.refresh` | 제시된 refresh 토큰이 즉시 revoke된다 | 정상 | Phase 5 완료 기준 — "refresh 재발급 시 이전 토큰이 즉시 무효" | 5 | ✅ |
| REQ-07-13 | 〃 | 응답의 refresh 토큰은 제시된 것과 다르다 | 정상 | api-list § 1. Auth — "`access_token` + **새 `refresh_token`** 을 함께 반환한다" | 5 | ✅ |
| REQ-07-14 | 〃 | 응답의 access 토큰은 access 타입이다 | 정상 | 〃 | 5 | ✅ |
| REQ-07-15 | 〃 | 새 refresh 토큰은 해시로 저장된다(원문 미저장) | 불변식 | api-list § refresh 토큰 저장소 — "토큰 원문은 저장하지 않는다" | 5 | ✅ |
| REQ-07-16 | 〃 | revoke된 토큰 재제시 → 해당 사용자 전체 revoke | 예외 | Phase 5 완료 기준 — "revoke된 토큰 재제시 시 해당 사용자 전체 revoke" | 5 | ✅ |
| REQ-07-17 | 〃 | revoke된 토큰 재제시 → `INVALID_TOKEN` | 예외 | api-list § refresh 토큰 저장소 — "`INVALID_TOKEN`(401)을 반환한다" | 5 | ✅ |
| REQ-07-18 | `AuthService.logout` | 해당 사용자의 유효 refresh 전체 revoke | 정상 | api-list § 1. Auth — "access 토큰으로 사용자를 식별해 refresh revoke" | 5 | ✅ |
| REQ-07-19 | `RefreshToken.revoke` | revoke 시 `revoked_at` 설정 | 정상 | api-list § refresh 토큰 저장소 — "`DELETE /auth/logout` → 해당 토큰 `revoked_at` 설정" | 5 | ✅ |
| REQ-07-20 | 〃 | 이미 revoke된 토큰 재revoke 시 최초 시각 유지 | 불변식 | Phase 5 — "`RefreshToken.revoke()`(이미 revoke면 최초 시각 유지)" | 5 | ✅ |
| REQ-07-21 | `AuthService.refresh` | 만료된 refresh 토큰 → `INVALID_TOKEN` | 경계 | Phase 5 결정 (2026-07-30, 아래 미결 해소) | 5 | ✅ |
| REQ-07-22 | 〃 | 저장소에 없는 refresh 토큰 → `INVALID_TOKEN` | 예외 | 〃 | 5 | ✅ |
| REQ-07-23 | 〃 | `BusinessException` 에 롤백하지 않는다(`noRollbackFor`) | 회귀 | 2026-07-30 실측 — 재사용 감지의 전체 revoke 가 예외와 함께 롤백됐다 | 5 | ✅ |

⚠️ **REQ-07-13이 잡은 것은 "다른 토큰인가"가 아니라 실제 결함이었다 (2026-07-30).** JWT `iat`/`exp`는 초 단위라
같은 초에 재발급하면 subject·type 이 같아 **이전 토큰과 바이트 단위로 동일한 문자열**이 나온다. 그러면 새 토큰의 해시가
방금 revoke 한 행과 겹쳐 `uq_refresh_tokens_token_hash` 를 위반하거나, **발급 즉시 revoke 된 토큰을 클라이언트에 주게 된다.**
`createRefreshToken` 에 `jti`(랜덤 UUID)를 넣어 해소했고, **`jti` 를 빼는 프로브로 REQ-07-13 이 실제로 잡는 것을 확인**했다
(프로브 없이는 "그냥 통과하는 케이스"로 보였을 자리다 — CLAUDE.md 구조 규칙 프로브 계약과 같은 이유).

~~**REQ-07-01·02·03은 Phase 5까지 실패한다.**~~ → **2026-07-29 해소.** Phase 4가 `/auth/kakao`·`/auth/refresh`를
실제로 만드는 시점이라 그때 `PUBLIC_PATHS`를 개별 3경로로 좁혔다. 상수 한 줄이고, AGENTS §5·Notion §5·§7이 이미 개별 나열을
계약으로 적고 있어 **코드만 뒤처져 있던** 상태였다. Phase 5에서 `logout`을 붙일 때부터 인증이 맞게 걸린다.

**코드로 쓰지 않은 케이스 (미결)**

| 케이스 | 왜 |
|--------|-----|
| ~~로테이션 — 재발급 시 이전 토큰 즉시 revoke~~ | → **REQ-07-12~15로 승격 (2026-07-30)** |
| ~~재사용 감지 — 전체 revoke + `INVALID_TOKEN`~~ | → **REQ-07-16·17로 승격 (2026-07-30)** |
| 동시 refresh 유예 윈도우 | 미결 질문 — "짧은 유예 윈도우를 둘지 감수할지 정해지지 않았다" |
| 자동가입 왕복 | **유효한 인가코드가 있어야 한다** — 1회용이고 사람이 브라우저로 로그인해야 얻는다. 자동화 대상이 아니라 수동 확인 항목으로 남긴다 |
| ~~만료된 refresh 토큰 제시 시 에러코드~~ | → **REQ-07-21로 승격 (2026-07-30).** Phase 5 구현 시 `INVALID_TOKEN` 으로 결정 |
| ~~저장소에 없는 해시 제시 시 동작~~ | → **REQ-07-22로 승격 (2026-07-30).** 〃 |
| `findByTokenHash` 가 revoke된 행도 반환 | **DB 가 있어야 검증된다.** Testcontainers 는 「범위 — 제외」다. 메서드 이름 규약(`findByTokenHash`)이 필터를 걸지 않는다는 점만으로는 계약이 지켜지는 근거가 못 되고, 파생 쿼리를 `@Query` 로 갈아끼우는 순간 조용히 깨진다 — 로컬 DB 수동 확인 항목 |

**Phase 4에서 결정돼 케이스로 승격된 것** — 마스킹 범위(REQ-07-09~11), `profile_image_url` https 정규화(아래 「결정」).

기대값이 정해지지 않은 것을 지어내면 그 틀린 기대값이 테스트로 굳어 올바른 구현을 막는다.
Phase 5에서 결정이 나오면 그때 케이스로 승격한다.

## 제약·함정

- **Kakao 토큰 응답 본문에 `access_token`이 평문으로 들어온다.** `RestTemplateLoggingInterceptor`는 **헤더만** 마스킹하고 본문은 `log.info`로 그대로 찍는다. Phase 4에서 본문 마스킹을 함께 처리하지 않으면 토큰이 로그에 남는다 (AGENTS §5 위반)
- **스키마를 분리하면 Flyway와 Hibernate 양쪽에 알려야 한다.** 한쪽만 설정하면 테이블은 지정 스키마에 생성되는데 조회는 `public`을 보는(또는 그 반대) 상태가 된다. `V1__init.sql`은 스키마를 명시하지 않아 현재 `search_path`에 의존한다
- **`kakao.client-secret`이 비어 있으면 토큰 교환 요청에서 파라미터 자체를 생략해야 한다.** 빈 값을 실어 보내면 카카오가 거부한다. 콘솔에서 "사용함"으로 켠 경우에만 필요한 값이라 비어 있는 상태가 정상 시나리오다
- **`redirect-uri`는 콘솔 등록값·클라이언트가 인가 요청에 쓴 값과 문자 단위로 같아야 한다.** 서버가 리다이렉트를 받지는 않지만 토큰 교환 요청에 들어간다. 다르면 `KOE006`/`invalid_grant`로 떨어진다
- **Kakao 앱 키는 4종이다 — 서버가 쓰는 것은 REST API 키.** 네이티브·JavaScript 키와 혼동하기 쉽고, **Admin 키는 전권이라 서버에도 두지 않는다**
- ⚠️ **콘솔 「허용 IP 주소」는 `kapi.kakao.com`에만 걸린다.** 실측(2026-07-29): 등록되지 않은 IP에서 토큰 교환(`kauth.kakao.com/oauth/token`)은 **성공**했는데 `kapi.kakao.com/v2/user/me`만 `{"code":-401,"msg":"ip mismatched!"}`로 거부됐다. **키가 틀린 것으로 오진하기 쉽다** — 토큰이 발급됐다면 키 3개는 정상이다. 값이 하나라도 등록돼 있으면 allowlist가 켜진 것으로 동작하므로, **배포 시 서버 egress IP를 등록하거나 이 설정을 비워야 한다.** 고정 egress IP가 없는 실행 환경이면 운영 로그인이 통째로 막힌다
- **이메일은 내려오지 않는다.** 실측(2026-07-29) `kakao_account.email`이 `null`이었다. 이메일은 **비즈니스 앱 전환 + 검수**가 필요한 동의항목이라 현재 앱에서는 받을 수 없다. `users.email`이 NULL 허용이라 자동가입은 막히지 않지만, **Kakao 사용자에게는 `email`이 항상 비어 있다고 보고 설계해야 한다** — 식별자는 `(provider, provider_user_id)` 하나뿐이고 `idx_users_email`은 당분간 빈 인덱스다
- **`profile_image_url`이 `http://` 스킴으로 온다.** 실측(2026-07-29) `http://k.kakaocdn.net/...`. `varchar(500)`이라 저장은 문제없지만 클라이언트가 그대로 쓰면 iOS ATS·Android cleartext 정책에 막힌다. **같은 경로를 `https://`로 요청하면 200이 나오는 것을 확인했으므로**(2026-07-29 실측) 저장 시 스킴을 `https`로 정규화할지 Phase 4에서 정한다
- ⚠️ **`@Transactional` 안에서 예외를 던지면 그 트랜잭션의 쓰기가 전부 사라진다 — "무효화하고 거절하기"는 이 기본값과 정면으로 충돌한다.** 재사용 감지가 정확히 그 모양이라 실제로 revoke 가 롤백됐다(2026-07-30 실측). **거절 응답은 그대로 나가므로 겉보기엔 정상이고, 목 기반 단위 테스트도 통과한다** — 목은 롤백되지 않기 때문이다 → **AGENTS.md §5 계약으로 승격됨 (2026-07-30).** 이 도메인에만 걸리는 함정이 아니다
- ⚠️ **앱은 시간을 UTC 로 저장하고 DB 기본값 `now()` 는 세션 타임존(KST)으로 저장한다.** `application.yml` 의 `hibernate.jdbc.time_zone: UTC` 때문이며 앱끼리는 일관적이다. 다만 **같은 컬럼에 9시간 어긋난 두 종류의 값이 섞일 수 있다** — SQL 로 직접 심은 행(`DEFAULT now()`)과 앱이 쓴 행이 그렇다. 2026-07-30 검증 중 실제로 관찰했다(심은 행 `23:38`, 앱이 쓴 행 `14:39`). 테스트 픽스처를 SQL 로 심을 때는 `now() at time zone 'UTC'` 를 쓸 것
- **`refresh_tokens.token_hash`는 `varchar(64)`면 충분하다.** api-list.md의 제안 스키마는 `varchar(255)`지만 SHA-256 hex는 64자 고정이다. UNIQUE 인덱스가 걸리는 컬럼이다
- **엔티티 ID를 앱 코드가 직접 채우면 `save()`가 merge로 빠진다.** Spring Data JPA가 "ID가 있으니 기존 엔티티"로 판단해 INSERT 전에 SELECT를 한 번 더 날린다. `@GeneratedValue`로 Hibernate에 맡긴다
- **`JwtAuthenticationFilter`는 이미 `isAccessToken()`으로 refresh 토큰의 인증 사용을 막고 있다.** 확인 완료 — 이 검사를 제거하면 refresh 토큰으로 API 호출이 뚫린다
- **API 계약의 1차 출처는 Notion API I/F다.** `docs/specs/api-list.md`는 파생 요약이며 충돌하면 Notion이 이긴다 (AGENTS §0). 2026-07-27에 39개 엔드포인트 기준으로 정합을 맞췄으나, Notion이 바뀌면 이 문서가 아니라 Notion을 먼저 본다
- **`user_social_accounts` 조회/연결/해제 엔드포인트는 존재하지 않는다.** `api-list.md`가 독자 추가했던 것을 제거했다. 그런데 이것이 "공개 경로 범위" 결정(2026-07-23)에서 "인증 필요한 기능을 `/auth/` 밖에 둔 사례"로 인용됐던 엔드포인트다 — 근거가 사라졌으므로 위 미결 1번을 반드시 확인해야 한다
- **`SecurityConfig.PUBLIC_PATHS`에 와일드카드를 쓰지 않는다.** `/api/v1/auth/**`로 두면 `DELETE /auth/logout`이 무인증 노출된다 — 이 엔드포인트는 Request Body가 없어 access 토큰이 유일한 사용자 식별 수단이다. Phase 5에서 반드시 개별 경로로 좁힐 것 (AGENTS §5 계약)
- **`ConditionTag` enum은 7개다.** `V1__init.sql` 주석이 4개로 빠져 있으니 함께 고칠 것 (REQ-10 범위)
- **`SecurityConfig.PUBLIC_PATHS`를 넓히지 않는다.** `/api/v1/auth/**`는 permitAll이라 인증이 필요한 기능을 그 아래 두면 무인증 노출된다 *(AGENTS §5에 이미 계약으로 등재됨)*
