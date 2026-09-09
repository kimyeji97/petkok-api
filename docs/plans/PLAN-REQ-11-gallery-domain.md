# PLAN-REQ-11 · gallery 도메인 (R2 presigned 업로드)

> 출처: 2026-09-08 세션(`/progress` REQ-11/12 순서 논의 직후) · 작성: 2026-09-08 · 상태: 🟡 진행 (Phase 0 완료)

## 배경

REQ-06 API 설계 초안에서 gallery(사진) 엔드포인트 4개가 정의됐고(`docs/specs/api-list.md §9`), `photos` 테이블은 REQ-01 스켈레톤(`V1__init.sql`) 때 이미 만들어졌으며, R2 업로드 인프라(`R2Config`·`R2Properties`)도 REQ-05에서 구성됐다. 그런데 도메인 코드(`business/gallery`·`data/gallery`)가 한 번도 만들어지지 않아 REQ-11은 계속 `⏸`로 남아 있었다.

REQ-10(다이어리 포함 기록 도메인 5종)을 진행하던 중 "다이어리 ↔ 사진 연결"(`photo_ids`·`photos[]`·`photo_count`)을 다룰 차례가 왔는데, `photos` 테이블·업로드 자체가 아직 없는 도메인이라 **REQ-11로 이관**하기로 결정했다(D4, 2026-08-27 — `PLAN-REQ-10` 56행). 그 결정의 이유가 그대로 REQ-11의 진입 조건이다: diary가 `photos` 엔티티를 직접 참조하면 **도메인 간 참조**(diary → gallery)가 생겨 ArchUnit 경계 규칙에 걸리고, 그걸 푸는 설계(포트 패턴 등)는 gallery 도메인이 실제로 서야 만들 수 있다.

REQ-11을 REQ-12(timeline)보다 먼저 하기로 했다 — R2 인프라가 이미 있고 다른 도메인 의존이 없어 착수가 가장 가볍다는 게 근거다(2026-09-08 대화, 결정 절 참고).

## 범위

**포함**
- `business/gallery`·`data/gallery` 신설 — `PhotoController`·`PhotoService`·`Photo` 엔티티(기존 `photos` 테이블 매핑)·`PhotoRepository`·요청/응답 DTO
- 엔드포인트 4개(`api-list.md §9`):
  - `POST /photos/presigned-url` — R2 업로드 URL 발급 (**펫 경로 밖**). 요청에 `content_type`·`content_length`를 받아 검증(아래 결정 참고), 인증(로그인 여부)만 확인하고 pet 소유권은 검증하지 않는다
  - `GET /pets/{pet_id}/photos` — 목록 (커서, `created_at` desc)
  - `POST /pets/{pet_id}/photos` — 업로드 완료 후 메타데이터 저장. `image_url`(필수) · `caption`·`taken_at`·`diary_entry_id`(전부 선택) — 사진을 다이어리에 붙이는 것도 이 요청 시점에 한다
  - `DELETE /pets/{pet_id}/photos/{photo_id}` — 삭제. DB 행과 R2 객체 둘 다 하드 삭제
- `PetAccessGuard`로 `/pets/{pet_id}/photos` 하위 리소스 소유권 검증 (다른 도메인과 동일 패턴, D5 재사용). `POST /photos/presigned-url`은 펫 경로 밖이라 이 가드를 타지 않는다(위 참고)
- framework 포트 인터페이스(가칭 `PhotoLookup`)로 다이어리 통합 — `business/gallery`가 구현하고 `business/diary`가 주입받아 `POST /diary`의 `photo_ids`, 상세 응답의 `photos`, 목록 응답의 `photo_count`를 채운다 (현재 `DiaryResponse`에 의도적으로 없음 — REQ-10-108~110이 그 부재를 불변식으로 고정해 뒀다. 이 REQ에서 그 불변식을 뒤집는다)

**제외**
- `GET /photos/{id}`(상세) · `PATCH /photos/{id}`(캡션 수정) — Notion API I/F에 없어 REQ-06 정리 때 이미 제거된 엔드포인트다(`api-list.md` 163행). 리소스 수정은 PATCH로 통일하는 AGENTS §5 원칙과도 무관하게, **원본 자체에 이 엔드포인트가 없다**
- 캘린더 도트(월별 status 요약 + dot) — Notion 소스 구조 §9에 설계만 있고 API I/F에 엔드포인트가 없다(`api-list.md` 178행). Notion에 먼저 추가돼야 손댈 수 있다
- REQ-12(timeline) 통합 — timeline의 통합 대상은 diary·feeding·activity·weight·shed 5개뿐이고 gallery(photos)는 포함되지 않는다(`api-list.md` 169행). 별도 REQ

## 결정

| 항목 | 결정 | 근거 | 기각한 안 |
|------|------|------|-----------|
| REQ-11 vs REQ-12 순서 | REQ-11 먼저 | R2 인프라(`R2Config`/`R2Properties`) 기존재, 다른 도메인 의존 없음 — 착수 비용이 가장 낮다 | REQ-12 먼저 — "QueryDSL 활성화 시점"이라는 게이트가 실제로는 없었음이 이번에 밝혀졌지만(`api-list.md` 176행, Notion은 앱 레벨 병합을 기본 추천), 5개 도메인을 merge-cursor로 합치는 설계 난이도가 더 높아 먼저 할 이유가 없다 |
| diary→gallery 연결 존재 자체 | REQ-11 범위에 포함한다(위 범위 절) | D4가 "REQ-11로 이관"이라고 명시했고, `DiaryResponse` 코드 주석이 그 이관을 그대로 가리키고 있다 | REQ-10에 포함 — D4에서 이미 기각(도메인이 안 서 있어 참조 형태를 정할 수 없었음) |
| diary→gallery 참조 형태 | **framework 포트 인터페이스**(`PhotoLookup`) — `framework`에 선언, `business/gallery`가 구현, `business/diary`가 주입받아 사용. entity는 노출하지 않는다(petId/diaryEntryId → count·요약 DTO만) | entity 비노출, `business→framework` 단방향 유지, `UserStatusChecker`(§3, framework가 인터페이스 선언·business가 구현)와 같은 형태라 이미 검증된 패턴을 그대로 재사용한다(2026-09-08 대화) | ArchUnit 예외(D5 `PetAccessGuard` 방식 — `business.diary`가 `business.gallery.service`·`data.gallery.dto`를 직접 참조하도록 허용) — 재사용은 쉽지만 도메인 간 참조 예외가 pet 하나에서 둘로 늘어 경계가 계속 넓어진다 |
| `POST /pets/{pet_id}/photos`에서 `diary_entry_id` 시점 | 사진 업로드 요청 시점에 함께 받는다(선택 필드) | `photos.diary_entry_id` nullable FK 설계 의도(단독 갤러리 vs 일기 첨부)와 바로 맞는다. 별도 연결 흐름이 필요 없다 | 사후에 diary 쪽에서 연결 — PATCH 엔드포인트를 새로 만들어야 해 스코프가 diary API 계약(현재 PATCH 없음)까지 번진다 |
| 허용 이미지 타입·크기 | `image/jpeg`·`image/png`·`image/webp`만 허용, 최대 10MB. **HEIC/HEIF는 거부**(`UNSUPPORTED_IMAGE_TYPE`) | 대부분 브라우저(Chrome·Firefox 등)가 HEIC를 네이티브 렌더링하지 못한다(Safari 예외) — 웹에서 못 보이는 이미지가 저장되는 사고를 막는다. 변환 책임은 모바일 클라이언트(iOS/Android 모두 갤러리에서 가져올 때 JPEG로 내보내는 것이 일반적)에 둔다(2026-09-08 대화) | 서버가 HEIC→JPEG 변환 — 변환 라이브러리 도입·처리 시간이 새로 필요해 REQ-11 스코프를 넘어선다 / 제한 없음 — 임의 파일·과대 용량 업로드 위험 |
| 삭제 시 R2 객체 처리 | DB 행 + R2 객체 **둘 다 하드 삭제** | `photos`는 소프트 딜리트 대상이 아니다(AGENTS §5 — `users`·`pets`만). 남겨둘 근거가 없고 스토리지 비용만 늘어난다(2026-09-08 대화) | DB 행만 삭제 — R2에 고아 파일이 영구 축적 |
| `POST /photos/presigned-url` 소유권 검증 시점 | **인증만**(로그인 여부) — pet 소유권은 `POST /pets/{pet_id}/photos`의 `PetAccessGuard`에서 검증 | presigned 발급 자체는 DB에 아무것도 안 남겨 오남용돼도 실제 피해가 없다. 엔드포인트가 애초에 "펫 경로 밖"으로 설계돼 `pet_id`를 받지 않는다(2026-09-08 대화) | presigned 요청에도 `pet_id`를 받아 그 자리에서 검증 — "펫 경로 밖" 설계와 어긋나 엔드포인트 스펙을 바꿔야 한다 |
| 삭제 시 R2·DB 실패 순서 | **R2 객체 삭제 성공 후에만 DB 행 삭제.** R2 삭제가 실패하면 DB 행은 그대로 두고 500 반환(재시도 가능한 상태 유지) | "고아 R2 파일(응답엔 안 잡히지만 스토리지에 남음)"보다 "고아 DB 행(깨진 이미지 URL이 API 응답에 계속 나옴)"이 더 나쁜 실패 모드다. R2 삭제 성공을 먼저 확인하면 DB는 항상 실제 존재하는 파일만 가리킨다(2026-09-08 대화) | DB 먼저 삭제 후 R2 삭제 — DB 삭제는 성공하고 R2 삭제만 실패하면 API 응답(목록·상세)에서는 사라졌는데 R2엔 그대로 남아 재시도할 방법이 없어진다(참조를 잃음) |

## 미결 질문

없음 — 2026-09-08 대화에서 전부 확정(위 「결정」 표 참고).

## 작업 단계

- [x] **Phase 0** — `PhotoLookup` 포트 인터페이스·`PhotoSummary` DTO 설계
      완료 기준: `PhotoLookup`이 `framework`에, 구현체가 `business/gallery`에, 사용처가 `business/diary`에 있고 `data..entity..`(Photo 엔티티)가 포트 시그니처에 노출되지 않음을 확인
      > **결과 갱신: 2026-09-08.** `framework/gallery/PhotoLookup.java`·`PhotoSummary.java` 구현·커밋(`f97b110`, 브랜치 `feat/req11-phase0-photo-lookup-port`). **완료 기준 문구 중 "구현체가 business/gallery에, 사용처가 business/diary에"는 이번에 만들지 않았다** — 각각 Phase 1·Phase 2의 몫이라 Phase 경계를 넘기지 않으려 좁혔다(완료 기준 문구가 애초에 Phase 경계보다 넓게 쓰였던 것으로 보인다). 실제로 검증 가능했던 두 기준(엔티티 미노출·dto 네이밍 비충돌)은 `/testrun`이 기존 ArchUnit 규칙 3개(`FRAMEWORK_MUST_NOT_KNOW_DOMAIN`·`DomainBoundaryTest.NO_CROSS_DOMAIN_DEPENDENCY`·`DTO_NAMING`) 재실행으로 전부 그린 확인했다.
      > **재정정: 2026-09-09.** `framework/gallery`라는 패키지명 자체가 지적받았다 — `gallery`는 도메인 이름이라 framework 트리 안에 도메인이 새어든 것처럼 보인다(`UserStatusChecker`가 관심사 이름인 `framework/security`에 있는 것과 다름). `framework/port`로 옮겼다(같은 커밋 브랜치에서 후속 커밋).
- [ ] **Phase 1** — gallery CRUD 단독 구현 (presigned 발급 · 목록 · 생성 · 삭제), diary 통합 제외
      완료 기준: 4개 엔드포인트 정상 동작 · `PetAccessGuard`로 403/404 검증(단, presigned 발급은 인증만) · `image/jpeg`·`png`·`webp` 외 타입과 10MB 초과 요청이 `UNSUPPORTED_IMAGE_TYPE`/`FILE_TOO_LARGE`로 거부됨 · 삭제 시 R2 객체 삭제가 성공해야 DB 행이 지워지고, R2 삭제 실패 시 DB는 그대로 둔 채 500 반환 · 커서 페이지네이션(`created_at` desc) keyset 유지 · `@WebMvcTest` 관례(`@Import({SecurityConfig.class, JacksonConfig.class})`) 적용
- [ ] **Phase 2** — diary 통합 (`PhotoLookup` 포트로 `photo_ids`·`photos`·`photo_count` 반영)
      완료 기준: `business/diary`가 `PhotoLookup`(framework 인터페이스)만 참조하고 `data..gallery..entity..`를 직접 참조하지 않음(ArchUnit 확인) · REQ-10-108/109/110 세 케이스의 기존 단언(없음/무시)을 뒤집는 새 계약으로 교체하고 `/testgen`이 그 갱신을 반영

## 제약·함정

- **`idx_photos_pet_created_at (pet_id, created_at DESC)` 인덱스에 `id`가 없다.** 다른 기록 도메인(D8)처럼 `created_at` 동시각 tie-break가 필요하면 `ORDER BY created_at DESC, id DESC`는 기능적으론 되지만 이 인덱스로 커버되지 않는다 — 성능 이슈면 마이그레이션으로 인덱스 보강을 고려할 것(정확성엔 영향 없음)
- **JPA Auditing `DateTimeProvider` 결함은 이미 고쳐져 있다**(2026-09-07, PR #51) — `Photo extends BaseCreatedEntity`를 추가해도 별도 배선 없이 `createdAt`이 채워진다. 다만 새 엔티티를 추가하는 김에 실제 DB로 최초 INSERT를 한 번 확인할 것(CLAUDE.md 권고)
- **도메인 간 참조 금지가 diary→gallery에서 정면으로 걸린다.** `PhotoLookup` 포트를 `framework`에 두지 않고 `business/gallery`에 직접 두면 `business/diary`가 그걸 참조하는 순간 ArchUnit `DomainBoundaryTest`가 막는다(§3). 포트 시그니처에 `Photo` 엔티티를 노출해도 같은 규칙(`FRAMEWORK_MUST_NOT_KNOW_DOMAIN`류)에 걸릴 수 있어 UUID·요약 DTO만 주고받는다(`UserStatusChecker` D 패턴과 동일 이유, §3)
- `caption`·`taken_at`은 선택 필드로 보이므로 PATCH가 아닌 생성 시점 값으로만 다룬다(수정 엔드포인트 자체가 스펙에 없음 — 위 제외 항목)
- **HEIC/HEIF는 명시적으로 거부해야 한다.** iOS 기본 사진 형식이라 클라이언트 변환을 안 거치면 그대로 올라올 수 있다 — `content_type` 화이트리스트 검증(jpeg·png·webp)을 빠뜨리면 대부분 브라우저에서 못 여는 이미지가 조용히 저장된다(2026-09-08 결정)
- **`PhotoSummary`를 `data/gallery/dto`에 두면 안 된다.** 기존 `DTO_NAMING` 규칙(dto 패키지 클래스는 `Request`/`Response`로 끝나야 함)에 걸린다. `UserStatusChecker` 선례(`framework/security`, entity 대신 원시값·UUID만 반환)를 그대로 따라 `PhotoLookup`과 `PhotoSummary` 둘 다 `framework`에 두고 dto 패키지 규칙 대상에서 아예 뺀다(`/testgen` 2026-09-08 발견)
- **`framework` 아래 하위 패키지 이름에 도메인 이름을 쓰지 않는다.** 처음엔 `framework/gallery`에 뒀는데, `gallery`가 AGENTS §3의 10개 도메인 이름 중 하나라 framework 트리 안에 도메인이 새어든 것처럼 보인다는 지적을 받았다(2026-09-09). `UserStatusChecker`가 관심사 이름(`framework/security`)에 있는 것과 같은 이유로 **패턴 이름**(`framework/port`)으로 옮겼다 — 앞으로 같은 포트 패턴을 또 쓰게 되면 여기 계속 모은다

## 검증 계약

> 작성: 2026-09-08 · 대상: Phase 0 착수 직전 · 검증: `/testrun REQ-11`

**Phase 0은 신규 테스트 케이스가 0건이다.** Phase 0 완료 기준 두 가지가 이미 이 레포에 있는 범용 ArchUnit 규칙으로 자동 강제되기 때문이다 — 새 클래스가 해당 패키지에 놓이는 순간부터 별도 배선 없이 적용된다.

| 완료 기준 | 강제하는 기존 규칙 | 왜 새 케이스가 필요 없나 |
|---|---|---|
| `PhotoLookup` 시그니처에 `Photo` 엔티티가 노출되지 않음 | `ArchitectureTest.FRAMEWORK_MUST_NOT_KNOW_DOMAIN` | `framework..`가 `data..entity..`를 참조하면 이미 잡는다(`allowEmptyShould(false)`, 대상 패키지 이미 비어있지 않음) |
| `business/diary`가 `business/gallery`·`data/gallery/entity`를 우회 참조하지 않음 | `DomainBoundaryTest.NO_CROSS_DOMAIN_DEPENDENCY` | diary↔gallery 슬라이스 간 `ignoreDependency` 예외를 추가하지 않는 한 이미 막는다 |
| `PhotoSummary`가 dto 네이밍 규칙과 충돌하지 않음 | `ArchitectureTest.DTO_NAMING` | `framework/port`에 두면(위 제약 참고) 애초에 `..dto..` 패턴에 안 걸려 규칙 대상 밖이다 |

Phase 1(gallery CRUD)·Phase 2(diary 통합)에서 실제 요청/응답 로직이 생기면 그때부터 케이스가 시작된다 — `/testgen`을 그 착수 직전에 다시 돌릴 것.

> **결과 갱신: 2026-09-08.** `/testrun REQ-11`이 위 3개 규칙을 재실행해 전부 통과 확인(`ArchitectureTest` 8/8 · `DomainBoundaryTest` 1/1), 인용된 규칙 이름 3개도 소스에 그대로 존재함을 재확인. Phase 0 완료 기준 충족.

