# AGENTS.md

이 파일은 AI 에이전트(Claude, Copilot 등)를 위한 프로젝트 진입점이다.

## 0. 출처 우선순위 (먼저 읽을 것)

**요구사항·설계·API 계약·ADR의 원본은 이 저장소가 아니라 Notion에 있다.**
<https://app.notion.com/p/yjkim97/PetKok-389b81b56e6080f6bfc2f7972108e778>

| 대상 | 1차 출처 |
| --- | --- |
| 요구사항, 유저 스토리, 비즈니스 규칙, 제약사항 | **Notion** → 기획/분석 |
| ADR (ADR-001 스택 선택, ADR-002 DB 엔진 선택) | **Notion** → 기획/분석. `docs/adr/`는 비어 있다 |
| 테이블 정의서(DDL), ERD | **Notion** → 설계 → DB 탭 |
| **API 계약 (엔드포인트·메서드·인증 여부)** | **Notion** → 설계 → API 탭 → `API I/F` DB |
| 소스 구조·레이어 규칙·파생 로직 설계 | **Notion** → 설계 → 소스 구조/아키텍처 설계 |
| 스택·패키지 구조·로컬 실행 | [`README.md`](README.md) |
| 코드 컨벤션·CI 게이트 | 이 문서 (§5·§6) |
| 진행 이력·판단 근거 | [`docs/PROGRESS.md`](docs/PROGRESS.md) |

- `docs/specs/api-list.md`는 **Notion API I/F의 파생 요약**이다. 원본이 아니다. **충돌하면 언제나 Notion이 이긴다**
- 설계 관련 판단이 필요하면 레포 문서만 보고 결정하지 말 것. 실제로 레포 문서만 보고 진행했다가 이미 Accepted 상태이던 ADR-002(DB 엔진)와 어긋난 계획을 세운 적이 있다
- 반대로 레포에서 확정한 내용이 Notion보다 앞서는 경우도 있다(예: refresh 토큰 저장소). 그때는 **Notion에 역반영을 제안**하고, 어느 쪽이 최신인지 날짜로 판단한다
- Notion DDL 블록은 소스 구조 문서보다 갱신이 늦는다. 둘이 다르면 소스 구조 문서와 `V1__init.sql`을 신뢰하고 확인을 요청한다

---

## 1. 프로젝트 개요

**PetKok API** — 반려동물(크레스티드 게코 / 강아지 / 고양이) 다이어리 백엔드. 게코 특화 로직이 핵심 차별점.
현재 **개발 1단계 = 뼈대(skeleton)** 상태: `global` 공통 계층 + 베이스 엔티티 + Flyway 초기 스키마까지. 도메인(auth/user/pet/…)은 다음 단계.

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

```
com.petkok
├── global/
│   ├── config/       SecurityConfig, JpaAuditingConfig, JacksonConfig, WebConfig, R2Config
│   ├── common/
│   │   ├── entity/   BaseCreatedEntity → BaseTimeEntity → BaseSoftDeleteEntity
│   │   ├── response/ ApiResponse{data,error}, ErrorResponse
│   │   └── pagination/ CursorRequest, CursorPage, CursorCodec
│   ├── security/     AuthPrincipal, @CurrentUser, jwt/(TokenProvider, AuthFilter, Properties)
│   ├── exception/    ErrorCode, BusinessException, GlobalExceptionHandler
│   └── util/         spring-java-utility 이식 (date/json/list/map/number/string/encrypt/file/os/spring/http 하위 유틸리티)
└── domain/           (다음 단계: auth → user → pet → diary/feeding/activity/weight/shed/gallery/timeline)
```

---

## 4. 워크플로우

- **브랜치**: GitHub Flow. `main` + feature 브랜치. 네이밍: `feat/kebab-case`, `fix/…`, `chore/…` 등
- **커밋**: Conventional Commits. 타입은 영어(`feat` `fix` `refactor` …), 제목·본문은 한글. lefthook `commit-msg`가 형식 검증(위반 시 차단)
- **PR**: PR 필수, 템플릿([`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md)) 작성. CI 통과가 머지 조건

---

## 5. 코드 컨벤션 (핵심)

- **레이어**: Controller → Service → Repository → Entity/DTO (단방향). **Entity는 Service 밖으로 나가지 않는다** (응답은 DTO)
- **응답**: 공통 wrapper `ApiResponse<T>` (`{data, error}`). 전역 snake_case (Jackson). 에러는 `BusinessException` + `ErrorCode` enum → `GlobalExceptionHandler` 전역 처리
- **스키마 소유 = Flyway.** Supabase 대시보드 수동 DDL 금지 (drift 방지)
- **감사(auditing)**: `created_at = @CreatedDate`, `updated_at = @LastModifiedDate` (JPA Auditing, DB 트리거 없음). 베이스 엔티티 상속으로 처리
- **소프트 딜리트**: `users`, `pets`만 `deleted_at` (`BaseSoftDeleteEntity`)
- **Enum**: Java Enum + `@Enumerated(STRING)`, DB는 varchar
- **페이지네이션**: 커서 기반 (opaque base64 `next_cursor`)
- **수정 메서드**: 리소스 수정은 `PATCH`(부분 수정)로 통일. `PUT`(전체 교체)은 쓰지 않는다 — 누락 필드와 `null` 의도를 구분할 수 없기 때문
- **공개 경로**: `SecurityConfig.PUBLIC_PATHS`(`/api/v1/auth/**`)에는 토큰 없이 호출되는 엔드포인트만 둔다. 인증이 필요한 기능을 `/auth/` 아래 두면 무인증 노출된다
- **네이밍/상수**: 클래스 UpperCamelCase, 상수 `UPPER_SNAKE_CASE`, DTO는 `XxxRequest`/`XxxResponse`
- **로깅**: Lombok `@Slf4j` (필드 `log`). 민감정보(전화번호·토큰 등)는 마스킹
- **외부 API 응답 버퍼링**: 로깅 인터셉터는 응답을 `BufferingClientHttpResponseWrapper`로 감싼 뒤 로깅한다. 감싸지 않으면 로깅이 응답 스트림을 소비해 호출부가 본문을 읽지 못한다 (에러 없이 빈 본문으로 보인다)
- **상태코드 위임**: `ClientHttpResponse.getStatusCode()`는 원본 `HttpStatusCode`를 그대로 위임한다. `HttpStatus.valueOf(int)`로 재변환하면 비표준 상태코드 수신 시 `IllegalArgumentException`이 터진다 (예: Cloudflare 520 — R2가 Cloudflare다)

---

## 6. 강제 규칙 (CI 게이트)

CI([`.github/workflows/ci.yml`](.github/workflows/ci.yml))에서 강제한다. **실패 시 머지 차단.** 로컬 lefthook pre-commit은 경고만 출력하고 차단하지 않는다.

- **Spotless**: google-java-format 포맷 검증
- **Checkstyle** (`-PciStrict`): 네이밍·복잡도·미사용 import (경고 1건도 실패)
- **ArchUnit**: 레이어 의존 방향·도메인 간 참조 규칙 — **feature 도메인 도입 시점까지 보류**(현재 검사 대상 없음). 억제(suppression) 없이 strict 유지가 원칙

---

## 7. 에이전트 주의사항

- README/코드에 없는 컨벤션을 **임의로 만들지 말 것.** 새 패턴이 필요하면 코드 작성 전에 먼저 제안하고 확인받는다
- Checkstyle 위반은 **억제하지 말고 소스를 규칙에 맞게 수정**한다 (예: 진입점 `HideUtilityClassConstructor`는 `proxyBeanMethods=false` + private 생성자로 해결)
- 아키텍처 결정은 ADR(`docs/adr/`)로 기록하는 것을 권장 (README의 ADR-001 참조)
