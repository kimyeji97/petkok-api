# AGENTS.md

이 파일은 AI 에이전트(Claude, Copilot 등)를 위한 프로젝트 진입점이다.

## 0. 출처 우선순위 (먼저 읽을 것)

**요구사항·설계·API 계약·ADR의 원본은 이 저장소가 아니라 Notion에 있다.**
<https://app.notion.com/p/yjkim97/PetKok-389b81b56e6080f6bfc2f7972108e778>

| 대상 | 1차 출처 |
| --- | --- |
| 요구사항, 유저 스토리, 비즈니스 규칙, 제약사항 | **Notion** → 기획/분석 |
| ADR (ADR-001 스택 선택, ADR-002 DB 엔진 선택) | **Notion** → 기획/분석. `docs/adr/`는 비어 있다 |
| 테이블 정의서(DDL), ERD | **Notion** → 설계 → DB 탭. 파생 요약은 [`docs/specs/db-schema.md`](docs/specs/db-schema.md) |
| **API 계약 (엔드포인트·메서드·인증 여부)** | **Notion** → 설계 → API 탭 → `API I/F` DB |
| 소스 구조·레이어 규칙·파생 로직 설계 | **Notion** → 설계 → 소스 구조/아키텍처 설계 |
| 스택·패키지 구조·로컬 실행 | [`README.md`](README.md) |
| 코드 컨벤션·CI 게이트 | 이 문서 (§5·§6) |
| 진행 이력·판단 근거 | [`docs/PROGRESS.md`](docs/PROGRESS.md) |

- `docs/specs/api-list.md`(API I/F)와 `docs/specs/db-schema.md`(테이블 정의서)는 **Notion의 파생 요약**이다. 원본이 아니다. **충돌하면 언제나 Notion이 이긴다**
- 설계 관련 판단이 필요하면 레포 문서만 보고 결정하지 말 것. 실제로 레포 문서만 보고 진행했다가 이미 Accepted 상태이던 ADR-002(DB 엔진)와 어긋난 계획을 세운 적이 있다
- 반대로 레포에서 확정한 내용이 Notion보다 앞서는 경우도 있다(예: refresh 토큰 저장소). 그때는 **Notion에 역반영을 제안**하고, 어느 쪽이 최신인지 날짜로 판단한다
- Notion DDL 블록은 소스 구조 문서보다 갱신이 늦는다. 둘이 다르면 소스 구조 문서와 `V1__init.sql`을 신뢰하고 확인을 요청한다

---

## 1. 프로젝트 개요

**PetKok API** — 반려동물(크레스티드 게코 / 강아지 / 고양이) 다이어리 백엔드. 게코 특화 로직이 핵심 차별점.
현재 **개발 1단계 = 뼈대(skeleton)** 상태: 공통 계층 + 베이스 엔티티 + Flyway 초기 스키마까지. 도메인(auth/user/pet/…)은 다음 단계.
2026-07-28에 패키지 구조를 `business`/`data`/`framework` 3분할로 재확정하고 이행까지 끝냈다(§3). `com.petkok.global.*`은 더 이상 없다.

**스택**: Java 21 · Spring Boot 3.3.x (Gradle **Kotlin DSL**) · Spring Data JPA(Hibernate 6) · Spring Security · Bean Validation · **PostgreSQL(Supabase) + Flyway** · JWT(Access/Refresh) + Kakao OAuth2 · Cloudflare R2(S3 호환).

---

## 2. 주요 명령어

```bash
./gradlew bootRun            # 기동 (기본 프로파일 local)
./gradlew build -x test      # 빌드 (테스트 제외)
./gradlew test               # 테스트
./gradlew spotlessApply      # 포맷 적용 (google-java-format)
./gradlew spotlessCheck      # 포맷 검증 (CI)
./gradlew checkstyleMain checkstyleTest -PciStrict   # 정적 분석 (CI 게이트)
./gradlew jacocoTestReport   # 커버리지 리포트
```

> `gradle-wrapper.jar`는 저장소에 포함되어 있다. 기동 시 Flyway가 `V1__init.sql`로 초기 테이블을 생성한다.

---

## 3. 패키지 구조

**2026-07-28 확정 — `business` / `data` / `framework` 3분할.** 각 트리 안은 반드시 도메인 단위로 묶는다.

```
com.petkok
├── business/                   도메인별 진입 + 비즈니스 로직
│   └── {도메인}/
│       ├── controller/         XxxController
│       └── service/            XxxService, *Calculator(I/O 없는 순수 계산)
├── data/                       영속 객체 + 전송 객체
│   ├── common/entity/          BaseCreatedEntity → BaseTimeEntity → BaseSoftDeleteEntity
│   └── {도메인}/
│       ├── entity/             JPA 엔티티
│       ├── repository/         JpaRepository + 커스텀 쿼리(keyset, 소프트딜리트 필터)
│       ├── dto/                XxxRequest / XxxResponse (record)
│       └── enums/              도메인 전용 enum
└── framework/                  도메인 무관 횡단 관심사
    ├── config/                 Security, JpaAuditing, Jackson, Web, R2(+R2Properties), RestTemplate
    ├── processor/              filter/(JwtAuthenticationFilter) · handler/(GlobalExceptionHandler)
    │                           · interceptor/(RestTemplateLoggingInterceptor) · aspect/ · converter/
    ├── security/               AuthPrincipal, @CurrentUser, jwt/(JwtTokenProvider, JwtProperties)
    ├── response/               ApiResponse{data,error}, ErrorResponse
    ├── pagination/             CursorRequest, CursorPage, CursorCodec
    ├── exception/              ErrorCode, BusinessException
    ├── constant/               전역 상수 · ApiUri
    └── util/                   spring-java-utility 이식 30개
                                (date/encrypt/file/http/json/list/map/number/os/spring/string + 루트 4종)
```

도메인 10개: auth · user · pet · diary · feeding · activity · weight · shed · gallery · timeline

- **`business/{도메인}`과 `data/{도메인}`은 같은 이름을 쓴다.** ArchUnit Slices가 이 이름을 슬라이스 키로 삼아 도메인 간 참조를 막는다 — 이름이 어긋나면 규칙이 무력해진다
- **DTO 패키지 이름은 `dto`.** `domain`으로 부르지 않는다 (DDD의 도메인 모델과 혼동)
- **repository는 entity 옆(`data/{도메인}/repository`).** 반환 타입이 Entity라 결합이 강하고, `business → data` 단방향을 깨지 않는다
- `timeline`은 자체 테이블이 없어 `business/timeline`만 둔다. 여러 도메인 Repository를 조합하므로 도메인 간 참조 금지 규칙의 **유일한 예외**다
- `@CurrentUser`는 `@AuthenticationPrincipal` 메타 애노테이션이라 별도 ArgumentResolver가 없다 (`processor/resolver/`를 두지 않는 이유)
- 베이스 엔티티는 `framework`가 아니라 `data/common/entity`. framework는 JPA 매핑 규약을 알지 않는다

> ✅ **이행 완료 (2026-07-28).** 55개 파일을 위 구조로 옮겼고 `com.petkok.global.*`은 남아 있지 않다. `business/`·`data/{도메인}`은 도메인 코드가 들어올 때 생성된다(현재는 `data/common/entity`만 존재).
>
> 이행 중 자리를 바꾼 3개 — 이유가 없으면 원위치로 되돌리기 쉬우므로 근거를 남긴다.
>
> | 클래스 | 이동 | 이유 |
> |---|---|---|
> | `RestTemplateLoggingInterceptor` | `config/` → `processor/interceptor/` | 설정이 아니라 호출 파이프라인에 끼어드는 컴포넌트 |
> | `GlobalExceptionHandler` | `exception/` → `processor/handler/` | 예외 **정의**가 아니라 응답 변환 처리기 |
> | `JwtAuthenticationFilter` | `security/jwt/` → `processor/filter/` | 요청 파이프라인 위치가 성격을 결정. 토큰 발급·검증(`JwtTokenProvider`)은 `security/`에 남는다 |

---

## 4. 워크플로우

- **브랜치**: GitHub Flow. `main` + feature 브랜치. 네이밍: `feat/kebab-case`, `fix/…`, `chore/…` 등
- **커밋**: Conventional Commits. 타입은 영어(`feat` `fix` `refactor` …), 제목·본문은 한글. lefthook `commit-msg`가 형식 검증(위반 시 차단)
- **PR**: PR 필수, 템플릿([`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md)) 작성. CI 통과가 머지 조건
- ⚠️ **머지 직전에 PR head SHA와 로컬 HEAD를 반드시 대조한다.** GitHub이 푸시를 PR에 반영하지 못하는 경우가 실제로 있었다 — 원격 브랜치와 `GET /branches/…`는 새 SHA인데 `GET /pulls/{n}`만 낡은 SHA를 5분 넘게 반환했고, 그대로 머지되어 **커밋 3건이 `main`에 누락**됐다(2026-07-27→28, PR #8 → #9로 복구)
- ⚠️ **CI 초록불은 SHA와 함께 확인한다.** `gh pr checks`는 이전 실행분 결과를 그대로 보여줄 수 있다. `gh run list --branch <브랜치> --json headSha,conclusion`으로 **어느 커밋 기준인지** 확인할 것 — 위 사고에서 통과로 읽은 체크는 전날 실행분이었다

---

## 5. 코드 컨벤션 (핵심)

- **레이어**: Controller → Service → Repository → Entity/DTO (단방향). **Entity는 Service 밖으로 나가지 않는다** (응답은 DTO)
- **트리 방향**: `business → data` 단방향. `framework`는 `business`·`data`를 **참조하지 않는다**(역참조 금지). 다른 도메인 참조 금지 — 예외는 `business/timeline` 하나뿐
- **응답**: 공통 wrapper `ApiResponse<T>` (`{data, error}`). 전역 snake_case (Jackson). 에러는 `BusinessException` + `ErrorCode` enum → `GlobalExceptionHandler` 전역 처리
- **스키마 소유 = Flyway.** Supabase 대시보드 수동 DDL 금지 (drift 방지)
- **감사(auditing)**: `created_at = @CreatedDate`, `updated_at = @LastModifiedDate` (JPA Auditing, DB 트리거 없음). 베이스 엔티티 상속으로 처리
- **소프트 딜리트**: `users`, `pets`만 `deleted_at` (`BaseSoftDeleteEntity`)
- **Enum**: Java Enum + `@Enumerated(STRING)`, DB는 varchar
- **페이지네이션**: 커서 기반 (opaque base64 `next_cursor`)
- **수정 메서드**: 리소스 수정은 `PATCH`(부분 수정)로 통일. `PUT`(전체 교체)은 쓰지 않는다 — 누락 필드와 `null` 의도를 구분할 수 없기 때문
- **공개 경로**: `SecurityConfig.PUBLIC_PATHS`에는 **access 토큰 없이 호출되는 엔드포인트만 개별 경로로** 나열한다. **`/api/v1/auth/**` 같은 와일드카드를 쓰지 않는다** — `/auth/` 아래에도 인증이 필요한 엔드포인트가 있어(`DELETE /auth/logout`) 무인증 노출된다. 현재 공개 대상: `/api/v1/auth/kakao`, `/api/v1/auth/refresh`, `/actuator/health`
- **네이밍/상수**: 클래스 UpperCamelCase, 상수 `UPPER_SNAKE_CASE`, DTO는 `XxxRequest`/`XxxResponse`
- **로깅**: Lombok `@Slf4j` (필드 `log`). 민감정보(전화번호·토큰 등)는 마스킹
- **외부 API 응답 버퍼링**: 로깅 인터셉터는 응답을 `BufferingClientHttpResponseWrapper`로 감싼 뒤 로깅한다. 감싸지 않으면 로깅이 응답 스트림을 소비해 호출부가 본문을 읽지 못한다 (에러 없이 빈 본문으로 보인다)
- **상태코드 위임**: `ClientHttpResponse.getStatusCode()`는 원본 `HttpStatusCode`를 그대로 위임한다. `HttpStatus.valueOf(int)`로 재변환하면 비표준 상태코드 수신 시 `IllegalArgumentException`이 터진다 (예: Cloudflare 520 — R2가 Cloudflare다)

---

## 6. 강제 규칙 (CI 게이트)

CI([`.github/workflows/ci.yml`](.github/workflows/ci.yml))에서 강제한다. **실패 시 머지 차단.** 로컬 lefthook pre-commit은 경고만 출력하고 차단하지 않는다.

- **Spotless**: google-java-format 포맷 검증
- **Checkstyle** (`-PciStrict`): 네이밍·복잡도·미사용 import (경고 1건도 실패)
- **ArchUnit**: 레이어 의존 방향·도메인 간 참조 규칙 — **`src/test` 신설 시점까지 보류**(현재 테스트 소스셋 자체가 없다). 억제(suppression) 없이 strict 유지가 원칙. 도입할 규칙 5종(도메인 간 참조 금지 · 레이어 방향 · Entity 누출 금지 · 트리 단방향 · 네이밍)은 Notion 「소스 구조」 §13에 스케치해 뒀다 — **§3의 패키지 이름 규칙이 곧 이 규칙의 전제**다

---

## 7. 에이전트 주의사항

- README/코드에 없는 컨벤션을 **임의로 만들지 말 것.** 새 패턴이 필요하면 코드 작성 전에 먼저 제안하고 확인받는다
- Checkstyle 위반은 **억제하지 말고 소스를 규칙에 맞게 수정**한다 (예: 진입점 `HideUtilityClassConstructor`는 `proxyBeanMethods=false` + private 생성자로 해결)
- 아키텍처 결정은 ADR(`docs/adr/`)로 기록하는 것을 권장 (README의 ADR-001 참조)
