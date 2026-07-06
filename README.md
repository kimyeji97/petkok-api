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
- PostgreSQL 15+ (Supabase 무료 티어 가능)

## 실행
### 1) Gradle Wrapper 준비
`gradle-wrapper.jar` 바이너리는 저장소에 포함되어 있지 않습니다. 둘 중 하나:
- **IntelliJ IDEA 로 프로젝트 열기** → Gradle 동기화 시 wrapper 자동 생성 (권장)
- 로컬에 Gradle 설치 후: `gradle wrapper --gradle-version 8.10.2`

### 2) 환경변수
`.env.example` 참고. 로컬은 `application-local.yml` 에 기본값이 있어 DB만 있으면 바로 뜹니다.
필수(운영): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`(32바이트+),
그리고 gallery 사용 시 `R2_*`.

### 3) 기동
```bash
./gradlew bootRun            # 기본 프로파일 local
# 또는 프로파일 지정
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
기동 시 Flyway 가 `V1__init.sql` 로 9개 테이블을 생성합니다.

## 패키지 구조
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
