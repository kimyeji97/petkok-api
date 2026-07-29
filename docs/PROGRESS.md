# 진행 현황 (PROGRESS)

> 시간순 작업 로그. git이 말하지 못하는 **왜 / 함정 / 기각 이유**를 남긴다.
> 파일명·라인수처럼 `git show`로 볼 수 있는 건 적지 않는다.
> 깨면 회귀하는 **계약**은 이 파일이 아니라 CLAUDE.md/AGENTS.md에 둔다.
>
> 최종 갱신: 2026-07-28

## 요구사항 인덱스

| REQ | 기능명 | 스펙 | 완료일 | 상태 |
|-----|--------|------|--------|:----:|
| REQ-01 | 스켈레톤 — global 공통 계층 + 베이스 엔티티 + Flyway `V1__init.sql` | [README](../README.md) | 2026-07-06 | ✅ |
| REQ-02 | 빌드·품질 도구 (Spotless / Checkstyle `-PciStrict` / JaCoCo) | [AGENTS §6](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-03 | 워크플로우·AI 진입점 (lefthook · CI · PR 템플릿 · AGENTS/CLAUDE) | [AGENTS §4](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-04 | 공통 유틸리티 이식 (`global/util`, spring-java-utility) | — | 2026-07-23 | ✅ |
| REQ-05 | RestTemplate 설정 + 요청·응답 로깅 인터셉터 | — | 2026-07-23 | ✅ |
| REQ-06 | API 설계 초안 + 설계 결정 3건 확정 | [api-list.md](specs/api-list.md) | 2026-07-23 | ✅ |
| REQ-13 | ~~MySQL 전환~~ — 2026-07-27 기각 (PostgreSQL 유지) | [PLAN-REQ-07](plans/PLAN-REQ-07-auth-and-db-environment.md) | — | ❌ |
| REQ-14 | 패키지 구조 재설계 + 이행 (`business`/`data`/`framework` 3분할) | [PLAN-REQ-14](plans/PLAN-REQ-14-package-structure-migration.md) | 2026-07-28 | ✅ |
| REQ-07 | auth 도메인 + DB 환경 구성 (Kakao 로그인 · refresh 로테이션 · V2 `refresh_tokens`) | [PLAN-REQ-07](plans/PLAN-REQ-07-auth-and-db-environment.md) | — | ⏸ |
| REQ-08 | user 도메인 (프로필 · 소셜 계정 연결) | [api-list §2](specs/api-list.md) | — | ⏸ |
| REQ-09 | pet 도메인 + `PetAccessGuard` (소유권 앵커) | [api-list §3](specs/api-list.md) | — | ⏸ |
| REQ-10 | 기록 도메인 5종 (diary/feeding/activity/weight/shed) | [api-list §4~8](specs/api-list.md) | — | ⏸ |
| REQ-11 | gallery (R2 presigned 업로드) | [api-list §9](specs/api-list.md) | — | ⏸ |
| REQ-12 | timeline (다중 테이블 union — QueryDSL 활성화 시점) | [api-list §10](specs/api-list.md) | — | ⏸ |

범례: ✅ 완료 · 🟡 진행 · ⏸ 보류 · ❌ 기각

---

# 로그

<!-- 최신이 위. 날짜 헤딩은 `## YYYY-MM-DD` 형식을 반드시 지킬 것 (/progress 가 파싱) -->

## 2026-07-28

### 노션 현행화 C단계 — 설계 문서와 구현이 갈린 4건

「소스 구조 / 아키텍처 설계」의 패키지 트리·global 표를 실제 코드와 대조해 맞췄다. 예외 서브클래스 트리(`NotFoundException`/`ForbiddenException`/`ConflictException`)는 채택된 적이 없고 실제는 `ErrorCode` enum + 단일 `BusinessException`, 베이스 엔티티는 2단계가 아니라 3단계, `global/util/` 30개와 `RestTemplateConfig`·`RestTemplateLoggingInterceptor`는 문서에 아예 없었다.

**범위 밖에서 더 심각한 걸 발견했다.** §5 표의 `SecurityConfig` 행이 아직 `/auth/**` permitAll로 적혀 있었다 — 2026-07-27에 확정한 "와일드카드 금지, 개별 경로 나열"과 정면으로 충돌한다. 하루 전에 정한 결정이 문서 다른 절에 반영 안 된 상태였다. 같이 고쳤다.

「다음 단계」 체크리스트는 REQ-01·04·05가 끝났는데도 전부 미체크였다. 완료 처리하면서 **코드 쪽 미해결 2건을 명시로 올렸다** — `SecurityConfig.PUBLIC_PATHS`가 아직 `/api/v1/auth/**`인 것, `V1__init.sql`의 condition_tag 주석이 7종이 아니라 4종인 것. 둘 다 REQ-07에서 처리한다.

### 함정 — 노션 코드블록이 이스케이프 문자열을 그대로 저장하고 있었다

§2 패키지 트리 블록이 박스문자·한글을 `├`, `도` 같은 **텍스트로** 담고 있었다. 노션에서도 깨져 보였을 것이다. 문제는 이 상태에서 부분 수정이 **불가능**하다는 것 — `update_content`의 `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없다(항상 실제 문자로 디코딩된다). 박스문자는 "`u251c` → ASCII 마커 → 실제 문자" 2단계 치환으로 우회했지만, 한글은 코드포인트 조합이 75개를 넘어 비현실적이었다. **결국 `replace_content` 전체 교체가 유일한 해법이었다.**

여기서 시간을 태운 진짜 원인은 따로 있다. **`old_str`과 `new_str`을 같은 문자열로 써 놓고 성공 응답을 받아 고쳤다고 착각한 것.** 이스케이프를 다루다 보니 양쪽이 같은 값이 되기 쉬웠고 도구는 성공을 반환한다. no-op 프로브(다른 문자열로 매칭 시도)를 걸고 나서야 저장 형태를 확정할 수 있었다. 같은 원인으로 §4 표의 🦎가 깨져 있던 것도 함께 복구했다.

→ 이 3건은 **`CLAUDE.md`로 승격**했다. 모르면 "수정이 안 먹는다"를 권한 문제로 오진한다.

### 패키지 구조 재설계 확정 — `business` / `data` / `framework`

기존 `global` + `domain` 단일 트리에서 3분할로 바꾸기로 했다. 도메인 코드가 0개인 지금이 이행 비용 최저점이다.

**결정의 핵심은 `data` 밑을 도메인별로 쪼갠 것이고, 근거는 ArchUnit이다.** 처음 제안은 `data/entity`·`data/domain`에 전부 평면으로 모으는 안이었는데, 그러면 `FeedingLog`와 `ShedRecord`가 같은 패키지라 Slices가 나뉘지 않아 **도메인 간 참조 금지 규칙이 성립하지 않는다.** 대안으로 클래스명 접두사 기반 커스텀 `ArchCondition`을 검토했으나 이름 규칙이 흔들리면 같이 무너져 기각했다. `business/{도메인}`과 `data/{도메인}`이 같은 이름을 쓰면 `slices().matching("com.petkok.*.(*)..")` 한 줄로 끝난다 — 같은 도메인끼리는 같은 슬라이스라 허용되고 교차 참조만 걸린다.

같이 정한 것:

- **DTO 패키지는 `domain`이 아니라 `dto`.** `domain`은 DDD에서 핵심 비즈니스 모델을 뜻해 entity와 혼동된다
- **repository는 entity 옆**(`data/{도메인}/repository`). 반환 타입이 Entity라 결합이 강하고, `business`에 두면 business 트리가 JPA를 알게 되면서 `business → data` 단방향이 깨져 규칙을 한 줄로 못 쓴다
- `uri`·`const`는 `data`가 아니다 — URI는 API 계약이라 `framework/constant`, 도메인 전용 값은 `data/{도메인}/enums`
- 베이스 엔티티는 `framework`가 아니라 `data/common/entity`. framework는 JPA 매핑 규약을 알지 않아야 한다
- `JwtAuthenticationFilter`는 `framework/processor/filter`. `security/jwt`에 두는 편이 응집도는 높지만, 요청 파이프라인에 끼어드는 위치가 성격을 결정한다고 봤다
- **`processor/resolver/`는 두지 않는다** — `@CurrentUser`가 `@AuthenticationPrincipal` 메타 애노테이션이라 ArgumentResolver가 실제로 존재하지 않는다(코드 확인). 설계상 있을 법해서 문서에 남으면 없는 클래스를 찾게 된다

노션 「소스 구조」에 **§13 구조 강제(ArchUnit)** 절을 신설해 규칙 5종을 코드로 적었다. **컴파일·실행해 본 적 없는 스케치**이고 문서에도 그렇게 명시했다.

> ⚠️ `business/{도메인}`과 `data/{도메인}`의 이름이 어긋나면 ArchUnit 규칙이 **에러 없이 조용히 무력화**된다 — 서로 다른 슬라이스로 잡혀 규칙은 통과하는데 경계는 안 지켜진다. AGENTS.md §3에 계약으로 등재했다.

### 이행 완료 (PR #10)

54개 파일을 옮겼다(`PetKokApplication`은 제자리). **`package`·`import` 외 변경 라인 0건**을 기계적으로 확인했다 — `git diff --find-renames -M -U0 | grep -vE '^[+-](package |import |$)'` 출력이 비어야 정상이고, 실제로 비었다. 게이트 4종(spotlessApply / build -x test / checkstyleMain -PciStrict / spotlessCheck) 통과.

**컴파일 에러 5건이 났고, 원인은 자리를 바꾼 3개였다.** `RestTemplateLoggingInterceptor`(config→processor/interceptor) · `GlobalExceptionHandler`(exception→processor/handler) · `JwtAuthenticationFilter`(security/jwt→processor/filter). 셋 다 **이전에는 의존 대상과 같은 패키지라 `import` 문이 아예 없었고**, 패키지를 벗어나면서 각각 `RestTemplateLoggingInterceptor`·`BusinessException`/`ErrorCode`·`JwtTokenProvider` import가 필요해졌다. 추가한 import 4줄이 이번 이행의 유일한 내용 변경이다.

> 같은 패키지 안에서 이동하면 안 나던 에러다. **패키지를 가로지르는 이동은 "옮기면 끝"이 아니라 암묵적 동일 패키지 참조가 드러나는 지점**이라고 보는 게 맞다.

빈 패키지(`framework/constant`, `processor/aspect`, `processor/converter`)는 만들지 않았다 — 넣을 클래스가 없고 git이 빈 디렉터리를 추적하지 못한다. 설계는 노션 §2에 남겨 뒀다. `business/`와 `data/{도메인}`도 도메인 코드가 들어올 때 생긴다. 현재 실재하는 것은 `data/common/entity`뿐이다.

### 사고 — PR #8이 낡은 SHA 기준으로 머지돼 커밋 3개가 누락됐다

푸시는 원격 브랜치(`aef47db`)까지 정상 도달했는데 **GitHub이 5분 넘게 PR head를 갱신하지 않았고**(`1f4297d` 고정, `mergeable: null`), 그 상태에서 머지가 실행되면서 그날 커밋 3개(07-27 기록 · 구조 확정 · 07-28 기록)가 `main`에 들어가지 못했다. `git ls-remote`와 `GET /repos/.../branches/...`는 `aef47db`를 반환하는데 `GET /pulls/8`만 `1f4297d`을 반환하는 상태였다.

내 쪽 원인은 **stale한 체크 결과를 통과로 읽은 것**이다. `gh pr checks`가 반환한 초록불은 전날(07-27) 실행분이었는데 SHA를 대조하지 않고 "CI 통과"로 판단했다. 같은 브랜치로 PR #9를 새로 열어 복구했고, 이후 PR #9·#10은 **머지 직전에 PR head와 로컬 HEAD를 대조**한 뒤 진행했다.

→ 이 대조 절차는 AGENTS.md §4에 계약으로 올렸다.

### 함정 — `| tail`이 gradle 종료코드를 가려 실패를 통과로 오보고했다

`./gradlew build -q 2>&1 | tail -15 && echo OK` 형태로 묶어 돌렸더니 파이프라인 종료코드가 `tail`의 것이 되어, **컴파일이 깨졌는데도 `=== build OK ===`가 출력됐다.** 위의 컴파일 에러 5건을 한 번 놓친 원인이다. 게이트는 파이프 없이 `set -e`로 각각 돌려야 한다.

→ CLAUDE.md 로컬 검증 절에 등재했다.

### 함정 — Notion이 인라인 코드 주변 굵게를 `****`로 재정규화한다

`**\`data\`**`를 쓰면 저장 시 `**** \`data\` ****` 형태로 바뀌어, 다음번 `update_content`의 `old_str`이 매칭되지 않는다. 변형을 추측으로 반복하다 결국 `fetch`로 실제 저장 형태를 확인하고서야 고쳤다 — 오늘 CLAUDE.md에 올린 함정("부분 치환이 계속 no-match면 저장 형태를 의심")과 정확히 같은 사례를 스스로 반복한 것이다. **인라인 코드와 굵게를 붙여 쓰지 않는 편이 안전하다.**

## 2026-07-27

이 파일(`docs/PROGRESS.md`)을 신설하면서 2026-07-06 baseline 이후 전체 이력을 소급 정리했다.
레포에 흩어진 진행 기록은 없었다 — README의 "다음 단계"는 로드맵, `docs/specs/api-list.md`는 스펙 문서라 이관 대상이 아니다.

**Notion이 설계의 1차 출처임을 계약으로 등재 (AGENTS §0 신설).**

MySQL 전환 계획을 세우던 중 Notion에 **ADR-002(DB 엔진 선택 — Supabase PostgreSQL, Status: Accepted, 2026-06-29)** 가 이미 있는 것을 발견했다. 레포 문서만 보고 판단한 결과 확정된 아키텍처 결정과 어긋난 계획을 만든 것이다. 원인은 **요구사항·설계·API 계약·ADR의 원본이 레포가 아니라 Notion에 있다는 사실이 어디에도 적혀 있지 않았던 것.** AGENTS.md 맨 앞에 출처 우선순위 표를 두어 매 세션 로드되게 했다.

대조에서 나온 방향별 격차:

- **Notion이 최신 → 레포를 고침**: `api-list.md`의 39개 엔드포인트를 Notion API I/F 기준으로 정합. 기록 도메인 경로가 복수형(`/diaries`)이었으나 원본은 단수(`/diary`), `logout`은 `POST`가 아니라 `DELETE`, presigned URL은 `/photos/upload-url`이 아니라 `/photos/presigned-url`. 근거 없이 추가돼 있던 엔드포인트 6개(social-accounts 3종·`/weights/chart`·photos 상세·PATCH) 제거
- **레포가 최신 → Notion에 역반영 필요**: refresh 토큰 저장소(Notion은 "이후 결정 DB/Redis"로 열려 있음), Gradle wrapper 포함 여부
- **Notion 내부 불일치**: 설계 탭 DDL 블록이 소스 구조 문서보다 뒤처져 있다 — `updated_at` 트리거가 남아 있고 `activity_type`에 `HANDLING`이 빠져 있다. 둘 다 소스 구조 문서에서는 확정 반영된 항목이라 **DDL 블록만 갱신 누락.** 그래서 AGENTS §0에 "DDL 블록과 소스 구조 문서가 다르면 소스 구조 문서를 신뢰"를 명시했다

**차단 조건 2건 해소 — 답은 DB 그리드가 아니라 행 본문에 있었다.**

Notion API I/F를 표(그리드)로만 읽었을 때는 `/auth/refresh`·`/auth/logout`이 똑같이 `🔒 인증 필요 = YES`였다. **각 행을 열어보니 서로 달랐다.**

- `/auth/refresh` → 본문 "🔓 **인증 불필요**", Request Body `{refresh_token}`. 체크박스가 오기였다 (Notion에서 해제)
- `/auth/logout` → 본문 "🔒 인증 필요, **Request Body 없음**", 204. body가 없으니 **access 토큰이 유일한 사용자 식별 수단**이다

→ **`PUBLIC_PATHS`는 와일드카드를 버리고 개별 경로로 나열한다**(`/auth/kakao`, `/auth/refresh`, `/actuator/health`). 현행 `/api/v1/auth/**`를 두면 `/auth/logout`이 무인증 노출된다. 2026-07-23 결정의 "logout은 refresh를 body로 받으니 access 없이 동작한다"는 **전제가 틀렸다** — 원칙은 유효하고 적용만 바뀌었다. AGENTS §5 갱신.

`condition_tag`도 **7개로 확정**. Notion ERD 설계가 "공통(정상/활발) + 게코 전용(거식/탈피도와줌/탈피완료/거꾸리/구토)"으로 명시하고, 테이블 정의서의 `is_assisted → '탈피도와줌' 연계` 규칙도 같은 값을 참조한다. `V1__init.sql` 주석의 4개가 누락이다.

**교훈**: Notion 데이터베이스는 **그리드 속성만 보면 안 된다.** 속성과 행 본문이 어긋날 수 있고, 이번엔 본문이 맞았다. api-list.md에 "체크박스보다 행 본문이 정확하다"를 명시했다.

**Notion 역반영 (이번에 수행)**: refresh 저장소 결정(소스 구조 §7) · Gradle wrapper 포함(구현 노트) · `updated_at` 트리거 제거(테이블 정의서 3곳 + 공통 설계 원칙) · `activity_type` HANDLING(테이블 정의서·ERD) · FastAPI 잔재 정리(테이블 정의서·ERD·시스템 컨텍스트 분석 11곳) · `/auth/refresh` 체크박스 해제 + 로테이션 응답 스펙 추가. 설계 탭 DDL 코드 블록은 API로 쓸 수 없는 객체(탭)라 사용자가 직접 수정했다. ADR·제약사항 DB는 역사적 기록이라 손대지 않았다.

**계약 승격 — RestTemplate 지적 2건을 AGENTS §5로 올렸다.** 응답 버퍼링 필수 / `getStatusCode()` 원본 위임. 둘 다 "모르는 사람이 고치면 조용히 재발"하는 종류라 로그에 묻으면 안 된다 — PROGRESS.md는 필요할 때만 읽히지만 AGENTS.md는 매 세션 로드된다.

**README stale 수정.** README "실행 §1"이 `gradle-wrapper.jar`가 저장소에 없다고 안내하고 있었다(IntelliJ 동기화 또는 `gradle wrapper` 실행 요구). 실제로는 `10f2ad2`(2026-07-07)에서 8.10.2 wrapper를 커밋해 추적 중이고 AGENTS §2는 "포함되어 있다"로 맞게 적혀 있어, 두 문서가 서로 모순이었다. wrapper 준비 절을 걷어내고 이하 번호를 당겼다.

같은 줄에 있던 **`.env.example` 참조도 함께 제거**했다 — 그 파일은 존재한 적이 없다. 없는 파일을 가리키는 안내는 신규 참여자·에이전트가 탐색에 시간을 쓰게 만든다.

### 함정 — Notion 탭 페이지는 API로 수정할 수 없다

「설계」 섹션의 DB·API 탭 본문은 `notion-update-page`가 **`validation_error: Object ... is not a page or database`** 로 거부한다. `fetch`로 읽히고 URL도 페이지처럼 생겼지만 쓰기가 안 되는 객체(탭)다. 같은 프로젝트의 일반 페이지(테이블 정의서·ERD 설계·소스 구조)는 정상 수정된다.

- 증상만 보면 권한 문제로 오인하기 쉽다. **읽기는 되는데 쓰기만 막히면 객체 타입을 의심할 것**
- 우회: 같은 내용이 실린 일반 페이지를 고치고, 탭 안의 원본(이번엔 DDL 코드 블록·HTTP 상태 코드 표)은 **사람이 직접 수정**해야 한다
- 이번에는 DDL 블록 4곳을 사용자가 직접 처리했다

### 브랜치 — 머지된 브랜치에 새 작업을 얹지 않는다

작업을 시작한 `docs/api-list`는 PR #7로 **이미 머지된 브랜치**였다. 여기에 커밋을 쌓아 새 PR을 열면 머지된 PR 페이지까지 어수선해진다. `docs/progress-and-mysql-plan`을 새로 파고 `origin/main` 위로 리베이스해 PR #8을 열었다.

> 로컬 `docs/api-list`에 리베이스 전 커밋 3개가 고아로 남아 있다(원격에는 없음). 내용은 새 브랜치에 모두 보존돼 있으므로 `git branch -f docs/api-list origin/docs/api-list`로 정리하면 된다.

### 다음 — 노션 현행화 C단계 (내일)

Notion 소스 구조 §5 패키지 트리와 실제 구현이 갈린 4건. 구현 노트가 일부 설명하고 있으나 본문 표·트리를 먼저 읽으면 오해한다.

1. 예외 서브클래스 트리(`NotFoundException`/`ForbiddenException`/`ConflictException`) — 실제로는 `ErrorCode` enum + 단일 `BusinessException`
2. 베이스 엔티티가 **3단계**(`BaseCreatedEntity` → `BaseTimeEntity` → `BaseSoftDeleteEntity`) — 표에는 2단계만
3. `global/util/` 계층 통째 누락 (REQ-04에서 이식한 30개)
4. `RestTemplateConfig` · `RestTemplateLoggingInterceptor` 누락 (REQ-05)

---

## 2026-07-23

### 공통 유틸리티 이식 (REQ-04)

date/json/list/map/number/string/encrypt/file/os/spring/http 30개를 `global/util`로 이식했다.
**왜 지금** — 도메인(auth→user→pet…) 착수 전에 변환·검증 보일러플레이트를 공통 계층에서 흡수하기 위함. 도메인마다 같은 걸 재발명하면 되돌리기 어렵다.

함정: 이식 코드가 commons-lang3 / guava / commons-compress를 쓰고 있어 의존성 3개가 따라 들어왔다. 유틸 하나만 골라 가져오는 게 아니라 묶음 이식이었기 때문.

### RestTemplate 로깅 인터셉터 (REQ-05)

Kakao OAuth 등 외부 API 연동 대비. 코드 리뷰에서 지적 3건이 나왔고, 그중 둘은 모르면 재발하는 종류다.

- **응답 본문 버퍼링이 필수다.** 인터셉터가 로깅하며 응답 스트림을 소비하면 호출부가 본문을 다시 읽지 못한다. `BufferingClientHttpResponseWrapper`로 복사한 뒤 로깅한다 — 이 래핑을 걷어내면 "응답이 비어 있다"로 조용히 깨진다
- **`getStatusCode()`는 원본 `HttpStatusCode`를 위임해야 한다.** `HttpStatus.valueOf(int)`로 재변환하면 비표준 상태코드 수신 시 `IllegalArgumentException`이 터진다 (예: Cloudflare 520). R2가 Cloudflare이므로 가상의 위험이 아니다
- 민감 헤더 5종(Authorization/Cookie 등) 마스킹 — AGENTS §5 로깅 규칙 준수. `MaskingUtil`에 자격증명용 메서드를 추가했다
- 오타 수정: contentType 비교 `"text/htmnl"` → `"text/html"`

**연기**: 응답 **본문**의 토큰 노출 마스킹은 처리하지 않았다. 어떤 필드가 민감한지는 연동 대상에 따라 다르므로 domain 단계 Kakao 연동 시점에 실제 응답을 보고 결정한다.

`ErrorCode.EXTERNAL_API_ERROR`(502)를 함께 추가했지만 호출부는 아직 없다 — domain 단계에서 쓰인다.

### CLAUDE.md 로컬 검증 실태 보완

에이전트가 잘못된 완료 보고를 하는 문제가 실제로 있었다. 원인 두 가지를 명시했다.

- **`src/test`가 없다.** `./gradlew test`는 통과하지만 검증된 것이 0이다. "테스트 통과"로 보고하면 안 된다 — 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐
- **lefthook은 자동 설치되지 않는다.** 새 클론·새 워크트리에서 `lefthook install`을 안 하면 `.git/hooks`가 비어 로컬 검증이 전부 무력화되고, CI `spotlessCheck`에서 터진다

### API 설계 초안 + 미결정 3건 확정 (REQ-06)

`V1__init.sql` 9개 테이블과 `ErrorCode`를 기준으로 엔드포인트를 정리하면서 미결정 3건을 남겼고, 같은 날 셋 다 확정했다.

**① refresh 토큰 저장소 → DB(`refresh_tokens`, V2 마이그레이션)**
stateless refresh 기각. 로그아웃·회원 탈퇴 시 즉시 무효화가 불가능하기 때문. 토큰 원문은 저장하지 않고 `SHA256Util` 해시로 보관한다 — DB 유출 시 그대로 재사용 가능해지므로. 무효화는 `revoked_at`으로 표현하며 소프트 딜리트가 아니다(그래서 `BaseCreatedEntity` 상속).

**② 로테이션 적용 + 재사용 감지**
`/auth/refresh` 호출마다 새 refresh를 발급하고 기존 것을 즉시 revoke한다. 정상 클라이언트는 revoke된 토큰을 다시 보낼 일이 없으므로, 재제시되면 탈취로 간주하고 해당 사용자 토큰 전체를 revoke한다.
- **재사용 감지 전용 ErrorCode 기각** — 기존 `INVALID_TOKEN`(401)으로 충분하다. 공격자에게 탐지 여부를 알려줄 이유가 없다
- **만료 행 정리 배치는 범위 제외** — 스케줄러 도입 결정이 함께 필요하다. 운영 부담이 실제로 보이는 시점에 별도로 다룬다

**③ 공개 경로 범위 + PATCH 통일 → AGENTS §5로 승격**
두 규칙 모두 어기면 회귀하므로 로그에 묻지 않고 컨벤션으로 올렸다.
- `/api/v1/auth/**`는 permitAll이므로 **토큰 없이 호출되는 엔드포인트만** 둔다. 인증이 필요한 기능을 `/auth/` 아래 두면 무인증 노출된다 — 소셜 계정 연결을 `/users/me/social-accounts`에 배치한 것이 이 원칙의 적용이다. (`/auth/logout`은 refresh를 body로 받아 revoke하므로 access 토큰 없이 동작 → permitAll이 맞다)
- 수정은 **PATCH로 통일, PUT 기각.** 클라이언트가 일부 필드만 보내는 경우가 대부분인데 PUT으로 받으면 누락 필드와 `null` 덮어쓰기 의도를 구분할 수 없다

---

## 2026-07-07

`develop-convention` 저장소의 컨벤션을 그룹 단위로 이식했다 (그룹 2·3·5·6).

### 빌드/품질 도구 (REQ-02)

- **gradle wrapper가 깨져 있었다.** 스켈레톤에 `gradle-wrapper.jar`가 없어 `./gradlew`가 스텁 에러로 종료 — 로컬·CI 빌드 자체가 불가능한 상태였다. 8.10.2 정식 wrapper를 복원하고 저장소에 커밋했다(이후 클론에서 즉시 빌드 가능)
- **Checkstyle 게이트를 둘로 나눴다.** 기본 실행은 경고만, `-PciStrict` 시 `maxWarnings=0`. 개발 중 매번 막히지 않으면서 머지 게이트는 유지하기 위함
- **`checkstyle.xml`의 SuppressionFilter를 제거했다.** 억제 없이 strict를 유지하고, 구조적 예외는 소스 수정으로 해결한다는 원칙. 실제로 아래 두 건이 이 원칙에 따라 소스 수정으로 처리됐다
  - `GlobalExceptionHandler`: 수동 `static final Logger log` 필드가 `ConstantName`(UPPER_SNAKE) 규칙에 걸림 → Lombok `@Slf4j`로 교체. 이후 프로젝트 전체 로깅이 `@Slf4j`로 통일됐다
  - `PetKokApplication`: `HideUtilityClassConstructor` 위반 → `@SpringBootApplication(proxyBeanMethods = false)` + private 생성자. CGLIB 프록시를 끄는 대가가 따라온다
- JaCoCo는 측정만, 게이트 없음 (`src/test`가 없으므로 현시점 의미 없음)
- 포맷 일괄 적용(`spotlessApply`, 23개 파일)은 **로직 커밋과 분리**했다. 4-space→2-space 전면 변경이라 섞이면 diff가 읽히지 않는다

### 워크플로우·AI 진입점 (REQ-03)

- lefthook `commit-msg`의 Conventional Commits 검증을 **shell/grep 정규식**으로 구현했다. commitlint를 쓰면 Node 툴체인을 들여야 하는데, Java 단일 스택에 런타임을 하나 더 얹을 이유가 없다
- **ArchUnit 스텝은 보류.** 레이어 의존 검사 대상이 `global`뿐이라 검사할 게 없다. feature 도메인 도입 시점(REQ-07~)에 활성화한다
- AGENTS.md 이식 시 원본에 있던 존재하지 않는 문서 링크(CONTRIBUTING / starter-decisions / `.claude/rules`)를 제거하고 README.md를 1차 출처로 지정했다. 없는 문서를 가리키면 에이전트가 탐색에 시간을 쓴다
- `.gitignore`에 `.idea/` 추가 — IntelliJ가 IDE 개인 설정을 index에 올리고 있었다

---

## 2026-07-06

baseline 스켈레톤 커밋. 이후 패키지·프로젝트명을 `com.petkok` / `PetKok`으로 리네임.
`global` 공통 계층(config·common·security·exception) + 베이스 엔티티 3단(`BaseCreatedEntity` → `BaseTimeEntity` → `BaseSoftDeleteEntity`) + Flyway `V1__init.sql` 9개 테이블까지가 범위. 도메인 없음.
