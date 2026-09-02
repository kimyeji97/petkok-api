@AGENTS.md

## Claude 전용
- 구현 시작 전 [`README.md`](README.md)와 `@AGENTS.md`의 해당 규칙을 먼저 읽을 것. 스펙 문서(있으면 `docs/specs/`)나 관련 ADR(`docs/adr/`)이 있으면 함께 확인
- 새로 배운 프로젝트 컨벤션은 임의 적용하지 말고 제안 후 AGENTS.md에 반영

## 개발 플로우 (AGENTS.md §4 보완)

AGENTS.md §4의 "계획서 → 검증 계약 → 구현 → 판정 → 기록" 순서는 Claude Code에서 **슬래시 커맨드 여섯 개**(`/workplan → /testgen → /implement → /testrun → /checkpoint → /progress`)로 돈다. **커맨드별 책임·금지 표와 판정 독립성 근거는 사용자 전역 `CLAUDE.md`에 있다** — 프로필(`$CLAUDE_CONFIG_DIR`)의 `CLAUDE.md`가 `~/my_sources/_init/claude-commands/CLAUDE.md`로 링크돼 매 세션 자동 로드된다(2026-09-02 승격 → 같은 날 전역으로 이관). 여기에는 **이 레포에서만 다른 것**만 적는다.

- **`/testgen`은 Phase 착수 직전에 그 Phase 분만 쓴다.** Java는 대상 클래스가 없으면 **테스트 소스 전체가 컴파일되지 않아** `main`이 빨간불이 된다(REQ-08·09·10·16 실측). 다른 언어보다 이 제약이 세다
- **계획서 경로는 `docs/plans/PLAN-REQ-NN-*.md`**, 미결의 원본은 Notion이다(AGENTS §0). `/workplan`이 미결로 남긴 것은 레포 문서로 답하지 말고 Notion에 먼저 적힌 뒤 옮긴다
- ⚠️ **커맨드 정의와 전역 계약은 이 레포에 없다.** `claude-commands` 저장소(`~/my_sources/_init/claude-commands`)가 실체이고 프로필은 심링크다. 새 머신에서는 그 저장소를 `clone` 후 `./install.sh`를 먼저 돌린다. 커맨드가 없으면 AGENTS §4의 도구 중립 원칙 셋(테스트가 구현보다 먼저 · Phase 1개 = 커밋 1개 · 판정하는 손은 구현을 고치지 않는다)을 손으로 지킨다

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
	- ⚠️ **프로브를 되돌릴 때 `git checkout <파일>` 을 쓰지 말 것 — 그 파일의 미스테이지 변경을 함께 삼킨다.** 인덱스(=HEAD)에서 복원하므로 **심은 위반뿐 아니라 아직 커밋 안 한 구현이 통째로 사라진다.** 에러도 경고도 없고 복원된 파일은 "원래대로"로 보인다. 2026-08-31 실측 — REQ-16 Phase 3 의 `Clock` 주입 전체를 이렇게 날려 재작성했다. **이 레포는 프로브를 상시 "심었다 지우는" 방식으로 돌리므로**(REQ-09·10·16) 이 조합을 반복해서 만난다. 되돌리기는 **`cp` 백업**으로 하거나, 프로브 전에 커밋해 둔다
	- **예외(`ignoreDependency`)를 추가할 때는 반대 방향도 잰다 — 예외를 걷어낸 원본 규칙에서 정상 사용이 FAIL 인지.** PASS 만 확인하면 "예외를 넣었는데 원래부터 통과하던 것"과 구별되지 않는다(2026-08-28 REQ-10 Phase 0 — `git stash` 로 규칙 변경을 걷어내고 같은 프로브를 한 번 더 돌려 FAIL 확인)
- ⚠️ **`--tests` 패턴은 glob `*`·`?`만 안다. 문자 클래스(`[56]`)·중괄호·정규식을 쓰면 0건 매칭이고, 그게 `BUILD SUCCESSFUL`로 나온다.** 위 프로브에서 이게 가장 위험하다 — 결과를 "통과"가 아니라 **"규칙이 결함을 못 잡는다"는 반대 결론**으로 읽게 만든다. 2026-08-10 실측: `--tests '*req_09_1[56]*'`이 초록불이라 핸들러가 무력한 줄 알았는데, 클래스명으로 다시 걸자 두 케이스 모두 정상 발화했다
	- **여러 케이스를 한 번에 돌리려면 클래스명으로 거른다** — `--tests '*PetControllerWebMvcTest'`. 케이스 단위가 필요하면 `--tests` 를 여러 번 나열한다
	- **실행 건수는 `build/test-results/test/*.xml`로 확인한다.** 콘솔에는 0건과 전건 통과가 똑같이 보인다
		- ⚠️ **XML 의 `testcase name` 은 메서드명이 아니라 `@DisplayName` 이다.** `req_09_12` 로 grep 하면 0건이고, 그게 "안 돌았다"로 읽힌다(2026-08-27 실측 — 실제로는 9건 전부 실행). 케이스를 셀 때는 `\[REQ-09-1[23]\]` 처럼 **DisplayName 의 ID** 로 센다
- ⚠️ **세거나 찾는 명령은 "패턴이 비지 않았는가"를 먼저 확인한다. 빈 패턴은 0건이 아니라 전건 매치다.** (`grep -F ""` → 모든 줄, `git grep -l ""` → 모든 파일). 변수에서 패턴을 뽑을 때 그 변수가 비면 검사가 **정반대 결론**을 낸다 — 2026-08-28 실측: 주석 처리된 `JWT_SECRET` 을 `grep '^JWT_SECRET='` 로 뽑아 빈 값이 됐고, 시크릿 유출 검사가 **168건**을 보고했다(실제 0건). 앞의 `--tests` 0건 매칭과 짝이다 — 한쪽은 없는 것을 "통과"로, 다른 쪽은 없는 것을 "전부 위반"으로 보여 준다
	- **검사기를 만들면 실제로 존재하는 문자열로 역프로브해 검사기가 살아 있는지 함께 확인한다.** 구조 규칙에 일부러 위반을 심어 보는 것과 같은 이유다 — 0건이 "깨끗함"인지 "검사가 고장남"인지 결과만 봐서는 구별되지 않는다
- **앱을 띄우려면 `.env`가 필요하다** — `set -a && . ./.env && set +a && ./gradlew bootRun`. `DB_PASSWORD`에는 기본값이 없어 주입하지 않으면 기동이 막힌다(의도적)
	- ⚠️ **`.env`의 `KEY=`(빈 값)은 "미설정"이 아니다.** 빈 문자열이 환경변수로 들어가고 Spring의 `${VAR:기본값}`은 **미정의일 때만** 기본값을 쓴다 — 빈 값이면 기본값이 무력화된다. 기본값을 쓰려면 그 줄을 주석 처리할 것
- ⚠️ **문서에 실제 시크릿을 쓰지 말 것.** 함정·재현 로그를 `PROGRESS.md`에 적다가 실제 로컬 DB 비밀번호가 public 레포에 커밋된 적이 있다(2026-07-29 `5c7313b`). 재현 상황을 설명할 때는 값을 더미로 바꾼 뒤 적는다
- `docs/specs/`의 두 문서 — [`api-list.md`](docs/specs/api-list.md)(Notion API I/F 파생), [`db-schema.md`](docs/specs/db-schema.md)(Notion 테이블 정의서 파생) — 는 **둘 다 파생 요약이다.** 원본이 아니다. **ADR-001·ADR-002(스택·DB 엔진)의 원본은 Notion에 있고**, 그 이후 결정은 `docs/adr/ADR-0001…`(4자리)에 쌓인다. 설계 판단 전에 AGENTS.md §0(출처 우선순위)을 먼저 볼 것

## 시각 처리 (AGENTS.md §5 보완)

REQ-16 에서 확정했다(→ [ADR-0002](docs/adr/ADR-0002-time-handling-timestamptz.md)). **저장 = 순간 · 노출 = `+09:00` · 계산 = `Asia/Seoul`.**

- **신규 시각 컬럼은 `timestamptz`, 엔티티 필드는 `OffsetDateTime`.** `timestamp` + `LocalDateTime` 쌍은 **저장된 값만으로 순간이 결정되지 않는다** — 세션 TZ 에 따라 같은 값이 다른 순간이 되고, 그 어긋남은 에러 없이 나타난다. 날짜만 있는 값(`entry_date`·`shed_date`·`measured_at`·`birthday`·`adoption_date`·`taken_at`)은 예외다. 타임존 개념이 없으므로 `date`·`LocalDate` 를 유지한다
	- ⚠️ **그런데 `ddl-auto: validate` 는 컬럼 *존재*만 보고 *타입*은 보지 않는다** (2026-08-28 실측). 엔티티를 `OffsetDateTime` 으로 두고 컬럼을 `timestamp` 로 남긴 채 기동해도 **그대로 뜬다.** 검사기 자체는 살아 있다 — 컬럼을 지우면 `Schema-validation: missing column` 으로 막힌다
	- **이 두 줄은 같이 읽어야 뜻이 산다.** 위 규칙을 어겨도 기동이 안 막히는 이유가 아래 사실이다. 즉 **엔티티만 바꾸고 마이그레이션을 빠뜨리면(또는 그 반대) 조용히 통과한다.** 실제 방어선은 마이그레이션 텍스트 검사(`V3TimestamptzMigrationTest`)와 엔티티 타입 검사(`TimeFieldTypeContractTest`) 두 개뿐이고, **둘 다 SQL 과 DB 실물을 맞대보지는 않는다.** 그 구멍은 Testcontainers 도입 전까지 열려 있다(REQ-16 미결 ⑥ — 계약으로만 남기기로 확정)
- **`now` 는 `Clock` 을 주입받는다. 무인자 `now()` 를 부르지 않는다** — `LocalDateTime`·`OffsetDateTime`·`LocalDate`·`Instant`·`ZonedDateTime` 5종 모두. JVM 기본 TZ 에 암묵 의존해 배포 환경에 `TZ` 가 없으면 **에러 없이** 9시간 어긋난다. ArchUnit `REQ_16_10_NO_DIRECT_NOW` 가 `business`·`framework` 에서 강제한다(`data` 는 범위 밖 — 엔티티에는 빈을 주입할 수 없다)
- **`Asia/Seoul` 문자열은 `framework/constant/TimeConstant` 한 곳에만 둔다.** 응답 표기(`JacksonConfig`)와 요청 해석(`OffsetDateTimeDeserializer`)이 **서로 다른 두 장치**라 한 곳만 바꾸면 조용히 갈린다. `REQ-16-16` 이 소스 텍스트를 훑어 강제한다

## Notion 편집 함정 (AGENTS.md §0 보완)

설계 원본이 Notion에 있어 편집이 잦다. 아래 3건은 모르면 증상을 오진한다.

- **「설계」 섹션의 DB·API 탭은 API로 수정할 수 없다.** `notion-update-page`가 `validation_error: Object ... is not a page or database`로 거부한다. `fetch`로 읽히고 URL도 페이지처럼 생겼지만 쓰기가 안 되는 객체(탭)다. **읽기는 되는데 쓰기만 막히면 권한이 아니라 객체 타입을 의심할 것.** 우회: 같은 내용이 실린 일반 페이지를 고치고, 탭 안의 원본은 사람이 직접 수정해야 한다
	- **단, 탭 안의 데이터베이스(`API I/F` 등)에 행을 추가하는 것은 된다** — `notion-create-pages` 에 `parent.data_source_id`(`collection://…`, `fetch` 로 얻는다)를 주면 생성된다(2026-08-27 실측, `DELETE /users/me/profile-image` 행). 기존 행 본문 수정(`update-page`)도 된다. 막히는 것은 **탭 페이지 자체의 본문**뿐이다. "사람이 직접"으로 미루기 전에 대상이 페이지인지 행인지 구분할 것
- **코드블록이 이스케이프 문자열을 그대로 저장하고 있을 수 있다.** 「소스 구조」 §2 트리가 박스문자·한글을 `├`, `도` 같은 **텍스트로** 담고 있었다(노션에서도 깨져 보인다). 이 상태에서는 `update_content` 부분 치환이 불가능하다 — `old_str`에 리터럴 `\uXXXX`를 실어 보낼 방법이 없어(항상 실제 문자로 디코딩된다) 매칭 자체가 안 된다. 박스문자는 ASCII 마커 2단계 치환으로 우회되지만 한글은 조합 수가 많아 비현실적이다. **해법은 `replace_content` 전체 교체 하나뿐.** 부분 치환이 계속 `No matches found`면 저장 형태를 의심할 것
- **`old_str`에 리스트 마커(`- `)를 포함하면 매칭되지 않는다.** 리스트 항목은 본문만 콘텐츠로 취급되는 모양이다. 2026-08-31 실측 — `- 컬럼명: \`deleted_at timestamp NULL\`` 은 `No matches found`, 마커를 뺀 `` `deleted_at timestamp NULL` `` 은 즉시 통과. **`No matches found`가 나면 내용을 의심하기 전에 줄머리 기호부터 떼 볼 것**
- **`content_updates` 배열은 원자적이다 — 한 건이 실패하면 같은 배치의 나머지도 전부 적용되지 않는다.** 부분 반영이 없다는 뜻이라 안전한 쪽이지만, **배치가 실패했을 때 "일부는 들어갔겠지"로 넘기면 안 된다.** 실패한 것만 고쳐 다시 보내지 말고 **전부 다시** 보낸다 (2026-08-31 실측 — 4건 중 1건 실패로 3건이 통째로 안 들어갔다)
- **`old_str`과 `new_str`을 같은 문자열로 쓰면 성공은 하지만 아무것도 안 바뀐다.** 이스케이프를 다루다 보면 둘 다 같은 값이 되기 쉽고, 도구는 성공을 반환하므로 고쳤다고 착각한다. 치환 후에는 **다른 문자열로 no-op 프로브**를 걸어 실제 저장 형태를 확인할 것
