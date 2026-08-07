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
    ├── security/               AuthPrincipal, @CurrentUser, UserStatusChecker(포트), jwt/(JwtTokenProvider, JwtProperties)
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
- **framework가 인터페이스를 정의하고 business가 구현하는 패턴** — `framework/security/UserStatusChecker`가 첫 사례다(2026-08-07, REQ-08 D2). framework의 컴포넌트가 도메인 정보를 필요로 할 때 **필요한 쪽이 필요한 모양을 선언하고, 아는 쪽이 채운다.** 의존은 여전히 `business → framework` 한 방향이라 트리 규칙을 깨지 않는다
	- ⚠️ **인터페이스는 반드시 `framework`에 둔다.** `business`로 옮기면 framework가 그것을 참조하게 되어 **규칙 두 개가 동시에** 깨진다 — `FRAMEWORK_MUST_NOT_KNOW_DOMAIN`(§6 규칙 #4)과 `LAYER_DIRECTION`. 후자는 필터가 정의된 세 레이어 어디에도 속하지 않는데 `Repository` 레이어가 `mayOnlyBeAccessedByLayers("Service")`이기 때문이다. **즉 규칙 #4만 열어서는 직참조가 여전히 통과하지 못한다** (2026-08-03·08-07 프로브 실측)
	- ⚠️ **포트 시그니처에 도메인 타입을 노출하지 않는다.** 엔티티를 돌려주면 framework가 `data..entity..`를 알게 되어 같은 규칙에 걸린다. `UserStatusChecker`가 `UUID`를 받아 `boolean`을 돌려주는 이유다
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
- ⚠️ **스택 PR의 base 자동 재지정은 "선행 PR 머지"가 아니라 "base 브랜치 삭제"가 조건이다.** 선행 PR을 머지해도 그 브랜치가 원격에 남아 있으면 후속 PR의 base는 그대로이고, 머지하면 **`main`이 아니라 그 브랜치로 들어간다.** 머지는 성공으로 표시되고 PR도 `MERGED`가 되므로 **조용하다** — `main`에 아무것도 안 올라간 것은 따로 확인해야만 보인다. 2026-08-07에 실제로 발생했다(#21 머지 28분 뒤 #22가 `chore/req08-phase0-archunit`으로 머지 → Phase 1 전체 13파일이 `main`을 비껴감, #23으로 복구)
	- 대응은 셋 중 하나 — 선행 PR 머지 시 **브랜치 삭제를 함께** 하거나, 후속 PR의 **base를 수동으로 `main`으로 변경**(`gh pr edit <n> --base main`)하거나, 애초에 스택하지 않는다
	- **머지 후에는 `main`에 실제로 들어갔는지 확인한다.** `git log --oneline origin/main..origin/<브랜치>`가 비어 있어야 한다. PR 상태(`MERGED`)만으로는 알 수 없다
- ⚠️ **`git log -- <경로>`로 "커밋 안 됐다"고 단정하지 말 것.** 이 명령은 **HEAD에서 도달 가능한 커밋만** 본다. 다른 브랜치의 작업은 안 보이는데 출력은 "이 파일의 전체 이력"처럼 보이고, **에러도 경고도 없다.** 2026-08-03에 이 한 줄로 "REQ-08 Phase 0가 유실됐다"고 판단해 **같은 변경을 통째로 중복 재구현**했다 — 실제로는 미푸시 로컬 브랜치(`chore/archunit-tighten-empty-allowance`의 `f6f66c7`)에 그대로 있었다. 확인은 `git log --all -- <경로>` 또는 `git branch --all --contains <sha>`로 한다
	- 같은 이유로 **작업을 끝냈으면 브랜치를 푸시한다.** 로컬에만 있는 브랜치는 `git status`·`git log`·워킹 트리 어디에도 나타나지 않아 **다음 세션에서 없는 것과 구별되지 않는다.** 위 사고의 뿌리는 "커밋을 안 한 것"이 아니라 "푸시·PR을 안 한 것"이었다

---

## 5. 코드 컨벤션 (핵심)

- **레이어**: Controller → Service → Repository → Entity/DTO (단방향). **Entity는 Service 밖으로 나가지 않는다** (응답은 DTO)
- **트리 방향**: `business → data` 단방향. `framework`는 `business`·`data`를 **참조하지 않는다**(역참조 금지). 다른 도메인 참조 금지 — 예외는 `business/timeline` 하나뿐
- **응답**: 공통 wrapper `ApiResponse<T>` (`{data, error}`). 전역 snake_case (Jackson). 에러는 `BusinessException` + `ErrorCode` enum → `GlobalExceptionHandler` 전역 처리
- **스키마 소유 = Flyway.** Supabase 대시보드 수동 DDL 금지 (drift 방지)
- **DB 스키마는 프로파일별로 분리한다** — `local`/`dev`/`prod` = `petkok_local`/`petkok_dev`/`petkok_prod`. 값의 출처는 `application-{profile}.yml`의 **`db.schema` 한 곳**이고, `application.yml`이 이 값을 Flyway(`spring.flyway.schemas` + `default-schema`)와 Hibernate(`hibernate.default_schema`) **양쪽**에 배선한다
	- ⚠️ **스키마를 손볼 때 이 배선 중 한쪽만 바꾸지 말 것.** 테이블이 생기는 곳과 조회하는 곳이 갈리는데 **에러 없이 "테이블이 없다"로만 나타난다.** 값을 바꿔야 하면 `db.schema`만 바꾼다
	- `V1__init.sql`은 스키마를 명시하지 않고 `search_path`에 의존한다. **이 의존은 의도적이다** — Flyway `default-schema`가 잡아 주므로 마이그레이션에 스키마를 하드코딩하면 환경별로 못 쓰게 된다
	- `application.yml`의 `${db.schema}`에는 기본값이 없다. 프로파일이 값을 빠뜨리면 기동 즉시 실패한다(조용히 `public`으로 새는 것보다 낫다)
- **감사(auditing)**: `created_at = @CreatedDate`, `updated_at = @LastModifiedDate` (JPA Auditing, DB 트리거 없음). 베이스 엔티티 상속으로 처리
- **소프트 딜리트**: `users`, `pets`만 `deleted_at` (`BaseSoftDeleteEntity`)
- ⚠️ **`@Transactional` 안에서 예외를 던지면 그 트랜잭션의 쓰기가 전부 사라진다.** "무효화하고 거절한다"는 모양(재사용 감지, 소유권 위반 기록, 실패 카운트 증가 등)이 이 기본값과 정면으로 충돌한다. **남겨야 하는 쓰기가 있으면 `noRollbackFor`(또는 별도 트랜잭션)를 반드시 명시한다**
	- **충돌 결과가 조용하다.** 거절 응답은 규격대로 나가므로 API 레벨 확인으로는 통과하고, **저장소를 목으로 대체한 단위 테스트도 통과한다**(목은 롤백되지 않는다). 2026-07-30 실측 — `AuthService.refresh`의 재사용 감지가 401을 정상 반환하면서 `revokeAllByUserId`만 되돌아가, 탈취범이 쥔 나머지 토큰이 그대로 살아 있었다
	- 따라서 **DB를 실제로 태워 확인**해야 하고, 확인 후에는 애노테이션 자체를 테스트로 고정한다(`AuthServiceRefreshTest` REQ-07-23이 그 예다). 동작은 목으로 검증할 수 없으므로 애노테이션 고정 + DB 왕복 두 겹으로 간다
- **Enum**: Java Enum + `@Enumerated(STRING)`, DB는 varchar
- **페이지네이션**: 커서 기반 (opaque base64 `next_cursor`)
- **수정 메서드**: 리소스 수정은 `PATCH`(부분 수정)로 통일. `PUT`(전체 교체)은 쓰지 않는다 — 누락 필드와 `null` 의도를 구분할 수 없기 때문
- ⚠️ **PATCH 요청 DTO 필드에 `@NotBlank`·`@NotNull`을 붙이지 않는다.** 둘 다 `null`을 거부하는데, PATCH는 **필드를 안 보내는 것이 정상 경로**다(누락·`null` = "변경 없음"). 붙이면 **부분 수정 요청이 통째로 400**이 된다. `NOT NULL`은 **엔티티의 불변식이지 요청 DTO의 불변식이 아니다** — 두 층의 제약을 같은 것으로 착각하기 쉽다. 길이 등 스키마 제약은 `@Size`로만 표현한다(`@Size`는 `null`을 통과시킨다). 2026-08-03 `UserUpdateRequest` 초안에서 실제로 밟았다
	- **길이 제약은 생략하지 말 것.** `@Size(max = N)`이 없으면 초과 입력이 `DataIntegrityViolationException`으로 올라와 **400이 아니라 500**이 된다 — 클라이언트 입력 오류가 서버 오류로 보고된다
	- **부분 반영 병합은 서비스에서 한다.** 엔티티의 `updateXxx(...)`는 받은 값을 그대로 쓰며 `null`에 도메인 의미를 두지 않는다. 엔티티가 `null`을 "변경 없음"으로 해석하기 시작하면 호출부마다 뜻이 갈린다. **서비스가 병합을 빠뜨리면 에러 없이 다른 필드가 지워지고 응답은 200으로 정상이다** — API 레벨 확인으로는 잡히지 않으므로 "일부 필드만 보냈을 때 나머지가 유지되는가"를 테스트로 고정한다(`UserServiceTest` REQ-08-03·04, `UserTest` REQ-08-08)
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
- **ArchUnit**: **도입 완료(2026-07-29).** `src/test/java/com/petkok/architecture/` 에 규칙 8개 — 도메인 간 참조 금지 · 레이어 방향 · Entity 누출 금지 · 트리 단방향 2종 · 네이밍 3종. 억제(suppression) 없이 strict 유지가 원칙
	- **도메인 간 참조 규칙은 `DomainBoundaryTest` 로 분리돼 있다.** `@AnalyzeClasses` 범위를 `business`·`data` 로 좁혀야 하기 때문이다 — `framework` 가 섞이면 `framework.config` 가 "config" 슬라이스로 잡혀 규칙이 엉뚱해진다
	- ⚠️ **슬라이스 패턴은 `com.petkok.*.(*)..` 이다. 첫 세그먼트를 캡처하면(`(business|data)`) 규칙이 정반대로 동작한다** — 트리 이름이 슬라이스 키에 포함돼 `business/feeding` 과 `data/feeding` 이 다른 슬라이스가 되고 같은 도메인 참조까지 위반으로 잡힌다
	- **§3의 패키지 이름 규칙이 곧 이 규칙의 전제**다. 이름이 어긋나면 규칙은 통과하는데 경계는 안 지켜진다

---

## 7. 에이전트 주의사항

- README/코드에 없는 컨벤션을 **임의로 만들지 말 것.** 새 패턴이 필요하면 코드 작성 전에 먼저 제안하고 확인받는다
- Checkstyle 위반은 **억제하지 말고 소스를 규칙에 맞게 수정**한다 (예: 진입점 `HideUtilityClassConstructor`는 `proxyBeanMethods=false` + private 생성자로 해결)
- 아키텍처 결정은 ADR(`docs/adr/`)로 기록하는 것을 권장 (README의 ADR-001 참조)
