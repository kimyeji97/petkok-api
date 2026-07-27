# 진행 현황 (PROGRESS)

> 시간순 작업 로그. git이 말하지 못하는 **왜 / 함정 / 기각 이유**를 남긴다.
> 파일명·라인수처럼 `git show`로 볼 수 있는 건 적지 않는다.
> 깨면 회귀하는 **계약**은 이 파일이 아니라 CLAUDE.md/AGENTS.md에 둔다.
>
> 최종 갱신: 2026-07-27

## 요구사항 인덱스

| REQ | 기능명 | 스펙 | 완료일 | 상태 |
|-----|--------|------|--------|:----:|
| REQ-01 | 스켈레톤 — global 공통 계층 + 베이스 엔티티 + Flyway `V1__init.sql` | [README](../README.md) | 2026-07-06 | ✅ |
| REQ-02 | 빌드·품질 도구 (Spotless / Checkstyle `-PciStrict` / JaCoCo) | [AGENTS §6](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-03 | 워크플로우·AI 진입점 (lefthook · CI · PR 템플릿 · AGENTS/CLAUDE) | [AGENTS §4](../AGENTS.md) | 2026-07-07 | ✅ |
| REQ-04 | 공통 유틸리티 이식 (`global/util`, spring-java-utility) | — | 2026-07-23 | ✅ |
| REQ-05 | RestTemplate 설정 + 요청·응답 로깅 인터셉터 | — | 2026-07-23 | ✅ |
| REQ-06 | API 설계 초안 + 설계 결정 3건 확정 | [api-list.md](specs/api-list.md) | 2026-07-23 | ✅ |
| REQ-07 | auth 도메인 (Kakao 로그인 · refresh 로테이션 · V2 `refresh_tokens`) | [api-list §1](specs/api-list.md) | — | ⏸ |
| REQ-08 | user 도메인 (프로필 · 소셜 계정 연결) | [api-list §2](specs/api-list.md) | — | ⏸ |
| REQ-09 | pet 도메인 + `PetAccessGuard` (소유권 앵커) | [api-list §3](specs/api-list.md) | — | ⏸ |
| REQ-10 | 기록 도메인 5종 (diary/feeding/activity/weight/shed) | [api-list §4~8](specs/api-list.md) | — | ⏸ |
| REQ-11 | gallery (R2 presigned 업로드) | [api-list §9](specs/api-list.md) | — | ⏸ |
| REQ-12 | timeline (다중 테이블 union — QueryDSL 활성화 시점) | [api-list §10](specs/api-list.md) | — | ⏸ |

범례: ✅ 완료 · 🟡 진행 · ⏸ 보류 · ❌ 기각

---

# 로그

<!-- 최신이 위. 날짜 헤딩은 `## YYYY-MM-DD` 형식을 반드시 지킬 것 (/progress 가 파싱) -->

## 2026-07-27

이 파일(`docs/PROGRESS.md`)을 신설하면서 2026-07-06 baseline 이후 전체 이력을 소급 정리했다.
레포에 흩어진 진행 기록은 없었다 — README의 "다음 단계"는 로드맵, `docs/specs/api-list.md`는 스펙 문서라 이관 대상이 아니다.

**발견 — README와 실제가 불일치.** README "실행 §1"은 `gradle-wrapper.jar`가 저장소에 없다고 안내하지만(IntelliJ 동기화 또는 `gradle wrapper` 실행 요구), 실제로는 `10f2ad2`(2026-07-07)에서 8.10.2 wrapper를 커밋해 추적 중이다. AGENTS.md §2는 "포함되어 있다"로 맞게 적혀 있어 두 문서가 서로 모순된다. README 쪽이 stale. 미수정 — 다음 작업 후보.

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
