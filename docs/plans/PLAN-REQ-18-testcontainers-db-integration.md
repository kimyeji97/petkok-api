# PLAN-REQ-18 · Testcontainers 도입 — DB 실물 대조 통합 테스트

> 출처: 2026-09-18 세션(REQ-11 마무리 후 남은 백로그 정리 중 REQ-16 미결⑥ 논의) · 작성: 2026-09-18 · 최종 갱신: 2026-09-21 · 상태: ✅ 완료 (Phase 1·2 전부 완료 · 검증 계약 REQ-18-01·02 통과 + REQ-18-03 수동 프로브 통과, 풀 스위트 345건 회귀 없음 · 미결 0건)

## 배경

`./gradlew test`는 DB를 전혀 타지 않는다(`CLAUDE.local.md`). 그래서 "엔티티 ↔ 스키마 실물 대조"는 지금까지 한 번도 자동화된 적이 없고, 대신 사람이 `docker start petkok-pg` + `bootRun`으로 손수 확인해 왔다 — REQ-10 Phase 1·2 keyset 경계 실측, REQ-16 `timestamptz` 왕복 확인이 그 예다.

이 공백이 실제로 구현 결함을 놓친 전례가 있다. JPA Auditing 기본 `DateTimeProvider`가 `OffsetDateTime` 필드를 못 채워 **모든 도메인의 최초 INSERT가 500으로 죽는** 결함이 REQ-10 로컬 DB 실측(2026-09-07)에서야 발견됐다 — `./gradlew test`는 그 경로 자체를 한 번도 실행한 적이 없었기 때문이다.

REQ-16이 이 갭을 알고도 미결로 남겼다(2026-09-03, PLAN-REQ-16 § 제약·함정):

> "따라서 '엔티티 ↔ 스키마 대조'는 실제로 일어나지 않는다. 엔티티만 바꾸고 마이그레이션을 빠뜨리면(또는 그 반대) 조용히 통과한다. Phase 1의 실제 방어선은 REQ-16-04·05(마이그레이션 텍스트 검사)이고, 그마저 DB의 실제 컬럼 타입과는 맞대보지 않는다 → 미결 ⑥"

CLAUDE.md도 이걸 "Testcontainers 도입 전까지 열려 있는 구멍"으로 문서화해 뒀다. 이번 REQ는 그 구멍을 실제로 메운다.

## 범위

**포함**

- 테스트 의존성 추가 — `org.springframework.boot:spring-boot-testcontainers`(test) · `org.testcontainers:postgresql` · `org.testcontainers:junit-jupiter`. Spring Boot 3.3.x가 `@ServiceConnection`으로 컨테이너의 JDBC 접속 정보를 자동 배선하는 1급 지원을 제공하므로, 수동 `@DynamicPropertySource` JDBC 설정 대신 이 경로를 쓴다
- 공용 PostgreSQL 컨테이너 설정 — 클래스마다 새로 띄우지 않고 테스트 세션 전체에서 재사용하는 싱글톤 패턴(빌드 시간 방어)
- 컨테이너 이미지 버전 = `postgres:17` — `CLAUDE.local.md`의 로컬 개발 DB(`petkok-pg`, `postgres:17`, 실제 17.11)와 맞춘다. 운영(Supabase)·로컬·테스트 세 환경의 엔진 버전을 통일
- Flyway 마이그레이션이 테스트 컨테이너 기동 시 자동 적용되도록 배선(스키마 소유는 여전히 Flyway, AGENTS §5)
- **최초 스모크 통합 테스트 1건** — `User` 엔티티(2026-09-18 확정, 아래 `## 결정`)를 실제 컨테이너에 저장 → 조회해 컬럼 타입·JPA Auditing 배선이 실물과 맞는지 확인. 카카오 자동가입 경로(`users` 최초 INSERT)의 재현
- 완료 기준 검증은 이 프로젝트의 프로브 관례를 따른다 — 엔티티 타입을 일부러 깨서(예: 시각 필드 타입을 되돌려서) 새 통합 테스트가 실제로 FAIL하는지 확인한 뒤 되돌린다(CLAUDE.md "구조 규칙을 고치면 일부러 위반을 심어 잡히는지 확인" 원칙의 DB 버전)

**제외**

- **기존 keyset·회귀 테스트 전체를 통합 테스트로 전환** — 이번엔 최소 스모크 1건으로 시작한다. "지금은 최소 스모크로 시작"이 대화에서 합의된 범위이지, 전체 전환은 논의된 적이 없다
- **CI 워크플로우(`ci.yml`) 변경** — `runs-on: ubuntu-latest`(GitHub 호스팅 러너)에는 Docker가 기본 설치돼 있어(확인함, 2026-09-18) 기존 `./gradlew test` 스텝이 별도 설정 없이 그대로 Testcontainers를 태운다. 손댈 CI 설정이 없다
- **기존 REQ-07~17 도메인의 keyset 경계·JPA Auditing 실측을 이걸로 대체** — 이미 사람이 확인해 통과한 것들을 재검증하는 게 아니라, **앞으로 발생할** 같은 종류의 결함을 자동으로 잡는 게 목적이다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 통합 방식 | Spring Boot `@ServiceConnection` + `spring-boot-testcontainers` | Spring Boot 3.3.x 1급 지원 — 컨테이너 JDBC URL·계정을 수동 배선할 필요가 없어 `@DynamicPropertySource` 보일러플레이트가 안 생긴다 | 수동 `@DynamicPropertySource` — 구버전 Spring Boot에서 쓰던 방식, 이 프로젝트 버전에서는 불필요한 코드 |
| 컨테이너 재사용 전략 | 세션 전체 싱글톤(클래스마다 재기동 안 함) | 컨테이너 기동 자체가 수 초 걸려, 클래스마다 새로 띄우면 빌드 시간이 급격히 늘어난다 | 클래스별 `@Testcontainers` 개별 기동 — 격리는 더 확실하지만 이 레포 규모(스모크 1건 시작)에는 과함 |
| PostgreSQL 버전 | `postgres:17` | `CLAUDE.local.md` 로컬 개발 DB와 동일 버전 — 로컬·테스트·운영 엔진 버전 불일치로 인한 재현 안 되는 실패를 막는다 | 최신 버전 고정 없이 `postgres:latest` — 버전 드리프트로 재현성이 깨진다 |
| CI 영향 | 없음(추가 설정 불필요) | `ci.yml`의 `ubuntu-latest`가 Docker를 기본 제공(확인함) | GitHub Actions Docker-in-Docker 별도 설정 — 불필요 |
| 첫 스모크 테스트 대상 | `User` 엔티티 | JPA Auditing 결함이 실제로 터졌던 정확한 경로(카카오 자동가입, `users` 최초 INSERT, REQ-10 2026-09-07 실측)를 그대로 재현 — 근거가 가장 직접적이다. FK 의존성이 없어 셋업도 가장 가볍다(펫·소유자 불필요) | `WeightLog`(REQ-10) — REQ-10 자신이 고른 "가장 단순한 패턴 기증자"지만, `pet_id`→`Pet`→`User` 두 단계 FK가 먼저 필요해 셋업이 더 무겁다 |
| 테스트 스키마 이름 | `petkok_test` 신설, `local`/`dev`/`prod`와 같은 명시적 스키마 관례를 따른다 | AGENTS §5 — "`application.yml`의 `${db.schema}`에는 기본값이 없다. 프로파일이 값을 빠뜨리면 기동 즉시 실패한다(조용히 `public`으로 새는 것보다 낫다)." `public`을 암묵적으로 쓰면 이 프로젝트가 명시적으로 거부해 온 조용한 기본값 폴백을 테스트에서만 허용하는 셈이 된다 | `public` 그대로 사용 — 설정은 줄지만 프로젝트 전체가 지켜 온 "스키마는 항상 명시" 원칙과 정면으로 어긋난다 |
| Testcontainers 버전 핀 | 버전 미명시 — Spring Boot BOM(`io.spring.dependency-management`)에 맡긴다 | `build.gradle.kts` 실측(2026-09-18) — BOM이 관리하는 의존성(`org.postgresql:postgresql`·`spring-boot-starter-*`)은 전부 버전을 안 적고, BOM 밖의 것(JJWT·AWS SDK·Guava)만 명시 버전을 쓴다. 유일한 예외 `extra["flyway.version"]`도 "BOM 기본값이 PG17과 안 맞는다"는 구체적 이유로 override한 것 — `org.testcontainers:*`는 BOM 관리 대상이라 같은 규칙이 그대로 적용된다 | 명시 버전 고정 — 이 레포의 기존 관례(BOM 관리 대상은 버전 생략)와 어긋난다. 문제가 생기면 그때 Flyway 선례처럼 `extra["testcontainers.version"]`으로 override |

## 미결 질문

- [x] **첫 스모크 테스트를 어느 도메인·어떤 케이스로 붙일까? — 확정(2026-09-18): `User`.** 카카오 자동가입 경로(`users` 최초 INSERT)를 재현한다 — REQ-10이 실제로 이 결함을 밟았던 바로 그 경로이자 FK 의존성이 없어 셋업이 가장 가볍다(근거는 `## 결정` 표)
- [x] **테스트 컨테이너의 스키마 이름 — 확정(2026-09-18): `petkok_test` 신설.** AGENTS §5의 "스키마는 항상 명시, 기본값 없음" 원칙을 테스트에도 그대로 적용한다(근거는 `## 결정` 표)
- [x] **버전 핀 정책 — 확정(2026-09-18): 버전 미명시, Spring Boot BOM에 맡긴다.** `build.gradle.kts` 실측으로 "BOM 관리 대상은 버전 생략, 문제 생기면 override"가 이미 이 레포의 일관된 관례임을 확인했다(근거는 `## 결정` 표)

## 작업 단계

- [x] **Phase 1 — 의존성·컨테이너 배선**
      완료 기준: `build.gradle.kts`에 세 의존성 추가 · 싱글톤 컨테이너 설정 클래스 작성 · Flyway 마이그레이션이 컨테이너 기동 시 자동 적용됨을 확인(컨테이너 안에 `flyway_schema_history` 테이블·마이그레이션 행이 실제로 생김) · `./gradlew test` 전체가 기존과 동일하게 통과(회귀 없음, 컨테이너 관련 신규 실패 0건) — **2026-09-21 `/testrun REQ-18`로 확인, 전부 충족**

- [x] **Phase 2 — 스모크 통합 테스트 + 프로브 검증**
      완료 기준: `User` 엔티티를 실제 컨테이너에 저장 → 조회하는 통합 테스트 최소 1건 추가(카카오 자동가입 경로 재현) · 엔티티 타입을 일부러 깨서 테스트가 FAIL하는 것 확인(프로브) → 원복 → PASS 재확인 · REQ-16 미결⑥ 체크 · `CLAUDE.md`의 "Testcontainers 도입 전까지 열려 있다"는 문구 정정 제안 — **2026-09-21 `/testrun REQ-18`·`/checkpoint`로 확인, 전부 충족.** REQ-16 미결⑥은 `PLAN-REQ-16` § 미결 질문에 REQ-18 완료 각주 추가, `CLAUDE.md` 「시각 처리」 절 문구도 정정(둘 다 이 커밋)

## 검증 계약

> 작성: 2026-09-18 · 대상: Phase 1 착수 직전 · 검증: `/testrun REQ-18`
> **테스트 코드는 Phase 별로 들어온다.** Phase 1 완료 기준 중 "의존성 추가"·"컨테이너 설정 클래스 작성"은 구현 산출물 자체이지 검증 가능한 동작이 아니라 케이스로 옮기지 않았다. Phase 2(스모크 테스트 + 프로브)는 Phase 1 구현이 끝나야 착수 전제가 갖춰지므로, 그 행은 `/testgen REQ-18`을 다시 돌려 Phase 2 착수 직전에 추가한다.
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-18-01]` 형태로 박는다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-18-01 | `Flyway` 빈(Testcontainers 통합) | 컨테이너 기동 후 마이그레이션이 자동 적용된다(`info().applied()`가 비어 있지 않다) | 정상 | Phase 1 완료 기준 — "Flyway 마이그레이션이 컨테이너 기동 시 자동 적용됨을 확인(컨테이너 안에 `flyway_schema_history` 테이블·마이그레이션 행이 실제로 생김)" | 1 | ✅ |
| REQ-18-02 | `UserRepository`(실 컨테이너 DB) | `User`를 저장→조회하면 JPA Auditing이 채운 `createdAt`이 채워져 있다(카카오 자동가입 경로 재현) | 정상 | Phase 2 완료 기준 — "`User` 엔티티를 실제 컨테이너에 저장 → 조회하는 통합 테스트 최소 1건 추가(카카오 자동가입 경로 재현)" | 2 | ✅ |
| REQ-18-03 | `JpaAuditingConfig.dateTimeProviderRef` | 커스텀 `DateTimeProvider`를 빼 기본(LocalDateTime-only)으로 되돌리면 REQ-18-02가 실제로 FAIL한다 → 원복 → PASS 재확인 | 프로브(수동) | Phase 2 완료 기준 — "엔티티 타입을 일부러 깨서 테스트가 FAIL하는 것 확인(프로브) → 원복 → PASS 재확인" | 2 | ✅ 수동 |

**REQ-18-03은 코드로 안 쓴다** — REQ-12-33·REQ-10-01~03과 같은 방식(`docs/plans/PLAN-REQ-12-timeline-calendar.md` §검증 계약 참고)으로 `/implement` 단계에서 수행했다. `결과` 열엔 확인 후 "✅ 수동"을 적는다(`/checkpoint`).

> ⚠️ **2026-09-21 `/implement` 실측 — 이 표가 원래 적었던 구체적 방법("`BaseCreatedEntity.createdAt` 필드 타입을 `OffsetDateTime`→`LocalDateTime`으로 되돌린다")은 실제로 FAIL하지 않았다.** Spring의 `ObjectToObjectConverter`가 `LocalDateTime.from(OffsetDateTime)`으로 오프셋만 버리고 조용히 변환에 성공했기 때문 — `대상`·`케이스` 열을 실제로 FAIL을 재현한 방법(`JpaAuditingConfig`의 `dateTimeProviderRef`를 빼 기본 `DateTimeProvider`로 되돌리는 것)으로 위와 같이 정정했다. `cp` 백업으로 두 버전 다 되돌렸다(`git checkout <파일>`은 미스테이지 변경을 함께 삼킨다, CLAUDE.local.md). 재현된 예외는 `Cannot convert unsupported date type java.time.LocalDateTime to java.time.OffsetDateTime` — REQ-10 2026-09-07 실측(PR #51)과 문구까지 동일해, 이 스모크 테스트가 **정확히 그 결함 클래스를 잡아낸다**는 것이 실측으로 확인됐다.

## 제약·함정

- **테스트 컨테이너는 Docker 데몬이 로컬에 떠 있어야 동작한다.** 이 머신은 `CLAUDE.local.md` 기준 이미 Docker로 `petkok-pg`를 띄우고 있어 문제없지만, 다른 머신에서 이 레포를 처음 클론하면 Docker 미설치 시 `./gradlew test`가 조용히 걸리거나 타임아웃날 수 있다 — README에 Docker 필수 안내 추가를 Phase 1에서 함께 검토
- **컨테이너 기동 시간이 첫 테스트 실행에 더해진다.** 싱글톤 재사용으로 최소화하지만, CI 캐시가 없는 첫 실행은 이미지 pull 시간이 추가로 붙는다
- **`ddl-auto: validate` 프로파일이 테스트 컨테이너에도 적용돼야 실제 검증 효과가 있다.** `create`/`update`로 두면 Hibernate가 스키마를 알아서 맞춰버려 REQ-16 미결⑥이 잡으려는 "엔티티↔마이그레이션 어긋남"을 오히려 못 잡는다 — Phase 1에서 프로파일 설정을 반드시 확인
