# PLAN-REQ-09 · pet 도메인 + `PetAccessGuard` (소유권 앵커)

> 출처: 2026-08-10 세션 · 작성: 2026-08-10 · 상태: 🟡 진행 (Phase 1·2 완료 · 결정 D1~D6 확정 · Phase 3 남음)

## 배경

`pets` 테이블은 `V1__init.sql` 에 있지만 **엔트리포인트가 하나도 없다.** 그리고 이건 pet 도메인만의 문제가 아니다 —

**`/pets/{pet_id}` 하위에 REQ-10~12 의 도메인 다섯이 통째로 매달려 있다**(diary · feeding · activity · weight · shed · gallery). 그 전부가 "이 펫이 내 것인가"를 물어야 하고, 그 질문에 답하는 자리가 `PetAccessGuard` 다. **여기서 형태를 잘못 잡으면 다섯 도메인이 그걸 그대로 복제한다.**

> Notion 「소스 구조」 §3 — "`/pets/{petId}/...` 하위 도메인은 진입 시 `PetAccessGuard.getOwnedPet(petId, userId)` 로 소유권·종 검증 후 Pet 을 받아 처리한다. `PetAccessGuard` 는 `business/pet/service` 에 둔다."

REQ-15 로 컨트롤러 테스트 관례가 확정됐으므로 **pet 컨트롤러부터 소급 없이 `@WebMvcTest` 를 적용**할 수 있다.

## 범위

**포함**

- `business/pet/{controller,service}` + `data/pet/{entity,repository,dto,enums}` 신설
- **엔드포인트 5종** (Notion `API I/F` → Pets 도메인 5행 = pet 도메인 전부)
- **`PetAccessGuard`** — `business/pet/service` 에 둔다(원본 명시)
- 컨트롤러 테스트 — AGENTS §6 관례 적용
- **원본 Validation** (Notion `API I/F` → 반려동물 등록에서 옮겨 적음, 2026-08-10) — `name` 필수 / `species`: `CRESTED_GECKO | DOG | CAT` / `gender`: `MALE | FEMALE | UNKNOWN`
	- ⚠️ **여기 옮겨 적은 이유가 있다.** `/testrun` 의 근거 인용 검사는 **파일에서 `grep`** 한다 — Notion 은 파일이 아니라 검사할 수 없다. 원본에만 두면 케이스가 근거를 못 갖거나 "근거 소실" 오탐이 난다. **파생 요약 `api-list §3` 에 이 Validation 이 아예 없다는 것도 같은 문제의 다른 얼굴이다**

**제외**

- **하위 도메인 구현** (diary·feeding·activity·weight·shed·gallery) — REQ-10~12. 이번엔 **가드가 그것들에 쓰일 형태인지**까지만 본다
- **`species` enum 의 종별 분기 로직** — `shed` 는 게코만, `activity` 는 종별 허용값이 다르다(Notion §8). **그 규칙이 실행되는 곳은 각 하위 도메인**이고 이번 범위가 아니다. 다만 **누가 검증하는가**는 미결로 올렸다(아래)
- **커서 페이지네이션** — 원본 `GET /pets` 응답에 `next_cursor` 가 없다(아래 미결). 근거 없이 만들지 않는다
- **`updated_at` 응답 노출** — 원본 `GET /pets` 응답 필드가 `id`·`name`·`species`·`breed`·`gender`·`birthday`·`adoption_date`·`profile_image_url`·`created_at` **9개다.** 엔티티는 갖고 있지만 넣지 않는다(REQ-08 `UserResponse` 와 같은 이유)
- **탈퇴 시 pets 처리** — REQ-08 이 "pet 도메인이 REQ-09라 대상 테이블 자체가 아직 없다"로 미뤘다. **이번에 pets 가 생기므로 다시 열리지만**, REQ-08 의 결정을 이 REQ 에서 뒤집지 않는다 → 미결로 이관

## 결정

> **D1·D3·D4 는 2026-08-10 실측(「프로브 결과」)을 근거로 확정했다.** 나머지는 여전히 미결이며 표를 억지로 채우지 않는다.

| 항목 | 결정 | 근거 | 기각한 안 |
|---|---|---|---|
| **D1** `PetAccessGuard` 위치 | **`business/pet/service`** (확정 2026-08-10) | Notion 「소스 구조」 §3 이 명시한다. ⚠️ **초안에 "Service→Service 라 `LAYER_DIRECTION` 에 걸린다"고 적었으나 프로브로 반증됐다** — 같은 레이어 내 참조는 위반이 아니다. 실제로 걸리는 것은 **도메인 간 참조 금지 하나뿐**이다(아래 「프로브 결과」) | **`framework` 포트 + pet 구현**(REQ-08 D2 방식) — **기각이 아니라 보류**다. 슬라이스 위반을 없애는 유일한 형태지만 `Pet` 을 못 돌려줘 종 검증이 막힌다(미결 ①) |
| **D2** 엔드포인트 계약 | **Notion `API I/F` 5행을 그대로 따른다** — `POST` 201 · `GET`(목록/상세) 200 · `PATCH` 200 · `DELETE` 204 | 2026-08-10 원본 5행을 직접 읽었다. **파생 요약 `api-list §3` 에는 상태코드도, 아래 제약들도 없다** | — |
| **D3** 가드의 반환 타입 | **`data/pet/dto` 의 읽기 전용 DTO**(`OwnedPetResponse(id, species)`). `Pet` 엔티티를 돌려주지 않는다 | **예외의 사정거리를 좁히기 위해서다.** `Pet` 을 돌려주면 `data.pet` 을 통째로 열어야 하고, 그러면 **하위 도메인이 `PetRepository` 를 직접 주입해도 ArchUnit 이 통과시킨다** — 소유권 앵커를 두는 목적 자체를 규칙이 못 지킨다. DTO 반환 + 좁은 예외 3건(`business.pet.service`·`data.pet.dto`·`data.pet.enums`)이면 **우회와 엔티티 누출이 둘 다 잡힌다**(2026-08-10 실측). AGENTS §5 "Entity 는 Service 밖으로 나가지 않는다"와도 결이 같다 — 하위 도메인에게 pet 은 남의 도메인이다 | **A: `Pet` 엔티티 반환**(Notion §3 문구 그대로) — 예외 6건이면 되지만 **우회 차단을 잃는다.** REQ-10~12 여섯 도메인 내내 대가를 치른다. **B2′: `framework` 포트 + `boolean` 반환** — 예외 0건으로 가장 깨끗하지만 **`species` 를 실어 나를 수 없어 종 검증 자리가 사라진다**(기능 손실이라 다르게 갚을 수 없다). **C: 하위가 `PetRepository` 직접** — 가드가 존재할 이유가 없어진다 |
| **D4** 종(species) 검증 주체 | **각 하위 도메인 Service.** 가드는 소유권만 판정하고 `species` 를 DTO 에 실어 넘긴다 | Notion 「소스 구조」 §8 이 이미 "**각 Service 가 진입 시 검증**"이라고 적고 있다(shed=게코만, activity=종별 허용값). §3·§11 의 "가드가 소유권·종 검증"은 **시그니처상 성립하지 않는다** — `getOwnedPet(petId, userId)` 에는 "이 엔드포인트가 기대하는 종"이 들어오지 않는다. D3 으로 DTO 가 `species` 를 실어 나르므로 **검증에 필요한 정보는 하위 도메인에 도달한다** | **가드가 기대 종을 인자로 받아 검증**(`getOwnedPet(petId, userId, Species...)`) — 가드가 도메인별 규칙을 알게 되어 **shed·activity 규칙이 pet 도메인으로 새어 든다.** 규칙이 바뀔 때마다 가드를 고쳐야 한다 |
| **D5** `PATCH` 로 `species` 가 오면 | **무시한다.** `PetUpdateRequest` 에 `species` 필드를 두지 않는다 | 원본은 "**species 변경 불가**"만 말하고 거부 방식을 정하지 않았다 — REQ-08 D3("원본에 없는 규약을 만들지 않는다")을 그대로 적용한다. **구현 비용이 0 이고 앱 전체 동작과 일관된다** — 2026-08-10 실측: 기존 `PATCH /users/me` 에 알 수 없는 필드를 보내면 **조용히 무시되고 200** 이다(Spring Boot 기본값 `FAIL_ON_UNKNOWN_PROPERTIES=false`). 착각 우려는 원본이 `PATCH` 응답을 "**수정된 펫 객체**"로 규정해 완화된다 — 클라이언트가 응답의 `species` 로 확인할 수 있다 | **ⓐ `fail-on-unknown-properties=true`** — 설정 한 줄이지만 **전역이다.** 모든 엔드포인트가 여분 필드에 400 을 내게 되어 클라이언트가 필드 하나만 더 보내도 깨진다. **ⓑ DTO 에 `species` 를 두고 "오면 400"** — 국소적이지만 **받지 않을 필드를 DTO 에 두는 모양**이 되고, REQ-08 D3(`null` = 변경 없음)과 의미가 섞인다 |
| **D6** 소프트 딜리트 구현 | **수동 — `users`(REQ-08)와 같게.** `findByIdAndDeletedAtIsNull` 계열 파생 쿼리 + `pet.softDelete()`. `@SQLDelete`·`@SQLRestriction` 을 쓰지 않는다 | **한 프로젝트에 두 방식이 생기는 것을 피한다.** 그리고 pets 는 빠뜨림 위험이 구조적으로 작다 — D3·D4 로 **하위 도메인이 전부 `PetAccessGuard` 를 통과**하고 우회는 ArchUnit 이 막는 것을 실측했으므로(「프로브 결과」), 걸러야 할 관문이 사실상 가드 하나다. 필터가 **코드에 보이는** 것도 이점이다 | **`@SQLRestriction`**(Notion §6 명시) — 빠뜨릴 수 없지만 **조용하고**, §6 자신이 경고하듯 **네이티브 쿼리·조인에서 안 걸려 삭제된 행이 에러 없이 샌다.** 무엇보다 `users` 와 방식이 갈린다. **`users` 까지 함께 전환** — 일관성은 지키지만 **REQ-08 을 건드리는 일이라 REQ-09 범위를 넘고**, 이미 머지된 검증 계약(`REQ-08-02` 등)을 다시 돌려야 한다 |

## 미결 질문

> ⚠️ **아래는 원본에 답이 없거나 원본끼리 어긋나는 것들이다.** 추측으로 채우지 않았다 — 이걸 정하는 것이 이 REQ 의 첫 작업이다.

- [x] ⭐ **종(species) 검증을 누가 하는가** — **각 하위 도메인 Service (D4 확정, 2026-08-10).** Notion §3·§11 과 §8 이 갈렸는데, §3 의 서술은 시그니처상 성립하지 않아 §8 을 따랐다
- [x] ⭐ **가드 참조를 어떻게 허용할 것인가** — **DTO 반환 + 좁은 예외 3건 (D3 확정, 2026-08-10).** 실측으로 우회·엔티티 누출이 둘 다 차단되는 것을 확인했다
- [x] ⭐ **`PetAccessGuard` 가 어떤 ArchUnit 규칙에 걸리는가** — **2026-08-10 프로브로 확정. 아래 「프로브 결과」 참고.** 남은 판단(예외를 늘릴지 vs 다른 형태)은 미결 ①' 로 옮겼다
- [ ] **Notion 역반영 2건 (D3·D4·D6 확정에 따른 것).** ⓐ 「소스 구조」 §6 의 소프트 딜리트 — `@SQLDelete`·`@SQLRestriction` 을 쓰라고 돼 있으나 실제는 **수동 파생 쿼리**다(`users`·`pets` 둘 다). ⓑ 「소스 구조」 §3 — 원본은 "`PetAccessGuard.getOwnedPet(petId, userId)` 로 소유권·**종 검증** 후 **Pet** 을 받아 처리한다"고 적었는데, 확정안은 **읽기 전용 DTO 를 받고 종 검증은 각 하위 Service** 가 한다. 의도는 같지만 문구가 다르다 — **사람이 Notion 에서 수정해야 한다**
- [ ] **`GET /pets` 에 커서 페이지네이션을 넣을 것인가.** 원본 응답은 `{"data":{"items":[...]}}` 뿐이고 `next_cursor` 가 없다. 그런데 AGENTS §5 는 "페이지네이션: 커서 기반"이고 `framework/pagination` 에 `CursorRequest`·`CursorPage`·`CursorCodec` 가 이미 있다. **한 사람이 가진 펫 수는 작다**는 것이 원본의 전제로 보이지만 명시돼 있지 않다 — 근거 없이 넣지도, 빼지도 않는다
- [x] **소프트 딜리트를 어떻게 구현할 것인가** — **수동 유지 (D6 확정, 2026-08-10).** `users` 와 일치시켰고, Notion §6 과 어긋나므로 역반영 대상에 포함된다
- [x] **`PATCH` 로 `species` 를 보내면 어떻게 되는가** — **무시 (D5 확정, 2026-08-10).** 실측으로 이것이 앱의 기본 동작임을 확인했고, 400 안 둘 다 대가가 더 컸다
- [ ] **회원 탈퇴 시 pets 를 어떻게 할 것인가.** REQ-08 이 "대상 테이블이 아직 없다"로 미룬 항목인데 **이번에 생긴다.** `users.deleted_at` 만 찍고 pets 를 두면 탈퇴 계정의 펫이 살아 있고, 재가입은 새 `users.id` 라(REQ-08 D1) **주인 없는 행이 된다.** REQ-08 의 결정을 이 REQ 에서 뒤집지 않되, 어디서 다룰지는 정해야 한다
- [ ] **`DELETE /pets/{id}` 의 "연관 기록 보존"이 무슨 뜻인가.** 원본은 "소프트 딜리트. **연관 기록(일지/식사/갤러리) 보존**"이라 적었다. 하위 테이블에는 `deleted_at` 이 없으므로 행은 자연히 남는데, **삭제된 펫의 하위 기록을 조회할 수 있어야 하는지**는 말하지 않는다. REQ-10 착수 전에 답이 필요하다

## 프로브 결과 (2026-08-10 실측)

미결 ②를 문서가 아니라 **실제로 심어서** 닫았다. 가짜 `PetAccessGuard` 와 `business/diary/service/DiaryService` 를 만들어 ArchUnit 이 무엇을 잡는지 셌다.

| 배치 | 발화한 규칙 |
|---|---|
| **A** `business/pet/service` 에 가드 · `Pet` 반환 (원본 명시안) | `도메인 간 참조 금지` **만** |
| **B** `framework` 포트가 `Pet` 반환 | `framework → business·data 참조 금지` **+** `도메인 간 참조 금지` |
| **B2** `framework` 포트가 `boolean` 반환 · 하위가 **구체 클래스**를 주입 | `도메인 간 참조 금지` — 포트 탓이 아니라 구체 클래스 주입 탓이다 |
| **B2′** 포트가 `boolean` 반환 · 하위가 **포트만** 주입 | ✅ **위반 0건** — ArchUnit 을 전혀 건드리지 않는다 |
| **B4** 포트가 `data/pet/enums/Species` 노출 | `framework → business·data 참조 금지` — **포트에 도메인 타입을 실을 수 없다** |
| **A + 예외** `business/pet/service` 한 줄만 허용 | ❌ **여전히 위반** — 하위 도메인이 `Pet`·`Species`(= `data.pet` 슬라이스)도 만진다 |
| **A + 예외 2개** (`business.pet.service` + `data.pet`) | ✅ 통과 — 예외 4 → 6건. **다만 `data.pet` 을 통째로 열어 우회가 뚫린다**(아래) |
| **D** 가드가 `data/pet/dto` 의 읽기 전용 DTO 반환 · 예외 3개(`business.pet.service` + `data.pet.dto` + `data.pet.enums`) | ✅ 통과. **그리고 우회는 여전히 잡힌다** |

### 세 안의 실측 비용 (2026-08-10)

| | ArchUnit 예외 | 종 검증 | 우회 차단 | 원본(§3) 부합 |
|---|:--:|:--:|:--:|:--:|
| **A** 가드가 `Pet` 엔티티 반환 | 4 → **6건** | ✅ | ❌ **뚫린다** | ✅ |
| **B2′** 포트가 `boolean` 반환 | **0건** | ❌ | ✅ | ❌ Pet 을 못 받는다 |
| **C** 하위가 `PetRepository` 직접 | (측정 안 함) | ✅ | ❌ | ❌ 가드가 존재할 이유가 사라진다 |
| ⭐ **D** 가드가 **읽기 전용 DTO** 반환 | 4 → **7건** | ✅ | ✅ **차단됨** | ◐ Pet 자체는 아니지만 "받아 처리"는 성립 |

**A안의 진짜 비용은 예외 개수가 아니라 그 예외가 여는 문이다.** 프로브에서 확인했다 —
`data.pet` 을 통째로 허용하면 **하위 도메인이 `PetRepository` 를 직접 주입해도 ArchUnit 이 통과시킨다.**
즉 **가드를 우회하는 코드가 규칙에 걸리지 않게 된다.** 소유권 앵커를 두는 목적 자체를 규칙이 더 이상 지켜 주지 못한다.

> **이것이 A와 B2′ 의 진짜 대립축이다.** 예외 2건이 늘어나는 것보다, **"가드를 통해야 한다"를 강제하던 장치가 풀린다**는 쪽이 무겁다. 반대로 B2′ 는 규칙을 완벽히 지키지만 **원본이 명시한 "Pet 을 받아 처리한다"를 포기**해야 하고 종 검증 자리가 사라진다.

**측정으로 좁혀지지 않는 지점** — 종 검증을 포트 시그니처에 실어 보내는 변형(예: 기대 종을 인자로 넘기고 `boolean` 만 받기)은 **framework 가 종 개념을 알아야** 하므로 `Species` 를 어디에 둘지의 문제로 되돌아온다. `data/{도메인}/enums` 규약(AGENTS §3)과 충돌하므로 **이건 측정이 아니라 판단이 필요하다.**

### ⭐ D안 — 예외의 사정거리를 좁히면 우회 차단이 살아남는다 (2026-08-10 추가 실측)

A안이 `data.pet` 을 통째로 여는 것이 문제였다면, **여는 범위를 `dto`·`enums` 로 한정**하면 어떻게 되는가를 쟀다.

가드가 `Pet` 엔티티 대신 `data/pet/dto/OwnedPetResponse`(읽기 전용 record, `id`·`species`)를 돌려주고, 예외를 셋으로 좁혔다 — `business.pet.service` · `data.pet.dto` · `data.pet.enums`. **`entity` 와 `repository` 는 닫아 둔다.**

| 확인 | 결과 |
|---|---|
| 하위 도메인이 가드를 통해 `species` 로 분기 | ✅ 통과 |
| 하위 도메인이 `PetRepository` 직접 주입 (**우회**) | ✅ **잡힌다** |
| 하위 도메인이 `Pet` 엔티티 직접 참조 (**엔티티 누출**) | ✅ **잡힌다** |

> **A안이 잃었던 것을 D안은 지킨다.** A는 `data.pet` 을 통째로 열어 "가드를 통해야 한다"는 강제력이 사라졌는데, D는 **가드가 돌려주는 것(DTO)과 그 안의 값(enum)만** 열고 **가드를 우회할 수단(entity·repository)은 닫아 둔다.** 예외는 6건 대신 7건으로 하나 더 늘지만, **늘어난 예외가 오히려 경계를 더 정확히 그린다.**

D안은 AGENTS §5 의 "**Entity 는 Service 밖으로 나가지 않는다**"와도 결이 같다 — 하위 도메인 입장에서 pet 은 남의 도메인이므로, 남의 엔티티가 아니라 **읽기 전용 표현**을 받는 것이 오히려 규약에 부합한다.

**남는 판단** — Notion §3 은 "`Pet` 을 받아 처리한다"고 적었는데 D는 DTO 를 받는다. **원본 문구와 정확히 같지는 않다.** 다만 원본의 의도(소유권 확인 + 종을 알고 분기)는 그대로 성립하므로, 채택한다면 **Notion 역반영 대상**이다.

### ⚠️ 계획서에 적었던 예상이 틀렸다

착수 전 미결 ②에 **"Service → Service 라 `LAYER_DIRECTION` 에 걸린다"** 고 적었는데 **걸리지 않았다.** `mayOnlyBeAccessedByLayers("Controller")` 는 **같은 레이어 안의 참조를 막지 않는다** — `business/diary/service` 도 `Service` 레이어이므로 통과한다.

> **08-03 프로브에서 `LAYER_DIRECTION` 이 필터의 Repository 직참조를 잡았던 것과 상황이 다르다.** 그때 걸린 이유는 필터가 **어느 레이어에도 속하지 않아서**였다. 같은 규칙이라도 "누가 부르느냐"에 따라 결과가 갈린다 — **규칙 이름만 보고 추론하면 틀린다.**

### 실제로 남는 문제는 하나다

**어떤 배치를 골라도 `Slice diary depends on Slice pet` 이 남는다.** 걸린 의존은 3종 — 생성자 파라미터 · 필드 타입 · 메서드 호출. 즉 **가드를 주입받는 것 자체**가 슬라이스 위반이고, 반환 타입을 바꿔도 사라지지 않는다.

유일한 예외는 **REQ-08 D2 와 똑같은 구조** — 인터페이스를 `framework` 에 두고 하위 도메인은 그 인터페이스만 참조하며, 구현체(`business/pet`)는 Spring 이 주입한다. 이때 하위 도메인의 바이트코드에는 `business.pet` 이 나타나지 않는다. **다만 그 경우 포트가 도메인 타입을 노출할 수 없어 `Pet` 을 돌려줄 수 없고, 그래서 미결 ①(종 검증)이 미해결로 남는다.**

> **①과 ①' 는 한 문제의 두 얼굴이다** — `Pet` 을 넘기면 슬라이스가 깨지고, 안 넘기면 종 검증을 못 한다. 이 긴장을 어떻게 풀지가 REQ-09 의 핵심 설계 판단이다.

## 작업 단계

> **미결 4건이 남아 있다** (2026-08-10 기준). ①·①'·②는 Phase 1 프로브와 D3·D4 로 닫혔고, `PATCH`/`species`(D5)·소프트 딜리트(D6)도 Phase 2 착수 전에 닫았다. 남은 넷은 **전부 pet 도메인 밖으로 번지는 것들**이다 — Notion 역반영 2건 · 커서 페이지네이션 · 탈퇴 시 pets · `DELETE` 후 하위 기록 조회.

- [x] **Phase 1 — ArchUnit 실측 (탐색) — 2026-08-10 완료**
      가짜 가드와 `business/diary/service` 를 심어 세 배치를 비교했다. 결과는 「프로브 결과」에 있다. **프로브는 전부 삭제했다.**
      → **미결 ②는 닫혔고, ①·①' 가 남았다.** 둘은 한 문제의 두 얼굴이라 함께 정해야 한다

- [x] **Phase 2 — pets CRUD 5종 — 2026-08-10 완료**
      완료 기준: 응답 필드가 원본 9개와 정확히 일치(`updated_at` 없음) · `POST` 201 · `DELETE` 204 · 남의 펫 접근 시 `PET_FORBIDDEN`(403) · 없는 펫은 `PET_NOT_FOUND`(404) · ArchUnit 8건 통과
      → 완료 기준 6항목 전부 충족. `/testrun REQ-09` 에서 **케이스 14개 ID · 테스트 23건 + ArchUnit 8건 전부 통과**(검증 계약 `결과` 열 참조).
      **계획에 없던 변경 1건** — `framework` 의 `GlobalExceptionHandler` 에 `HttpMessageNotReadableException` 핸들러를 추가했다. Phase 경계 밖이지만 `REQ-09-15·16` 이 이것 없이는 성립하지 않는다(사유는 `PROGRESS.md` 2026-08-10).

- [ ] **Phase 3 — `PetAccessGuard` 확정**
      Phase 1 결정대로 구현하고, **하위 도메인이 실제로 쓸 수 있는지**를 확인한다.
      완료 기준: 소유권 위반 403 · 미존재 404 가 **HTTP 왕복으로** 검증됨(AGENTS §6 관례) · 프로브로 ArchUnit 이 여전히 살아 있는지 확인

## 검증 계약

> 작성: 2026-08-10 · 근거: 이 계획서(원본은 Notion `API I/F` · 「소스 구조」) · 검증: `/testrun REQ-09`
> `결과` 열은 `/checkpoint`가 채운다. 케이스 ID는 테스트명에 `[REQ-09-01]` 형태로 박는다.
> **`message` 를 단언하지 않는다** — 로케일을 탄다(REQ-15 실측). `status` 와 `error.code` 만 본다.

| ID | 대상 | 케이스 | 유형 | 근거 | Phase | 결과 |
|----|------|--------|:--:|------|:--:|:--:|
| REQ-09-01 | 응답 DTO | 필드가 정확히 9개 | 불변식 | 범위—제외 — "원본 `GET /pets` 응답 필드가 `id`·`name`·`species`·`breed`·`gender`·`birthday`·`adoption_date`·`profile_image_url`·`created_at` **9개다.**" | 2 | ✅ |
| REQ-09-02 | 〃 | `updated_at` 이 없다 | 불변식 | 〃 | 2 | ✅ |
| REQ-09-03 | `POST /pets` | 201 을 반환한다 | 정상 | D2 — "`POST` 201" | 2 | ✅ |
| REQ-09-04 | `DELETE /pets/{id}` | 204 · 본문 없음 | 정상 | D2 — "`DELETE` 204" | 2 | ✅ |
| REQ-09-05 | `PATCH /pets/{id}` | 이름만 보내면 나머지 필드가 유지된다 | 회귀 | 제약·함정 — "**부분 반영 병합은 서비스에서**" | 2 | ✅ |
| REQ-09-06 | 수정 요청 DTO | `@NotBlank`·`@NotNull` 이 없다 | 회귀 | 제약·함정 — "**PATCH 요청 DTO 에 `@NotBlank`·`@NotNull` 금지**" | 2 | ✅ |
| REQ-09-07 | 남의 펫 조회 | `PET_FORBIDDEN` | 예외 | Phase 2 완료 기준 — "남의 펫 접근 시 `PET_FORBIDDEN`(403)" | 2 | ✅ |
| REQ-09-08 | 없는 펫 조회 | `PET_NOT_FOUND` | 예외 | Phase 2 완료 기준 — "없는 펫은 `PET_NOT_FOUND`(404)" | 2 | ✅ |
| REQ-09-14 | `POST /pets` | `name` 누락 → 400 | 경계 | 범위—포함 — "원본 Validation … `name` 필수" | 2 | ✅ |
| REQ-09-15 | 〃 | 정의되지 않은 `species` 값 거부 | 예외 | 범위—포함 — "`species`: `CRESTED_GECKO | DOG | CAT`" | 2 | ✅ |
| REQ-09-16 | 〃 | 정의되지 않은 `gender` 값 거부 | 예외 | 범위—포함 — "`gender`: `MALE | FEMALE | UNKNOWN`" | 2 | ✅ |
| REQ-09-17 | `PATCH /pets/{id}` | `species` 를 보내도 종이 바뀌지 않는다 | 불변식 | D5 — "**무시한다.** `PetUpdateRequest` 에 `species` 필드를 두지 않는다" | 2 | ✅ |
| REQ-09-18 | `Pet` 엔티티 | `@SQLDelete`·`@SQLRestriction` 이 붙어 있지 않다 | 회귀 | D6 — "`@SQLDelete`·`@SQLRestriction` 을 쓰지 않는다" | 2 | ✅ |
| REQ-09-19 | 삭제된 펫 조회 | `PET_NOT_FOUND` (수동 필터가 실제로 건다) | 예외 | D6 — "`findByIdAndDeletedAtIsNull` 계열 파생 쿼리" | 2 | ✅ |
| REQ-09-09 | `PetAccessGuard` | 반환 타입이 `Pet` 엔티티가 아니다 | 불변식 | D3 — "`Pet` 엔티티를 돌려주지 않는다" | 3 | — |
| REQ-09-10 | 가드 반환 DTO | `species` 를 싣는다 | 불변식 | D4 — "`species` 를 DTO 에 실어 넘긴다" | 3 | — |
| REQ-09-11 | 가드 반환 DTO | `data/pet/dto` 에 있다 | 불변식 | D3 — "**`data/pet/dto` 의 읽기 전용 DTO**" | 3 | — |
| REQ-09-12 | 소유권 위반 | **HTTP 왕복** 403 `PET_FORBIDDEN` | 예외 | Phase 3 완료 기준 — "소유권 위반 403 · 미존재 404 가 **HTTP 왕복으로** 검증됨" | 3 | — |
| REQ-09-13 | 미존재 펫 | **HTTP 왕복** 404 `PET_NOT_FOUND` | 예외 | 〃 | 3 | — |

**`ArchUnit 8건 통과`(Phase 2·3 완료 기준)에는 케이스를 새로 만들지 않았다** — 기존 `ArchitectureTest`·`DomainBoundaryTest` 가 이미 덮는다. 다만 D3 의 좁은 예외가 실제로 우회를 막는지는 **`/implement` 의 프로브**로 확인한다(케이스로는 표현되지 않는다).

**아래 1건은 계획서 미결이라 케이스를 쓰지 않았다** — `DELETE` 후 하위 기록 조회 가능 여부. 미결이 닫히면 케이스를 추가한다.
(`PATCH`/`species` 는 D5 로, 소프트 딜리트는 D6 으로 2026-08-10 에 닫혀 `REQ-09-17~19` 가 추가됐다.)

## 제약·함정

- ⭐ **파생 요약 `api-list §3` 은 원본보다 얇다.** 2026-08-10 대조에서 **원본에만 있는 것 4건**을 확인했다 — ① `species` 등록 후 변경 불가 ② `DELETE` 시 연관 기록 보존 ③ `POST` 가 201 ④ `GET /pets` 응답이 `items` 배열. **`api-list` 만 보고 구현하면 넷 다 빠진다.** REQ-08 에서 파생 문서가 원본에 없는 내용을 만들어 낸 사례가 두 번 있었는데(`/users/me/social-accounts`, 탈퇴 시 revoke), **이번엔 반대로 빠뜨린 사례**다 — 파생 요약은 양방향으로 어긋난다
- **Entity 는 Service 밖으로 나가지 않는다** (AGENTS §5) — `PetAccessGuard.getOwnedPet` 이 `Pet` 엔티티를 돌려주는데, 이것이 컨트롤러까지 흘러가면 ArchUnit `NO_ENTITY_IN_CONTROLLER` 가 잡는다. **가드의 반환 타입이 곧 경계**다
- **PATCH 요청 DTO 에 `@NotBlank`·`@NotNull` 금지** (AGENTS §5, REQ-08 D7 에서 밟았다) — `null` 은 "변경 없음"이다. 길이 제약은 `@Size` 로만
- **부분 반영 병합은 서비스에서** (AGENTS §5, REQ-08 D6) — 엔티티의 `updateXxx` 는 받은 값을 그대로 쓴다. 빠뜨리면 **에러 없이 다른 필드가 지워지고 응답은 200** 이다
- **컨트롤러 테스트는 `@Import({SecurityConfig, JacksonConfig})`** (AGENTS §6) — 빠뜨리면 **테스트가 통과하면서 틀린 계약을 고정한다**
- **`@Entity` 가 늘면 `ddl-auto: validate` 가 처음으로 일한다** — Notion 구현 노트가 "첫 엔티티가 들어오는 시점에 처음 확인된다"고 적어 둔 항목이다. `pets` 엔티티와 `V1__init.sql` 이 어긋나면 **기동 시점에 터진다**(REQ-07 에서 `users` 로 이미 한 번 통과했으므로 이제는 실증된 경로지만, 컬럼 추가 시 여전히 유효)
