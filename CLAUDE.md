@AGENTS.md

## Claude 전용
- 구현 시작 전 [`README.md`](README.md)와 `@AGENTS.md`의 해당 규칙을 먼저 읽을 것. 스펙 문서(있으면 `docs/specs/`)나 관련 ADR(`docs/adr/`)이 있으면 함께 확인
- 새로 배운 프로젝트 컨벤션은 임의 적용하지 말고 제안 후 AGENTS.md에 반영

## 로컬 검증 (AGENTS.md §6 보완)
- lefthook 훅이 설치되어 있다면(`.git/hooks/pre-commit` 존재) 커밋 시 `spotlessApply`가 자동 적용되고 Checkstyle 경고가 출력된다. 새 클론·새 워크트리에서는 `lefthook install`을 먼저 실행할 것 — 미설치 상태면 아무 검증도 걸리지 않아 CI `spotlessCheck`에서 터진다
- 커밋 전 CI 게이트 재현: `./gradlew spotlessApply && ./gradlew build -x test && ./gradlew checkstyleMain -PciStrict`
- ⚠️ **게이트를 `| tail`·`| head`·`| grep` 같은 파이프에 물리지 말 것.** 파이프라인 종료코드가 마지막 명령의 것이 되어 앞쪽 실패가 통째로 가려진다. 출력을 줄이려면 파이프 대신 `-q`만 쓰고 `set -e`로 각각 실행한다. 실측 2건 —
	- `./gradlew build -q 2>&1 | tail -15 && echo OK` → **컴파일 에러 5건인데 `OK`가 출력됐다**
	- `timeout 180 ./gradlew bootRun | grep ...` → **macOS에 `timeout`이 없어**(coreutils의 `gtimeout`) 즉시 죽었는데 종료코드 0으로 보였다. 시간 제한이 필요하면 백그라운드 실행 + 로그 파일 확인으로 대체한다
- **테스트명에는 검증 계약 ID를 `[REQ-07-12]` 형태로 박는다** (`@DisplayName` + 메서드명 `req_07_12_...`). 계획서 `## 검증 계약` 표의 ID와 **한 글자도 어긋나면 안 된다** — 이 문자열이 표와 코드를 잇는 유일한 끈이고, `/testrun`의 REQ 필터가 이걸로 골라낸다. 어긋나면 **에러가 아니라 "0건 통과"로 나타나** 검증이 없는데 초록불이 된다. 새 케이스는 코드보다 표를 먼저 갱신할 것(`/testgen`)
- **`src/test`는 ArchUnit 구조 규칙 8개 + 검증 계약(REQ-07 auth · REQ-08 user · REQ-09 pet · REQ-15 컨트롤러)이다.** pet 아래 도메인(diary·feeding·activity·weight·shed·gallery·timeline)은 코드도 테스트도 없다. `./gradlew test` 통과를 "기능이 검증됐다"로 넓혀 보고하지 말 것 — **커버된 케이스는 각 계획서 `## 검증 계약` 표가 전부다**
	- 구조 규칙의 **빈 집합 통과는 2026-08-03(REQ-08 Phase 0)에 막았다** — `allowEmptyShould(false)` 6건 + `LAYER_DIRECTION`은 `withOptionalLayers(false)`. 대상 코드가 없으면 통과가 아니라 실패한다
- ⚠️ **구조 규칙을 고치면 일부러 위반을 심어 잡히는지 확인할 것.** 규칙이 조용히 공허해지는 일이 실제로 있었다 — 슬라이스 패턴에 괄호를 잘못 쳐(`(business|data)`) 트리 이름이 슬라이스 키에 섞이는 바람에 같은 도메인 참조까지 위반으로 잡혔다. 통과/실패만 봐서는 알 수 없다
- ⚠️ **`--tests` 패턴은 glob `*`·`?`만 안다. 문자 클래스(`[56]`)·중괄호·정규식을 쓰면 0건 매칭이고, 그게 `BUILD SUCCESSFUL`로 나온다.** 위 프로브에서 이게 가장 위험하다 — 결과를 "통과"가 아니라 **"규칙이 결함을 못 잡는다"는 반대 결론**으로 읽게 만든다. 2026-08-10 실측: `--tests '*req_09_1[56]*'`이 초록불이라 핸들러가 무력한 줄 알았는데, 클래스명으로 다시 걸자 두 케이스 모두 정상 발화했다
	- **여러 케이스를 한 번에 돌리려면 클래스명으로 거른다** — `--tests '*PetControllerWebMvcTest'`. 케이스 단위가 필요하면 `--tests` 를 여러 번 나열한다
	- **실행 건수는 `build/test-results/test/*.xml`로 확인한다.** 콘솔에는 0건과 전건 통과가 똑같이 보인다
- **앱을 띄우려면 `.env`가 필요하다** — `set -a && . ./.env && set +a && ./gradlew bootRun`. `DB_PASSWORD`에는 기본값이 없어 주입하지 않으면 기동이 막힌다(의도적)
	- ⚠️ **`.env`의 `KEY=`(빈 값)은 "미설정"이 아니다.** 빈 문자열이 환경변수로 들어가고 Spring의 `${VAR:기본값}`은 **미정의일 때만** 기본값을 쓴다 — 빈 값이면 기본값이 무력화된다. 기본값을 쓰려면 그 줄을 주석 처리할 것
- ⚠️ **문서에 실제 시크릿을 쓰지 말 것.** 함정·재현 로그를 `PROGRESS.md`에 적다가 실제 로컬 DB 비밀번호가 public 레포에 커밋된 적이 있다(2026-07-29 `5c7313b`). 재현 상황을 설명할 때는 값을 더미로 바꾼 뒤 적는다
- `docs/specs/`의 두 문서 — [`api-list.md`](docs/specs/api-list.md)(Notion API I/F 파생), [`db-schema.md`](docs/specs/db-schema.md)(Notion 테이블 정의서 파생) — 는 **둘 다 파생 요약이다.** 원본이 아니다. `docs/adr/`는 비어 있고 **ADR-001·ADR-002의 원본은 Notion에 있다.** 설계 판단 전에 AGENTS.md §0(출처 우선순위)을 먼저 볼 것

## Notion 편집 함정 (AGENTS.md §0 보완)

설계 원본이 Notion에 있어 편집이 잦다. 아래 3건은 모르면 증상을 오진한다.

- **「설계」 섹션의 DB·API 탭은 API로 수정할 수 없다.** `notion-update-page`가 `validation_error: Object ... is not a page or database`로 거부한다. `fetch`로 읽히고 URL도 페이지처럼 생겼지만 쓰기가 안 되는 객체(탭)다. **읽기는 되는데 쓰기만 막히면 권한이 아니라 객체 타입을 의심할 것.** 우회: 같은 내용이 실린 일반 페이지를 고치고, 탭 안의 원본은 사람이 직접 수정해야 한다
- **코드블록이 이스케이프 문자열을 그대로 저장하고 있을 수 있다.** 「소스 구조」 §2 트리가 박스문자·한글을 `├`, `도` 같은 **텍스트로** 담고 있었다(노션에서도 깨져 보인다). 이 상태에서는 `update_content` 부분 치환이 불가능하다 — `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없어(항상 실제 문자로 디코딩된다) 매칭 자체가 안 된다. 박스문자는 ASCII 마커 2단계 치환으로 우회되지만 한글은 조합 수가 많아 비현실적이다. **해법은 `replace_content` 전체 교체 하나뿐.** 부분 치환이 계속 `No matches found`면 저장 형태를 의심할 것
- **`old_str`과 `new_str`을 같은 문자열로 쓰면 성공은 하지만 아무것도 안 바뀐다.** 이스케이프를 다루다 보면 둘 다 같은 값이 되기 쉽고, 도구는 성공을 반환하므로 고쳤다고 착각한다. 치환 후에는 **다른 문자열로 no-op 프로브**를 걸어 실제 저장 형태를 확인할 것
