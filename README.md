# PetKok API (petkok-api)

반려동물 다이어리 백엔드. **Spring Boot 3.x / Java 21** 단일 스택 (ADR-001).
대상: 크레스티드 게코 / 강아지 / 고양이. 게코 특화 로직이 핵심 차별점.

> 이 저장소는 **개발 1단계 = 뼈대(skeleton)** 상태입니다.
> global 공통 계층 + 베이스 엔티티 + Flyway 초기 스키마까지 포함하며,
> 도메인(auth/user/pet/...) 구현은 다음 단계에서 추가됩니다.

## 기술 스택
- Java 21, Spring Boot 3.3.x (Gradle Kotlin DSL)
- Spring Data JPA (Hibernate 6), Spring Security, Bean Validation
- PostgreSQL (Supabase) + **Flyway** (스키마 단일 주체)
- JWT (Access/Refresh) + Kakao OAuth2 (커스텀 플로우)
- Cloudflare R2 (S3 호환, presigned 업로드)

## 요구사항
- JDK 21
- **PostgreSQL 17** — 운영(Supabase)이 17.x 이므로 로컬도 메이저를 맞춥니다.
  마이너까지 같을 필요는 없지만 **메이저가 다르면 로컬에서만 통과하는 마이그레이션이 나올 수 있습니다.**

## 실행
> `gradle-wrapper.jar`(8.10.2) 는 저장소에 포함되어 있습니다. 별도 준비 없이 `./gradlew` 를 바로 쓰세요.

### 1) 로컬 DB 준비
```sql
-- 슈퍼유저로 1회
CREATE ROLE root LOGIN PASSWORD '<로컬용 비밀번호>';
CREATE DATABASE petkok OWNER root;
```
**스키마는 만들지 않아도 됩니다.** Flyway 가 `spring.flyway.schemas` 에 적힌 스키마를 없으면 만들고, DB 소유자인 `root` 는 `CREATE` 권한을 갖습니다.

| 프로파일 | 스키마 |
| --- | --- |
| `local` | `petkok_local` |
| `dev` | `petkok_dev` |
| `prod` | `petkok_prod` |

로컬까지 이름 있는 스키마를 쓰는 이유는 **스키마 지정 경로를 매일 실행시키기 위해서**입니다. 로컬만 `public` 으로 두면 그 설정이 로컬에서 한 번도 검증되지 않고 dev 배포에서 처음 갈립니다. 값의 출처는 `application-{profile}.yml` 의 `db.schema` 한 곳이고, Flyway 와 Hibernate 가 **둘 다** 이 값을 참조합니다 — 한쪽만 지정하면 테이블이 생긴 곳과 조회하는 곳이 달라져 **에러 없이 "테이블이 없다"** 로 나타납니다.

> ⚠️ **포트를 확인하세요.** Postgres.app 처럼 여러 메이저 버전을 병행 설치하면 버전마다 포트가 달라집니다(예: 17 → `5433`). 포트를 잘못 짚으면 **다른 버전의 빈 DB 에 조용히 붙어** "테이블이 없다" 로 오인하기 쉽습니다.
> ```sql
> SHOW port; SELECT version();
> ```

인증을 `trust` 로 두면 비밀번호가 검증되지 않습니다. 운영과 같은 방식으로 맞추려면 `pg_hba.conf` 의 **`host` 라인**을 `scram-sha-256` 으로 바꾸고 `SELECT pg_reload_conf();` 하세요. `local`(유닉스 소켓) 라인까지 바꾸면 비밀번호 없는 관리자 롤이 잠기니 주의합니다.

### 2) 환경변수
`cp .env.example .env` 후 값을 채웁니다. `.env` 는 `.gitignore` 로 제외됩니다 — **이 저장소는 public 이므로 실제 값은 절대 커밋하지 마세요.**

로컬은 `application-local.yml` 이 접속 URL(`localhost:5433/petkok`)과 계정(`root`)을 기본값으로 갖고 있어 **`DB_PASSWORD` 하나만** 채우면 뜹니다. 비밀번호에는 기본값을 두지 않았습니다 — 더미 값을 넣어 두면 scram 인증 실패가 "비밀번호 틀림" 으로만 보여 원인 파악이 늦어집니다.

필수(운영): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`(32바이트+), 그리고 gallery 사용 시 `R2_*`. 스키마는 프로파일이 정하므로 `DB_SCHEMA` 는 기본값을 덮어쓸 때만 씁니다.

> ⚠️ `.env` 에서 `KEY=` (빈 값)은 "미설정" 이 아닙니다. 빈 문자열이 환경변수로 들어가 `${VAR:기본값}` 의 **기본값이 적용되지 않습니다**. 기본값을 쓰려면 그 줄을 주석 처리하세요.

> Spring 이 `.env` 를 자동으로 읽지는 않습니다. 셸에서 주입하거나 IDE Run Configuration 에 지정하세요.
> ```bash
> set -a && . ./.env && set +a && ./gradlew bootRun
> ```

### 3) 기동
```bash
./gradlew bootRun            # 기본 프로파일 local
# 또는 프로파일 지정
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
기동 시 Flyway 가 `petkok_local` 스키마(없으면 생성)에 `V1__init.sql` 로 9개 테이블을 만듭니다.
확인: `curl localhost:8080/actuator/health` → `{"status":"UP"}`

## 패키지 구조
`business` / `data` / `framework` 3분할. 각 트리 안은 **도메인 단위**로 묶습니다(2026-07-28 확정, 상세는 [AGENTS.md §3](AGENTS.md)).

```
com.petkok
├── business/                   도메인별 진입 + 비즈니스 로직
│   └── {도메인}/               controller/ · service/(+ *Calculator)
├── data/                       영속 객체 + 전송 객체
│   ├── common/entity/          BaseCreatedEntity → BaseTimeEntity → BaseSoftDeleteEntity
│   └── {도메인}/               entity/ · repository/ · dto/ · enums/
└── framework/                  도메인 무관 횡단 관심사
    ├── config/                 Security, JpaAuditing, Jackson, Web, R2(+Properties), RestTemplate
    ├── processor/              filter/ · handler/ · interceptor/ · aspect/ · converter/
    ├── security/               AuthPrincipal, @CurrentUser, jwt/
    ├── response/               ApiResponse{data,error}, ErrorResponse
    ├── pagination/             CursorRequest, CursorPage, CursorCodec
    ├── exception/              ErrorCode, BusinessException
    ├── constant/               전역 상수 · ApiUri
    └── util/                   spring-java-utility 이식 30개
```

도메인 10개: auth · user · pet · diary · feeding · activity · weight · shed · gallery · timeline
(현재는 뼈대만 있어 `business/`·`data/{도메인}` 은 아직 비어 있고 `data/common/entity` 만 존재합니다.)

> **`business/{도메인}` 과 `data/{도메인}` 은 반드시 같은 이름을 씁니다.** ArchUnit Slices 가 이 이름을 슬라이스 키로 삼아 도메인 간 참조를 막습니다 — 이름이 어긋나면 규칙이 **에러 없이 조용히 무력화**됩니다.

## 핵심 컨벤션
- **레이어**: Controller → Service → Repository → Entity/DTO (단방향). Entity 는 Service 밖으로 안 나감.
- **응답**: `{data, error}` 래퍼, 전역 snake_case (Jackson).
- **스키마 소유 = Flyway.** Supabase 대시보드 수동 DDL 금지 (drift 방지).
- **updated_at = JPA Auditing** (`@LastModifiedDate`). DB 트리거 없음. created_at = `@CreatedDate`.
- **소프트 딜리트**: users, pets 만 `deleted_at`.
- **Enum**: Java Enum + `@Enumerated(STRING)`, DB 는 varchar (CHECK 없음).
- **페이지네이션**: 커서 기반 (opaque base64 `next_cursor`).

## 베이스 엔티티 매핑 가이드
| 대상 테이블 | 상속 베이스 |
| --- | --- |
| users, pets | `BaseSoftDeleteEntity` (created/updated/deleted) |
| diary_entries | `BaseTimeEntity` (created/updated) |
| user_social_accounts, feeding_logs, activity_logs, weight_logs, shed_records, photos | `BaseCreatedEntity` (created) |

## 다음 단계
1. auth → user → pet 수직 슬라이스 (PetAccessGuard 소유권 앵커)
2. 기록 도메인 확장 (diary/feeding/activity/weight/shed/gallery/timeline)
3. 파생 로직 순수 클래스 (AnorexiaStreakCalculator, ShedPredictionCalculator)
