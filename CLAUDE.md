@AGENTS.md

## Claude 전용
- 구현 시작 전 [`README.md`](README.md)와 `@AGENTS.md`의 해당 규칙을 먼저 읽을 것. 스펙 문서(있으면 `docs/specs/`)나 관련 ADR(`docs/adr/`)이 있으면 함께 확인
- 새로 배운 프로젝트 컨벤션은 임의 적용하지 말고 제안 후 AGENTS.md에 반영

## 로컬 검증 (AGENTS.md §6 보완)
- lefthook 훅이 설치되어 있다면(`.git/hooks/pre-commit` 존재) 커밋 시 `spotlessApply`가 자동 적용되고 Checkstyle 경고가 출력된다. 새 클론·새 워크트리에서는 `lefthook install`을 먼저 실행할 것 — 미설치 상태면 아무 검증도 걸리지 않아 CI `spotlessCheck`에서 터진다
- 커밋 전 CI 게이트 재현: `./gradlew spotlessApply && ./gradlew build -x test && ./gradlew checkstyleMain -PciStrict`
- **`src/test`가 아직 없다.** `./gradlew test`는 통과해도 검증된 것이 없다 — "테스트 통과"로 보고하지 말 것. 현재 실질 게이트는 컴파일 + Spotless + Checkstyle뿐
- `docs/specs/`·`docs/adr/`는 아직 생성되지 않았다 (README의 ADR-001은 README 본문에만 존재)
