# PLAN-REQ-14 · 프로젝트 소스 구조 변경안

> 출처: 2026-07-28 세션 · 작성: 2026-07-28 · 상태: 🟡 진행

## 배경

노션 「소스 구조」 §2 패키지 트리와 실제 구현이 4곳 갈려 있었고(C단계 현행화), 그 대조 과정에서 구조 자체를 다시 볼 필요가 드러났다.

문제는 둘이다.

**① 구조 규칙이 산문으로만 존재한다.** "Entity는 Service 밖으로 나가지 않는다", "도메인 간 참조 금지" 같은 규칙이 AGENTS.md에 글로만 있다. 강제 수단이 없으니 도메인이 늘어나면 조용히 깨진다. 게코 전용 로직(shed는 `CRESTED_GECKO`만, activity는 종별 분기)이 이 프로젝트의 차별점인데, 도메인 경계가 흐려지면 그 로직이 다른 도메인으로 샌다.

**② 지금이 구조를 바꿀 수 있는 마지막 시점이다.** 도메인 코드가 0개다. 공통 계층 55개 파일만 있어 이행이 순수 패키지 이동으로 끝난다. auth 도메인이 들어가는 순간 비용이 급증한다.

## 범위

**포함**

- 패키지 구조 확정 — `business` / `data` / `framework` 3분할 (완료, 노션 + AGENTS.md 반영됨)
- `com.petkok.global.*` 55개 파일을 신구조로 이행 — 별도 브랜치 `refactor/package-structure`, 별도 PR
- 이행 완료 후 노션 「소스 구조」와 AGENTS.md에서 "코드는 아직 `global`이다" 경고 제거

**제외**

- **도메인 코드 작성(auth 등)** — 이행 PR은 **순수 이동만**. 로직이 섞이면 diff에서 이동과 변경을 구분할 수 없어 리뷰가 불가능해진다. auth는 REQ-07
- **ArchUnit 규칙 실제 도입** — `src/test` 소스셋 자체가 없다. 노션 §13에 5종을 스케치해 뒀으나 **컴파일·실행해 본 적 없다.** `src/test` 신설 시점으로 미룸
- **`SecurityConfig.PUBLIC_PATHS` 와일드카드 제거** — 결정은 2026-07-27에 끝났으나 코드 수정은 auth 구현과 함께(REQ-07). 이행 PR에서 동작을 바꾸지 않는다
- **`V1__init.sql` condition_tag 주석 7종 정정** — 같은 이유로 REQ-07
- **`business/{도메인}` 하위 실제 생성** — 도메인 코드가 없으므로 만들 것이 없다. 이행 대상은 사실상 `framework/*`와 `data/common/entity`뿐

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 최상위 구조 | `business` / `data` / `framework` 3분할 | 역할별 최상위 분리 + 트리 안은 도메인 단위 | `global` + `domain` 단일 트리(노션 현행 설계) — business/data 구분이 사라진다 |
| **`data` 하위 구성** | **도메인별** (`data/{도메인}/entity`) | `business/{d}`와 `data/{d}`가 같은 이름이면 ArchUnit Slices 한 줄로 도메인 간 참조를 막을 수 있다 | **`data/entity` 평면(사용자 1안)** — 엔티티가 한 패키지라 슬라이스가 나뉘지 않아 규칙이 성립 불가. 클래스명 접두사 기반 커스텀 룰은 이름이 흔들리면 같이 무너져 취약 |
| DTO 패키지명 | `dto` | `domain`은 DDD에서 핵심 비즈니스 모델을 뜻해 entity와 혼동된다 | `data/domain` — 유지하려면 하위를 `request`/`response`로 쪼개고 "이 프로젝트에서 domain=DTO"를 명문화해야 했다 |
| repository 위치 | `data/{도메인}/repository` | 반환 타입이 Entity라 컴파일 결합이 강하고, `business → data` 단방향이 유지돼 ArchUnit 규칙을 한 줄로 쓸 수 있다 | `business/{도메인}/repository` — business 트리가 JPA를 알게 되고 단방향이 깨진다 |
| `uri` · `const` 위치 | `framework/constant`(전역) + `data/{도메인}/enums`(도메인 전용) | URI는 데이터가 아니라 API 계약이고 controller와 함께 읽힌다. 실제로 필요한 enum은 전부 도메인 전용(Species·ConditionTag·ActivityType) | `data/uri`, `data/const` — 전역 상수 창고는 잡동사니가 된다 |
| 베이스 엔티티 위치 | `data/common/entity` | framework는 JPA 매핑 규약을 알지 않아야 한다 | `framework/common/entity` |
| `JwtAuthenticationFilter` | `framework/processor/filter` | 요청 파이프라인에 끼어드는 위치가 성격을 결정한다. `processor` 개념을 살림 | `framework/security/jwt` — TokenProvider와의 응집도가 근거였으나 processor가 비게 된다 |
| ArgumentResolver | **두지 않음** | `@CurrentUser`가 `@AuthenticationPrincipal` 메타 애노테이션이라 리졸버가 실제로 존재하지 않는다(코드 확인) | `framework/processor/resolver` — 설계상 있을 법했으나 실물이 없다 |
| 이행 시점 | auth 착수 **전**, 별도 PR | 도메인 코드 0개일 때가 최저 비용 | auth와 함께 — 이동과 신규 로직이 한 diff에 섞인다 / 나중에 — 비용 급증 |
| 노션 페이지 갱신 방식 | `replace_content` 전체 교체 | 코드블록이 이스케이프 문자열을 그대로 저장하고 있어 부분 치환이 불가능했다(아래 함정) | `update_content` 조각 치환 — 한글 이스케이프를 매칭할 수 없어 실패 |

## 미결 질문

- [x] **현재 브랜치(`docs/progress-and-mysql-plan`) 처리 순서** — 구조 확정(`AGENTS.md` §3 + `CLAUDE.md` 함정)을 현재 브랜치에 커밋해 PR #8에 포함시키고, 머지 후 `main`에서 분기하기로 확정(2026-07-28). 이렇게 해야 이행 PR의 diff가 순수 이동만 담는다
- [ ] **빈 패키지를 미리 만들 것인가.** `framework/constant`, `processor/aspect`, `processor/converter`는 지금 넣을 클래스가 없다. 자리만 만들어 둘지, 필요할 때 만들지 안 정했다
- [ ] **이행 검증 범위 — `bootRun`까지 할 것인가.** 컴파일·Spotless·Checkstyle은 DB 없이 되지만, 빈 등록·컴포넌트 스캔이 실제로 뜨는지는 기동해야 안다. 기동에는 로컬 Postgres가 필요하다(Flyway `V1__init.sql`). 어디까지를 완료로 볼지 미정
- [ ] **ArchUnit §13 규칙이 실제로 동작하는지 미검증.** 특히 Slices 패턴 `com.petkok.*.(*)..`이 `business`/`data`를 건너뛰고 도메인명만 슬라이스 키로 잡는지 확인 안 됐다. `timeline` 예외 처리 방식도 스케치 수준
- [ ] `data/timeline`을 만들 것인가. timeline은 자체 테이블이 없어 `business/timeline`만 두기로 했으나, DTO가 필요하면 `data/timeline/dto`가 생긴다 — 조건부로만 합의됨
- [ ] `ApiUri`(엔드포인트 경로 상수 클래스) 도입 여부. `framework/constant`에 "둘 경우 여기"까지만 정했고 도입 자체는 미정

## 작업 단계

- [ ] **Phase 0** — 현재 브랜치 처리 (미결 질문 1번 해소 후)
      완료 기준: `main`이 구조 확정 문서를 포함하고, working tree가 clean

- [ ] **Phase 1** — `main`에서 `refactor/package-structure` 분기
      완료 기준: `git log origin/main..HEAD`가 비어 있는 상태에서 시작

- [ ] **Phase 2** — 패키지 이동 (`git mv` + `package`/`import` 선언 수정)
      완료 기준: `./gradlew build -x test` 성공. `com.petkok.global` 문자열이 소스 전체에서 0건

- [ ] **Phase 3** — CI 게이트 재현
      완료 기준: `./gradlew spotlessApply` → `build -x test` → `checkstyleMain -PciStrict` 세 개 모두 통과, **Checkstyle 경고 0건**

- [ ] **Phase 4** — 기동 확인
      완료 기준: **미정** (미결 질문 3번)

- [ ] **Phase 5** — PR 생성 + CI 통과
      완료 기준: PR 템플릿 작성, CI `SUCCESS`, diff가 **이동/이름변경만**이고 로직 변경 0건임을 리뷰로 확인

- [ ] **Phase 6** — 노션 + AGENTS.md 역반영
      완료 기준: 노션 §5 도입부·구현 노트, AGENTS.md §1·§3에서 "아직 `com.petkok.global.*`" 경고가 사라지고 코드와 문서가 일치

## 제약·함정

- **`business/{도메인}`과 `data/{도메인}`은 반드시 같은 이름을 써야 한다.** 이름이 어긋나면 ArchUnit Slices 규칙이 **에러 없이 조용히 무력화**된다 — 서로 다른 슬라이스로 잡혀 규칙은 통과하는데 경계는 안 지켜진다. AGENTS.md §3에 계약으로 등재함
- **패키지 이동 후 `spotlessApply`를 먼저 돌린다.** import 순서가 바뀌므로 이 순서를 어기면 CI `spotlessCheck`에서 터진다
- **lefthook이 설치돼 있지 않으면 로컬에서 아무 검증도 안 걸린다.** 새 워크트리·새 클론이면 `lefthook install` 먼저
- **노션 코드블록에 이스케이프 문자열이 저장돼 있으면 MCP로 부분 수정이 불가능하다.** §2 트리 블록이 박스문자·한글을 `├`, `도` 같은 **문자열 그대로** 담고 있었다. `update_content`의 `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없어(항상 실제 문자로 디코딩된다) 매칭 자체가 안 된다. 박스문자는 ASCII 마커를 경유해 우회했으나 한글은 조합 수가 많아 비현실적 → **`replace_content` 전체 교체가 유일한 해법.** 증상만 보면 "수정이 안 먹는다"로 보이므로, 부분 치환이 계속 no-match면 저장 형태를 의심할 것
- **노션 「설계」 섹션의 탭 페이지는 API로 수정할 수 없다** (`validation_error: not a page or database`). 읽기는 되는데 쓰기만 막히면 권한이 아니라 객체 타입 문제다. 일반 페이지는 정상 수정된다
- **이행 PR에 로직 변경을 섞지 않는다.** 순수 이동이어야 리뷰어가 "동작이 그대로다"를 diff만 보고 판단할 수 있다
