# PLAN-REQ-22 · 사진 자유 태그(FR-GAL-03) + 월별 대표 사진 성장 앨범(FR-GAL-04)

> 출처: 현재 세션 대화 · 작성: 2026-09-22 · 상태: ✅ 완료

## 배경

`docs/TODO.md`의 남은 갭 중 FR-GAL-03("태그 필터 — 탈피 / 핸들링 / 일상 / 병원 등")과 FR-GAL-04("성장 앨범 — 월별 대표 사진 수동 지정 또는 자동 선택")를 묶어 하나의 REQ로 진행하기로 했다(사용자 확정, 2026-09-22).

현재 `photos` 테이블(`Photo` 엔티티)엔 `id`·`pet_id`·`diary_entry_id`·`image_url`·`caption`·`taken_date`·`created_at`뿐이고 태그·대표사진 개념이 전혀 없다. 더 근본적으로 **`PATCH /pets/{pet_id}/photos/{photo_id}` 자체가 존재하지 않는다** — `PhotoController`는 `POST`(생성)·`GET`(목록)·`DELETE`뿐이라, 사진을 만든 뒤 메타데이터를 고치는 경로가 하나도 없다. FR-GAL-04가 요구하는 "수동 지정"(대표 사진을 나중에 바꾸는 것)이 성립하려면 이 수정 경로부터 새로 설계해야 한다.

Notion `비즈니스 규칙` DB에 자동 선택 기준이 이미 정해져 있다 — **BR-GAL-04**: "해당 월 첫 번째 업로드 사진을 기본 대표 사진으로 지정. 수동 변경 가능." 이 문서가 대표 사진 자동 규칙의 원본이다.

태그 값 집합은 대화 중 두 번 갈렸다 — 처음엔 "고정 enum 4개(탈피/핸들링/일상/병원)"로 확정했다가, 사용자가 직접 "사진 태그 값 집합은 사용자가 입력하는 걸로. 인스타 해시태그처럼(공백 제거해서 처리) — 사용자가 항목을 미리 설정하는 게 아니라 그때그때 다는 것처럼"으로 뒤집었다. **최종은 자유 태그**다(아래 `## 결정`).

## 범위

**포함**

- `photos`에 **자유 태그**(사용자가 그때그때 입력, 고정 목록 없음) — 사진 하나에 여러 개 부착 가능(인스타 해시태그 유비)
- `GET /pets/{pet_id}/photos`에 태그 필터 쿼리 파라미터 추가(기존 엔드포인트 계약 보강)
- **월별 대표 사진** — 자동(그 달 첫 업로드) + 수동 지정(BR-GAL-04 그대로)
- 신설 **`PATCH /pets/{pet_id}/photos/{photo_id}`** — `caption`·`taken_date`·`tags`·`is_representative` 부분 수정. 이 REQ 이전엔 이 경로 자체가 없었다
- 신설 **`GET /pets/{pet_id}/photos/growth-album`** — 월별 대표 사진만 모은 목록(시간순)
- `POST /pets/{pet_id}/photos`(생성)에도 선택적 `tags` 필드 추가 — 업로드 시점에 바로 태그를 달 수 있게(인스타 유비)
- Notion `API I/F`에 신규 행(`PATCH` · `GET .../growth-album`) 선반영 + 기존 "사진 목록" 행에 태그 필터 계약 보강 — REQ-20 Phase 0과 같은 순서(코드보다 Notion 먼저)

**제외**

- 프론트엔드 렌더링(해시태그 입력 UI, 성장 앨범 화면) — 이 저장소는 백엔드 전용
- 태그 자동완성·추천, 태그별 통계·인기 태그 — 대화에 없음, FR-GAL-03 원문에도 없음
- 여러 태그를 AND/OR로 조합하는 복합 필터 — Notion 원문은 "태그 필터"(단수 동작)뿐이라 단일 태그 필터만 한다. 필요해지면 별도로 다시 제안
- `PATCH`에서 `tags`의 부분 추가/삭제(append/remove) — 이번엔 **전체 교체**만 한다(아래 결정)
- FR-GAL-06("사진 상세 뷰에서 해당 날짜 일지로 바로 이동") — 이번 조사 중 우연히 발견했지만 사용자가 요청한 범위(FR-GAL-03·04)가 아니라 건드리지 않는다

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| 태그 값 집합 | **자유 텍스트**, 사용자가 그때그때 입력 | 사용자가 "인스타 해시태그처럼, 미리 설정하는 게 아니라 그때그때 다는 것"으로 명시적으로 뒤집음(대화 확정, 2026-09-22) | 고정 enum 4개(탈피/핸들링/일상/병원) — 처음엔 이걸로 확정했으나 사용자가 직접 기각. `ConditionTag`(diary)와 같은 레포 관례였지만 이번 요구와 안 맞음 |
| 태그 개수 | 사진 1건에 **여러 개** 허용 | "인스타 해시태그처럼"이라는 유비 — 인스타 게시물은 해시태그를 여러 개 붙이는 게 일반적(추정, 명시적 확인은 안 받음) | 사진당 태그 1개 — 유비와 안 맞아 기각 |
| 태그 정규화 | 각 태그 문자열의 **앞뒤·내부 공백을 제거**해 저장 | 사용자 지시("공백 제거해서 처리") 그대로 | 공백 유지 — 명시적으로 기각됨 |
| 태그 저장 구조 | 신규 자식 테이블 `photo_tags(id, photo_id, tag)` | 사진 1건 : 태그 N건이라 컬럼 하나에 욱여넣으면(콤마 구분 등) 태그 단위 필터·유니크 제약이 안 됨. 이 레포의 기존 패턴(petId를 UUID 컬럼으로만 두고 `@ManyToOne` 안 씀)을 그대로 따라 `photoId`도 UUID 컬럼으로만 둠 | 콤마 구분 단일 컬럼 — 필터 성능·중복 방지 둘 다 약해져 기각 |
| 태그 길이 제한 | `varchar(50)` | AGENTS §5 "길이 제약은 생략하지 말 것" — 없으면 초과 입력이 500으로 샌다. 50자는 다른 자유 텍스트 필드(`caption` 500자)보다 훨씬 짧게 잡음(해시태그 성격상 짧은 단어가 자연스러움, 추정) | 무제한 — DB 예외가 500으로 새는 걸 막기 위해 기각 |
| 태그·대표사진 수정 API | **`PATCH /pets/{pet_id}/photos/{photo_id}` 신설**, `caption`·`taken_date`·`tags`·`is_representative` 전부 이 한 엔드포인트로 | 사용자 확정(대화). 다른 record 도메인(weight·shed·feeding·activity·environment)과 같은 PATCH 관례를 photos에도 맞춤 | 태그는 생성 시에만, 대표사진만 별도 액션 엔드포인트 — 기각(대화 확정) |
| `tags` PATCH 의미론 | **전체 교체** — 보낸 배열로 기존 태그 셋을 통째로 대체. `null`(필드 누락)이면 변경 없음, `[]`(빈 배열)이면 전체 삭제 | AGENTS §5 PATCH 관례("누락·`null` = 변경 없음")의 연장. 부분 추가/삭제 API는 이번 범위 밖(위 `## 범위 — 제외`) | 태그별 추가/삭제 API — 범위 밖으로 기각 |
| 대표 사진 저장 방식 | `photos.is_representative`(boolean) 컬럼. **자동 기본값은 저장하지 않고 조회 시 계산** — 그 달에 수동 지정된 사진이 없으면 그 달 사진 중 가장 이른 것(`taken_date` 우선, 없으면 `created_at`)을 기본값으로 취급 | "저장하지 않고 조회 시 계산" 원칙을 이 레포가 이미 여러 곳(weight_change_rate·거식 스트릭·탈피 예측·environment daily-summary)에서 쓰고 있음. 자동 규칙을 매번 백필하지 않아도 되는 이점 | 첫 업로드 시 자동으로 `is_representative=true`를 써 넣기 — 나중에 그 달에 더 이른 `taken_date` 사진이 소급 추가되면 규칙이 깨지는데 트리거로 재계산해야 해 복잡해져 기각 |
| 대표 사진 유일성 | `pet_id` + 월 단위로 **수동 대표는 최대 1건**. `is_representative=true`로 PATCH하면 같은 달의 기존 대표(있다면)를 서비스 계층에서 함께 `false`로 되돌린다 | BR-GAL-04("월별 대표 사진")가 달마다 하나를 전제. DB 제약(월은 저장 컬럼이 아니라 파생값이라 유니크 인덱스로 못 건다)이 아니라 앱 레이어에서 강제 — AGENTS §5의 "enum도 앱 레이어에서만 검증" 관례와 같은 결 | DB 부분 유니크 인덱스로 강제 — 월이 저장 컬럼이 아니라(파생) 인덱스 대상이 될 수 없어 기각 |
| 월 그룹 기준 | `taken_date` 우선, 없으면 `created_at`의 **KST 달력 날짜**(`TimeConstant.KST`)로 대체 | `taken_date`가 nullable이라 기준이 필요함. REQ-16/ADR-0002 "달력 판정은 Asia/Seoul"을 그대로 적용 | `created_at`만 사용 — 촬영일과 업로드일이 다를 수 있어(과거 사진 소급 업로드) `taken_date`를 우선하는 게 "성장" 의미에 더 맞아 기각 |
| 대상 종 | **전종 공통** | `photos`·`PhotoService`엔 원래 종 제약이 없음. FR-GAL-04 유저 스토리(US-GAL-02)의 페르소나가 "게코"이긴 하나 명시적 제한 문구는 없음(대화 확정) | 게코 전용 — 기각(대화 확정) |
| 성장 앨범 정렬 | **시간순(오래된 달 → 최근 달)** | "성장 앨범"·"시간에 따른 외형 변화를 한눈에" (US-GAL-02)라는 표현이 성장 추이를 보는 용도임을 시사 — 다른 목록(최신순)과 다른 순서가 자연스러움(추정) | 최신순(다른 목록과 통일) — "성장"이라는 목적과 안 맞아 기각 |
| 성장 앨범 페이지네이션 | 없음 — 전체를 한 번에 반환 | REQ-12 timeline과 같은 이유: 월 단위 집계라 건수가 자연히 작음(반려동물 수명 동안 수백 개월을 넘기 어려움) | 커서 페이지네이션 — 건수가 작아 불필요하다고 판단, 기각 |
| Notion 역반영 시점 | 구현 착수 전에 먼저 — Phase 0 | REQ-20과 같은 이유(AGENTS §0, API 계약 원본은 Notion) | 구현 후 역반영 — REQ-11·17에서 겪은 지연 전례 반복 안 함 |

## 미결 질문

- [ ] **태그 길이(50자) 초과 입력을 400으로 거부할지** — `/testgen`·`/implement` 양쪽이 제기. AGENTS §5는 "`@Size` 없으면 초과 입력이 500으로 샌다"고 경고하지만, `List<String> tags`에 요소별 Bean Validation(`List<@Size(max=50) String>`)을 거는 방식이 이 레포 Hibernate Validator 버전에서 확실히 동작하는지 이번 세션에서 확신이 안 서 구현·테스트 둘 다 보류했다. 현재는 50자 초과 태그를 보내면 DB `varchar(50)` 제약에서 `DataIntegrityViolationException`(500)으로 샐 것으로 추정(미실측)

이 REQ의 최초 3건(태그 값 집합·API 모양·대상 종)은 `AskUserQuestion`으로, 나머지(개수·정규화 세부·월 그룹 기준·정렬·페이지네이션)는 대화 근거·레포 기존 관례로 추정 결정했다(`## 결정` 표에 "추정" 표기).

## 작업 단계

- [x] **Phase 0 — Notion API I/F 선반영**
      완료 기준: `API I/F` DB에 신규 행 2개 추가 — `PATCH /pets/{pet_id}/photos/{photo_id}`(caption·taken_date·tags·is_representative) · `GET /pets/{pet_id}/photos/growth-album`(월별 대표 사진, 시간순). 기존 "사진 목록"(`GET /pets/{pet_id}/photos`) 행에 `tag` 쿼리 파라미터 계약 추가. `fetch` 재조회로 반영 확인
      → 2026-09-22 완료. 신규 2행 + 기존 2행(사진 목록·사진 메타데이터 저장) 보강, `fetch` 재조회로 콜아웃·코드블록 정상 렌더 확인(REQ-20·21에서 겪은 이스케이프 함정 재발 없음)

- [x] **Phase 1 — 사진 자유 태그 + 월별 대표 사진**
      완료 기준: `V7__photo_tags.sql` 적용(`photo_tags` 테이블 신설 + `photos.is_representative` 컬럼 추가) · `POST /photos`가 `tags` 배열을 받아 저장 · `PATCH /photos/{id}` 신설, `caption`·`taken_date`·`tags`(전체 교체)·`is_representative`(같은 달 기존 대표 자동 해제) 반영 · `GET /photos`가 `tag` 쿼리 파라미터로 필터링 · `GET /photos/growth-album`이 월별 대표(수동 우선, 없으면 그 달 최이른 사진)를 시간순으로 반환 · 태그 문자열은 공백 제거 후 저장, 빈 문자열이 되면 무시 · 컨트롤러 테스트(AGENTS §6 관례) 통과
      → 2026-09-22 완료. `/testrun REQ-22`에서 검증 계약 25건 전부 1차 통과 확인, 전체 스위트(396건) 회귀 없음(REQ-22 범위 밖 실패 2건은 Testcontainers 환경변수 미설정 — REQ-22와 무관함을 직접 재확인). 커밋 `8cfacf8` · PR #58(스쿼시)로 `main` 병합 완료(`b9a4139`) — 머지 직전 CI가 spotless 포맷 위반(사후 주석 수정 후 `spotlessApply` 재실행 누락)으로 한 번 막혀 `fix(req22)` 커밋으로 정정

## 검증 계약

> 작성: 2026-09-22 · 대상: Phase 1 착수 직전 · 검증: `/testrun REQ-22`
> Phase 0(Notion API I/F 선반영)은 코드가 아니라 문서 작업이라 케이스로 옮기지 않았다 — 완료 기준 자체(신규 행·기존 행 보강, `fetch` 재조회로 확인)가 검증 방법이다.
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-22-01]` 형태로 박는다.
> ⚠️ REQ-22-23·25는 정확히는 이번 `/testgen`이 Notion(Phase 0에서 이미 반영, 로컬 grep 불가)에 맞춰 설계한 응답 스키마를 근거로 한다 — 다른 케이스처럼 이 계획서 문구를 그대로 grep해 찾을 수 있는 인용이 아니다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-22-01 | `V7__photo_tags.sql` | `photo_tags` 테이블을 생성한다 | 회귀(구조) | PLAN §결정 표 '태그 저장 구조' — "신규 자식 테이블 `photo_tags(id, photo_id, tag)`" | 1 | ✅ |
| REQ-22-02 | `V7__photo_tags.sql` | `photo_tags.tag`는 `varchar(50)`이다 | 회귀(구조) | PLAN §결정 표 '태그 길이 제한' — "`varchar(50)`" | 1 | ✅ |
| REQ-22-03 | `V7__photo_tags.sql` | `photos`에 `is_representative` 컬럼을 추가한다 | 회귀(구조) | Phase 1 완료 기준 — "`photos.is_representative` 컬럼 추가" | 1 | ✅ |
| REQ-22-04 | `PhotoTagNormalizer` | 태그 앞뒤 공백을 제거한다 | 정상 | PLAN §결정 표 '태그 정규화' — "앞뒤·내부 공백을 제거해 저장" | 1 | ✅ |
| REQ-22-05 | 〃 | 태그 내부 공백을 제거한다 | 정상 | 〃 | 1 | ✅ |
| REQ-22-06 | 〃 | 공백 제거 후 빈 문자열이 되면 무시한다 | 경계 | Phase 1 완료 기준 — "태그 문자열은 공백 제거 후 저장, 빈 문자열이 되면 무시" | 1 | ✅ |
| REQ-22-07 | 〃 | 중복 태그는 한 번만 남는다 | 불변식 | PLAN §결정 표 '태그 저장 구조' — "필터 성능·중복 방지 둘 다 약해져 기각"(child table을 택한 이유가 중복 방지) | 1 | ✅ |
| REQ-22-08 | `PhotoService.create` | 보낸 `tags`를 저장한다 | 정상 | Phase 1 완료 기준 — "`POST /photos`가 `tags` 배열을 받아 저장" | 1 | ✅ |
| REQ-22-09 | 〃 | `tags` 없이 생성하면 태그를 저장하지 않는다 | 경계 | PLAN §범위 포함 — "선택적 `tags` 필드 추가" | 1 | ✅ |
| REQ-22-10 | `PhotoService.update` | `caption`만 보내면 `caption`만 바뀌고 `taken_date`는 유지된다 | 정상 | Phase 1 완료 기준 — "`PATCH /photos/{id}` 신설, `caption`·`taken_date`·... 반영" | 1 | ✅ |
| REQ-22-11 | 〃 | `tags`를 누락하면 기존 태그를 건드리지 않는다 | 정상 | PLAN §결정 표 '`tags` PATCH 의미론' — "`null`(필드 누락)이면 변경 없음" | 1 | ✅ |
| REQ-22-12 | 〃 | `tags`가 빈 배열이면 기존 태그를 전부 삭제한다 | 경계 | PLAN §결정 표 '`tags` PATCH 의미론' — "`[]`(빈 배열)이면 전체 삭제" | 1 | ✅ |
| REQ-22-13 | 〃 | `tags`를 보내면 기존 태그를 전체 교체한다 | 정상 | PLAN §결정 표 '`tags` PATCH 의미론' — "보낸 배열로 기존 태그 셋을 통째로 대체" | 1 | ✅ |
| REQ-22-14 | 〃 | `is_representative=true`면 같은 달의 기존 대표는 해제된다 | 불변식 | PLAN §결정 표 '대표 사진 유일성' — "같은 달의 기존 대표(있다면)를 서비스 계층에서 함께 `false`로 되돌린다" | 1 | ✅ |
| REQ-22-15 | 〃 | 존재하지 않는 `photo_id`는 `RESOURCE_NOT_FOUND`다 | 예외 | PLAN §제약·함정 — "(REQ-10 D6 관례 — 기록 조회는 반드시 `pet_id`를 함께 건다)" | 1 | ✅ |
| REQ-22-16 | `PhotoService.list` | `tag`로 필터링하면 그 태그를 가진 사진만 반환한다 | 정상 | Phase 1 완료 기준 — "`GET /photos`가 `tag` 쿼리 파라미터로 필터링" | 1 | ✅ |
| REQ-22-17 | 〃 | `tag` 파라미터 없이 호출하면 기존 목록 동작 그대로다 | 회귀 | PLAN §범위 포함 — "기존 엔드포인트 계약 보강"(대체 아님) | 1 | ✅ |
| REQ-22-18 | `PhotoService.getGrowthAlbum` | 수동 지정된 대표가 있으면 그것을 반환한다 | 정상 | PLAN §결정 표 '대표 사진 저장 방식' — "그 달에 수동 지정된 사진이 없으면... 기본값으로 취급"(반대해석) | 1 | ✅ |
| REQ-22-19 | 〃 | 수동 지정이 없으면 그 달 가장 이른 사진을 반환한다 | 정상 | PLAN §결정 표 '대표 사진 저장 방식' — "그 달 사진 중 가장 이른 것(`taken_date` 우선, 없으면 `created_at`)을 기본값으로 취급" | 1 | ✅ |
| REQ-22-20 | 〃 | `taken_date`가 없으면 `created_at`의 KST 날짜로 월을 판단한다 | 회귀 | PLAN §결정 표 '월 그룹 기준' — "`created_at`의 KST 달력 날짜(`TimeConstant.KST`)로 대체" | 1 | ✅ |
| REQ-22-21 | 〃 | 여러 달이 있으면 오래된 달부터 반환한다 | 정상 | PLAN §결정 표 '성장 앨범 정렬' — "시간순(오래된 달 → 최근 달)" | 1 | ✅ |
| REQ-22-22 | `PATCH /photos/{id}` | 200을 반환한다 | 정상 | Phase 0 완료 기준 — "`PATCH /pets/{pet_id}/photos/{photo_id}`(caption·taken_date·tags·is_representative)" | 1 | ✅ |
| REQ-22-23 | 〃 | 응답에 `tags`·`is_representative` 키가 있다 | 정상 | Notion API I/F 「사진 수정」 행(Phase 0 반영, 로컬 grep 불가) | 1 | ✅ |
| REQ-22-24 | `GET /photos/growth-album` | 200을 반환한다 | 정상 | Phase 0 완료 기준 — "`GET /pets/{pet_id}/photos/growth-album`(월별 대표 사진, 시간순)" | 1 | ✅ |
| REQ-22-25 | 〃 | 응답 각 항목에 `year_month`·`photo` 키가 있다 | 정상 | Notion API I/F 「성장 앨범」 행(Phase 0 반영, 로컬 grep 불가) | 1 | ✅ |

## 제약·함정

- **Phase 0이 Phase 1의 전제다.** AGENTS §0 원칙대로 API 계약이 코드보다 먼저 Notion에 있어야 한다
- **PATCH의 `tags`는 `null`(누락)과 `[]`(빈 배열)을 구분해야 한다** — `null`은 "변경 없음", `[]`는 "전체 삭제"다. record 필드를 그냥 `!= null`로만 체크하면 이 둘을 구분할 수 있다(Java `List`는 빈 리스트와 `null`이 별개 값이라 자연스럽게 구분됨) — 다만 구현 시 "빈 배열이면 기존 태그를 지운다"는 분기를 빠뜨리기 쉬우니 주의
- **월 그룹 계산에서 `created_at`을 쓸 땐 KST 달력 날짜로 변환해야 한다.** `TimeConstant.KST` 없이 그냥 `OffsetDateTime`의 날짜만 뽑으면 REQ-16 D4가 경고한 "UTC 자정 근처 어긋남"이 그대로 재발한다
- **대표 사진 유일성은 DB 제약이 아니라 서비스 로직이 지킨다** — PATCH로 `is_representative=true`를 설정하는 트랜잭션 안에서 같은 달의 기존 대표를 함께 `false`로 갱신해야 한다. 빠뜨리면 한 달에 대표가 여러 개 생겨도 에러 없이 조용히 통과한다
- **`photo_tags`는 `photos`처럼 `petId`가 아니라 `photoId`만 갖는다** — 펫 소유권은 `photos` 테이블에서 이미 검증되므로 `photo_tags`에 다시 `pet_id`를 중복시키지 않는다(정규화). 다만 태그로 필터링할 때는 `photo_tags`와 `photos`를 조인해 `pet_id`를 함께 걸어야 한다(REQ-10 D6 관례 — 기록 조회는 반드시 `pet_id`를 함께 건다)
