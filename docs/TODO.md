# TODO — 백엔드 기능 갭

> ⚠️ **이 문서는 원본이 아니다.** 요구사항의 1차 출처는 **Notion「PetKok」→ 기획/분석 → 요구사항 탭 → `기능 요구사항` 데이터베이스**(FR-*)다.
> 이 문서는 그 25건을 실제 백엔드 코드·스키마와 대조해 **구현 안 된 것만** 추린 파생 체크리스트다. 충돌하면 Notion이 이긴다.
> 원본: <https://app.notion.com/p/yjkim97/PetKok-389b81b56e6080f6bfc2f7972108e778> → 기획/분석 → 요구사항
>
> 최종 대조: 2026-09-21 (REQ-01~19 전부 완료 후 Notion FR 25건 전수 대조 — `temperature`/`humidity`/`svl`/`representative`/`category` 등 키워드로 스키마·코드 grep, 애매한 건 직접 코드 확인)

## 백엔드에 전혀 없는 것

| ID | 우선순위 | 내용 | 근거(없음 확인) |
|---|:---:|---|---|
| FR-GAL-04 | 🟡 Should Have | 월별 대표 사진 수동 지정 또는 자동 선택 | `photos` 테이블에 대표 여부 플래그 없음 |
| FR-GAL-03 | 🟡 Should Have | 사진 태그 필터 — 탈피 / 핸들링 / 일상 / 병원 등 | `photos.caption`은 자유 텍스트뿐, `diary_entries.condition_tag` 같은 구조화 태그 컬럼 없음 |

## 부분 갭 (엔드포인트는 있으나 요구사항 일부만 커버)

| ID | 내용 | 실제 지원 범위 | 근거 |
|---|---|---|---|
| FR-GAL-01 | "사진/영상 업로드" — 일지 연동 또는 독립 업로드 | **영상 업로드 불가** | `PhotoService.ALLOWED_CONTENT_TYPES = {image/jpeg, image/png, image/webp}` |
| FR-GAL-05 | 성장 비교 뷰 — 날짜 A vs 날짜 B 사진 나란히 보기 | 날짜 지정 조회 없음, 커서 페이지네이션만 지원 | `PhotoController.list`가 `cursor`·`limit`만 받음(날짜 파라미터 없음) |

## 확인 완료 — 이미 커버됨 (참고용, 조치 불필요)

프로필 이미지(presigned URL 재사용) · 다이어리 CRUD · 급여 CRUD · 산책 기록(Activity 도메인) · 반려동물 CRUD · 갤러리 날짜순 정렬·일지 연동 · 캘린더 뷰(REQ-12 timeline) · 탈피 주기 예측(REQ-10 shed) · 거식 연속일 경고(REQ-10 anorexia-streak) · **온습도 기록 및 일간 평균 요약(FR-FEED-05, REQ-20으로 2026-09-22 완료)** · **체중 기록 및 꺾은선 그래프 시각화(FR-FEED-03, 🔴 Must Have)** — `GET /pets/{pet_id}/weight`가 이미 날짜별 체중 + `weight_change_rate`(변화율)·`is_weight_warning`(경고) 파생 필드를 반환. 2026-09-21 전수 대조가 이 항목을 갭·확인 완료 어느 목록에도 올리지 않아 누락됐던 것을 REQ-21에서 발견·보정(2026-09-22)

## 폐기됨 — 더 이상 추적하지 않음

- **FR-FEED-04**(🟡 Should Have, "게코 전용 — SVL(몸길이) 성장 그래프") — SVL 트래킹 자체를 이 프로젝트에서 하지 않기로 폐기(REQ-21, 2026-09-22). 체중 성장 그래프는 FR-FEED-03이 별개로 이미 다루고 있어 대체 불필요. Notion FR-FEED-04·연결 유저 스토리(US-FEED-05) 페이지에 폐기 사유 콜아웃 반영, [PLAN-REQ-21](plans/PLAN-REQ-21-weight-graph-fr-cleanup.md) 참고

## 별건 — Notion이 뒤처진 것 (여기서 처리 안 함)

- `CONS-TECH-01`(제약사항)이 "백엔드 1차 구현은 Python(FastAPI)"라고 남아 있으나 실제로는 Spring Boot로 확정·구현됨. ADR-001 관련 역반영 건이라 별도로 다룰 것.
