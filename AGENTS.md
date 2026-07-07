# AGENTS.md

이 파일은 AI 에이전트(Claude, Copilot 등)를 위한 프로젝트 진입점이다.
사람이 읽는 상세 문서는 [`README.md`](README.md)가 1차 출처다 — 스택·패키지 구조·컨벤션은 README를 신뢰하라.

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
│   └── exception/    ErrorCode, BusinessException, GlobalExceptionHandler
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
- **네이밍/상수**: 클래스 UpperCamelCase, 상수 `UPPER_SNAKE_CASE`, DTO는 `XxxRequest`/`XxxResponse`
- **로깅**: Lombok `@Slf4j` (필드 `log`). 민감정보(전화번호·토큰 등)는 마스킹

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
