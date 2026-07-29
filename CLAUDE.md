@AGENTS.md

## Claude 전용
- 구현 시작 전 [`README.md`](README.md)와 `@AGENTS.md`의 해당 규칙을 먼저 읽을 것. 스펙 문서(있으면 `docs/specs/`)나 관련 ADR(`docs/adr/`)이 있으면 함께 확인
- 새로 배운 프로젝트 컨벤션은 임의 적용하지 말고 제안 후 AGENTS.md에 반영

## 로컬 검증 (AGENTS.md §6 보완)
- lefthook 훅이 설치되어 있다면(`.git/hooks/pre-commit` 존재) 커밋 시 `spotlessApply`가 자동 적용되고 Checkstyle 경고가 출력된다. 새 클론·새 워크트리에서는 `lefthook install`을 먼저 실행할 것 — 미설치 상태면 아무 검증도 걸리지 않아 CI `spotlessCheck`에서 터진다
- 커밋 전 CI 게이트 재현: `./gradlew spotlessApply && ./gradlew build -x test && ./gradlew checkstyleMain -PciStrict`
- ⚠️ **게이트를 `| tail`·`| head` 같은 파이프에 물리지 말 것.** 파이프라인 종료코드가 마지막 명령의 것이 되어 gradle 실패가 가려진다. 실측: `./gradlew build -q 2>&1 | tail -15 && echo OK` → **컴파일 에러 5건인데 `OK`가 출력됐다.** 출력을 줄이려면 파이프 대신 `-q`만 쓰고 `set -e`로 각각 실행한다
- **`src/test`가 아직 없다.** `./gradlew test`는 통과해도 검증된 것이 없다 — "테스트 통과"로 보고하지 말 것. 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐
- 스펙 문서는 [`docs/specs/api-list.md`](docs/specs/api-list.md) 하나뿐이며 **Notion API I/F의 파생 요약이다** — 원본이 아니다. `docs/adr/`는 비어 있고 **ADR-001·ADR-002의 원본은 Notion에 있다.** 설계 판단 전에 AGENTS.md §0(출처 우선순위)을 먼저 볼 것

## Notion 편집 함정 (AGENTS.md §0 보완)

설계 원본이 Notion에 있어 편집이 잦다. 아래 3건은 모르면 증상을 오진한다.

- **「설계」 섹션의 DB·API 탭은 API로 수정할 수 없다.** `notion-update-page`가 `validation_error: Object ... is not a page or database`로 거부한다. `fetch`로 읽히고 URL도 페이지처럼 생겼지만 쓰기가 안 되는 객체(탭)다. **읽기는 되는데 쓰기만 막히면 권한이 아니라 객체 타입을 의심할 것.** 우회: 같은 내용이 실린 일반 페이지를 고치고, 탭 안의 원본은 사람이 직접 수정해야 한다
- **코드블록이 이스케이프 문자열을 그대로 저장하고 있을 수 있다.** 「소스 구조」 §2 트리가 박스문자·한글을 `├`, `도` 같은 **텍스트로** 담고 있었다(노션에서도 깨져 보인다). 이 상태에서는 `update_content` 부분 치환이 불가능하다 — `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없어(항상 실제 문자로 디코딩된다) 매칭 자체가 안 된다. 박스문자는 ASCII 마커 2단계 치환으로 우회되지만 한글은 조합 수가 많아 비현실적이다. **해법은 `replace_content` 전체 교체 하나뿐.** 부분 치환이 계속 `No matches found`면 저장 형태를 의심할 것
- **`old_str`과 `new_str`을 같은 문자열로 쓰면 성공은 하지만 아무것도 안 바뀐다.** 이스케이프를 다루다 보면 둘 다 같은 값이 되기 쉽고, 도구는 성공을 반환하므로 고쳤다고 착각한다. 치환 후에는 **다른 문자열로 no-op 프로브**를 걸어 실제 저장 형태를 확인할 것
