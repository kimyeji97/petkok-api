# 작업계획서 — MySQL 전환 + auth 도메인 (REQ-07)

작성일: 2026-07-27 · 상태: **초안 (착수 전 검토 필요)**

## 0. 배경

DB 엔진을 **PostgreSQL(Supabase) → MySQL/MariaDB**로 교체하기로 했다. 이 결정이 auth 구현의 선행 조건이 된다 — 현재 `V1__init.sql`은 PostgreSQL 전용 문법이라 MySQL에서 **기동조차 되지 않는다.**

접속 정보:

| 환경 | 상태 | 비고 |
| --- | --- | --- |
| local | `localhost:3306` / `root` / DB명 `petkok` | 비밀번호는 레포에 커밋하지 않는다 (§A-5) |
| dev / prod | **인프라 미구성** | DB(=스키마) 단위로 분리 예정 |

### 착수 전 확인이 필요한 것

**로컬 DB의 정확한 제품·버전** (`select version(), @@version_comment;`). 이 환경엔 `mysql` 클라이언트가 없어 확인하지 못했다. 아래 셋이 버전에 따라 갈리고, 전부 스키마 설계에 영향을 준다.

| 항목 | MySQL 8.0+ | MariaDB | MySQL 5.7 |
| --- | --- | --- | --- |
| 내림차순 인덱스 (`... desc`) | 실제 반영 | **파싱만 하고 무시**(ASC로 생성) | 무시 |
| 네이티브 `UUID` 타입 | 없음 | 10.7+ 있음 | 없음 |
| 기본 collation | `utf8mb4_0900_ai_ci` | `utf8mb4_general_ci` 계열 | `latin1` 주의 |

커서 페이지네이션이 전부 `desc` 정렬이라 첫 행은 그냥 넘길 수 없다.

---

# Phase A — 엔진 전환

auth의 선행 조건. **Phase B와 PR을 분리한다** — 스키마 전면 교체라 diff가 크고, 도메인 코드와 섞이면 리뷰가 불가능해진다.

## A-1. `V1__init.sql` 재작성 (V2 추가가 아니라 V1 자체 교체)

**왜 변환 마이그레이션을 쌓지 않는가**: dev/prod 인프라가 아직 없고 로컬 하나뿐이다. 지켜야 할 기존 데이터가 없는데 PostgreSQL→MySQL 변환 이력을 영구히 들고 갈 이유가 없다.

> ⚠️ **함정**: 로컬 DB에 V1이 이미 적용돼 있으면 Flyway 체크섬 검증에서 기동이 막힌다. `drop database petkok; create database petkok ...` 후 재기동한다(로컬 데이터는 버린다). `flyway repair`는 이력만 고치고 실제 테이블은 PostgreSQL 시절 그대로 남으므로 여기선 쓰면 안 된다.

### 전환 매핑

| 항목 | 현재 (PostgreSQL) | 전환 후 (MySQL) |
| --- | --- | --- |
| PK 타입 | `uuid` + `default gen_random_uuid()` | `binary(16)`, DB 기본값 제거 (§A-2) |
| 타임스탬프 | `timestamp` | **`datetime(6)`** — `TIMESTAMP` 금지 |
| `created_at`/`updated_at` 기본값 | `default now()` | 제거 (JPA Auditing 소유) |
| FK | 컬럼 인라인 `references` | 테이블 레벨 `foreign key` |
| 부분 인덱스 7개 | `create index ... where ...` | 미지원 → 재설계 (§A-3) |
| `boolean` | `boolean` | `tinyint(1)` (Hibernate 기본 매핑) |
| `text` / `decimal(8,2)` / `date` / `int` | — | 동일, 변경 없음 |
| charset | (해당 없음) | `utf8mb4` 명시 — 닉네임·메모 이모지 |

### 이 Phase의 핵심 함정 3가지

**① 인라인 `references`를 MySQL은 조용히 무시한다.**
V1의 모든 FK가 `user_id uuid not null references users (id)` 형태다. InnoDB는 **컬럼 정의 안의 `REFERENCES` 절을 파싱만 하고 제약을 만들지 않는다.** 문법 오류도 경고도 없다. 그대로 옮기면 FK가 하나도 없는 스키마가 만들어지고, 고아 레코드가 생겨야 비로소 알게 된다. 반드시 테이블 레벨로 바꾼다:

```sql
constraint fk_pets_user foreign key (user_id) references users (id)
```

**② `TIMESTAMP`가 아니라 `DATETIME(6)`.** 이유 세 가지:
- `TIMESTAMP`는 **2038-01-19 상한**이 있다
- `explicit_defaults_for_timestamp=OFF`면 테이블의 **첫 TIMESTAMP 컬럼에 `ON UPDATE CURRENT_TIMESTAMP`가 자동으로 붙는다.** AGENTS §5의 "`updated_at`은 JPA Auditing이 관리, DB 트리거 없음" 계약이 아무도 모르게 깨진다
- 기본 정밀도가 초 단위다

`datetime(6)`은 셋 다 해당 없다. 다만 `DATETIME`은 TZ 변환을 하지 않으므로, 이미 설정된 `hibernate.jdbc.time_zone: UTC`와 커넥션 TZ를 일관되게 맞춰야 한다(§A-4).

**③ 부분 인덱스가 없다.** MySQL·MariaDB 모두 미지원. 7개를 §A-3에서 재설계한다.

## A-2. PK 전략 — `binary(16)` + 시간 정렬 UUID

**타입**: `char(36)`(가독성) vs `binary(16)`(16 vs 36바이트). InnoDB는 PK가 **모든 세컨더리 인덱스에 복제**되므로 차이가 테이블 수만큼 누적된다 → **`binary(16)` 권장.** Hibernate 6이 MySQL에서 `UUID`를 `binary(16)`으로 매핑하므로 `ddl-auto: validate`와도 맞는다.
- 대가: 콘솔에서 raw SQL을 볼 때 `BIN_TO_UUID(id)`가 필요하다. 디버깅 편의를 우선한다면 `char(36)`도 방어 가능한 선택 — 다만 되돌리기 비싸므로 지금 정한다.

**생성 방식**: DB 기본값(`gen_random_uuid()`)을 제거하고 앱에서 생성한다.

> ⚠️ **랜덤 UUIDv4를 PK로 쓰면 InnoDB 삽입 성능이 떨어진다.** PK가 클러스터드 인덱스라, 무작위 키가 계속 들어오면 페이지 분할이 반복된다. **시간 정렬 UUID**(UUIDv7, 또는 Hibernate `@UuidGenerator(style = TIME)`)로 발급한다. 단 Hibernate의 `TIME` 스타일이 실제로 어떤 바이트 순서를 내는지는 버전마다 다르므로, **구현 시 생성값을 직접 찍어 시간 정렬인지 확인한다.**

> ⚠️ **`@GeneratedValue`로 Hibernate에 맡긴다.** ID를 앱 코드가 직접 채운 뒤 `save()`를 부르면 Spring Data JPA가 "ID가 있으니 기존 엔티티"로 판단해 `merge`로 보내고, INSERT 전에 SELECT가 한 번 더 나간다. 수동 할당이 꼭 필요해지면 `Persistable<UUID>.isNew()` 구현이 함께 와야 한다.

## A-3. 부분 인덱스 7개 대체

| 원본 인덱스 | 조건절 | MySQL 대체 |
| --- | --- | --- |
| `idx_users_email` | `deleted_at is null` | `(email)` — 탈퇴자가 소수라 필터 효과 미미 |
| `idx_users_deleted_at` | `deleted_at is not null` | **삭제 권장** — NULL이 대부분이라 인덱스만 커진다. 탈퇴자 조회 쿼리가 아직 없다 |
| `idx_pets_user_id` | `deleted_at is null` | `(user_id, deleted_at)` — 목록 조회가 `user_id=? and deleted_at is null` |
| `idx_pets_deleted_at` | `deleted_at is not null` | **삭제 권장** (위와 동일) |
| `idx_diary_pet_condition` | `condition_tag is not null` | `(pet_id, condition_tag, entry_date)` — WHERE만 제거 |
| `idx_feeding_pet_refused` | `is_refused = true` | `(pet_id, is_refused, fed_at)` — 필터 컬럼을 선행으로 재배치 |
| `idx_photos_diary_entry_id` | `diary_entry_id is not null` | `(diary_entry_id)` |

`desc` 지정은 §0의 버전 확인 결과에 따라 유지/제거를 결정한다.

## A-4. 빌드·설정 교체

- `runtimeOnly("org.postgresql:postgresql")` → `com.mysql:mysql-connector-j` (MariaDB면 `org.mariadb.jdbc:mariadb-java-client`)
- `implementation("org.flywaydb:flyway-database-postgresql")` → `flyway-mysql`
- `application.yml`: `driver-class-name` 교체
- JDBC URL: `jdbc:mysql://localhost:3306/petkok?...` — TZ·charset 파라미터는 드라이버 버전에 맞춰 확정하고, §A-1 ②의 UTC 일관성을 검증한다
- `ddl-auto: validate`는 유지. 전환 후 첫 기동에서 엔티티 매핑과 DDL이 어긋나면 여기서 잡힌다 — **이게 Phase A의 완료 판정 수단이다**

## A-5. 환경 분리와 시크릿

MySQL은 **schema == database**다. dev/prod는 같은 서버의 별도 데이터베이스로 나눈다.

| 환경 | DB명 | 계정 |
| --- | --- | --- |
| local | `petkok` | `root` (로컬 한정) |
| dev | `petkok_dev` | 전용 계정 — 해당 DB 권한만 |
| prod | `petkok_prod` | 전용 계정 — **`root` 금지** |

- Flyway 이력 테이블(`flyway_schema_history`)이 DB마다 독립적으로 생기므로 환경 간 간섭이 없다
- 분리는 URL 레벨로 처리한다(프로파일별 `DB_URL`). 애플리케이션 코드는 환경을 알 필요가 없다

> ⚠️ **시크릿**: 로컬 비밀번호를 `application-local.yml`에 기본값으로 박으면 레포에 커밋된다(현재 파일이 `postgres/postgres`를 그렇게 두고 있다). `${DB_PASSWORD:...}` 자리표시자만 남기고 실제 값은 환경변수 또는 gitignore된 로컬 override로 주입한다. **`.gitignore`에 해당 패턴이 아직 없다 — 함께 추가해야 한다.**
> 비밀번호에 `@`가 포함돼 있어, JDBC URL에 인라인으로 넣는 방식을 쓰면 인코딩이 필요하다. 지금처럼 별도 property로 두면 문제없다.

## A-6. 문서 정리

PostgreSQL·Supabase 전제가 박혀 있는 곳을 모두 수정한다: README(스택·요구사항·기동 설명), AGENTS §1, `docs/specs/api-list.md`의 `refresh_tokens` 제안 스키마(현재 PostgreSQL 문법).

**ADR-002 신설을 제안한다.** 엔진 교체는 되돌리기 비싼 결정이고 README의 ADR-001(단일 스택)과 맞물린다. AGENTS §7이 아키텍처 결정의 ADR 기록을 권장하는데 `docs/adr/`는 아직 비어 있다 — 첫 문서로 적합하다.

---

# Phase B — auth 도메인 (REQ-07)

## B-1. `V2__refresh_tokens.sql`

`api-list.md`의 제안 스키마를 MySQL 문법으로 옮기고 Phase A 규칙(binary(16) / datetime(6) / 테이블 레벨 FK)을 적용한다. 두 군데가 원안과 달라진다:

- `create index ... where revoked_at is null` → 부분 인덱스 불가 → **`(user_id, revoked_at)`**
- `token_hash varchar(255)` → **`varchar(64)`**. SHA-256 hex는 64자 고정이다. UNIQUE 인덱스가 걸리는 컬럼이라 utf8mb4에서 폭이 4배로 계산되므로 줄이는 편이 낫다

## B-2. 엔티티

`User`(`BaseSoftDeleteEntity`) · `UserSocialAccount`(`BaseCreatedEntity`) · `RefreshToken`(`BaseCreatedEntity` — 무효화는 `revoked_at`이고 소프트 딜리트가 아니다). Entity는 Service 밖으로 나가지 않는다(AGENTS §5).

## B-3. Kakao 연동

REQ-05에서 만든 `RestTemplate` + 로깅 인터셉터를 그대로 쓴다. 흐름: 인가코드 → 토큰 교환 → 사용자 정보 조회 → `(provider, provider_user_id)`로 조회, 없으면 자동가입.

> ⚠️ **응답 본문 마스킹을 여기서 처리해야 한다.** 현재 인터셉터는 **헤더만** 마스킹한다. 카카오 토큰 응답 **본문**에 `access_token`·`refresh_token`이 그대로 담겨 오고 `log.info`로 찍힌다. REQ-05 때 "민감 필드가 연동 대상마다 다르니 실제 응답을 보고 결정"으로 미뤄둔 항목이 바로 이 작업이다.

연동 실패는 `ErrorCode.EXTERNAL_API_ERROR`(502) — REQ-05에서 추가만 해두고 호출부가 없던 코드다.

## B-4. 토큰 발급·로테이션

`JwtTokenProvider`는 이미 구현되어 있다(`createAccessToken`/`createRefreshToken`, subject=userId, `type` claim). 그대로 재사용한다.

- refresh 원문은 저장하지 않고 `SHA256Util` 해시로 보관
- `POST /auth/refresh`: 검증 → 기존 토큰 revoke → 신규 access+refresh 동시 발급·반환
- **재사용 감지**: `revoked_at`이 찍힌 토큰이 제시되면 해당 사용자 전체 revoke 후 `INVALID_TOKEN`(401). 전용 ErrorCode는 만들지 않는다 *(결정 완료 — 2026-07-23)*

> ⚠️ **미결정 — 동시 refresh 요청.** 모바일 클라이언트가 앱 재시작이나 병렬 요청으로 같은 refresh를 거의 동시에 두 번 보내면, **정상 사용자인데도 재사용 감지에 걸려 전 기기 로그아웃**된다. 짧은 유예 윈도우(직전 revoke 토큰 N초 허용)를 둘지, 감수할지 결정이 필요하다. 구현 착수 전에 정한다.

## B-5. SecurityConfig / 필터

확인 결과 **변경할 것이 거의 없다.**
- `PUBLIC_PATHS`는 `/api/v1/auth/**` + `/actuator/health` 현행 유지 *(결정 완료 — 2026-07-23)*
- `JwtAuthenticationFilter`가 이미 `isAccessToken()`으로 refresh 토큰의 인증 사용을 막고 있다

## B-6. 검증 체계 도입 — 지금이 적기

- **`src/test` 신설.** 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐이다. auth는 토큰 만료·로테이션·재사용 감지처럼 **수동 확인이 어렵고 조용히 깨지는** 로직이라, 여기서 테스트 없이 진행하면 회귀를 감지할 수단이 없다
- **ArchUnit 활성화.** AGENTS §6이 "feature 도메인 도입 시점까지 보류"라고 명시했고, 그 시점이 지금이다
- **Testcontainers(MySQL) 도입 검토.** 로컬 DB 상태에 의존하지 않고 마이그레이션·리포지토리를 검증할 수 있다. Phase A에서 갈아엎은 스키마의 회귀 방지에도 쓰인다

---

# 순서와 완료 기준

| # | 범위 | 완료 기준 |
| --- | --- | --- |
| 1 | A-1 ~ A-4 | `bootRun`으로 V1 마이그레이션 적용 + `ddl-auto: validate` 통과 |
| 2 | A-5 ~ A-6 | 시크릿이 레포에 없음 · 문서에 PostgreSQL 전제 잔존 없음 · ADR-002 |
| 3 | B-1 ~ B-2 | V2 적용 + 엔티티 validate 통과 |
| 4 | B-3 ~ B-5 | 카카오 로그인 → refresh → 로그아웃 왕복 동작 |
| 5 | B-6 | 테스트 통과 + ArchUnit 통과 + CI green |

1~2와 3~5는 **PR을 나눈다.**

# 미결정 항목

1. **로컬 DB 제품·버전** (§0) — 내림차순 인덱스·UUID 타입·collation이 여기 달렸다
2. **PK 타입** `binary(16)` vs `char(36)` (§A-2) — 되돌리기 비싸다
3. **동시 refresh 요청 처리** (§B-4)
4. **dev/prod 인프라 형태** (관리형 vs 자체 호스팅) — §A-5의 계정·권한 설계가 의존
5. **Testcontainers 도입 여부** (§B-6)
