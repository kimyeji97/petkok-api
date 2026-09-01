-- V4__feeding_food_size.sql : feeding_logs.food_size 추가 (REQ-10 Phase 3, PLAN-REQ-10 D2)
-- API I/F 급여 기록·목록이 응답 계약에 food_size 를 포함한다. 스키마가 계약보다 늦은 것이지
-- 계약이 틀린 게 아니다. 게코 곤충 사이즈는 amount(마리 수)·amount_unit 과 다른 축이라
-- 매핑으로 갚을 수 없다.
--
-- 종과 무관하게 그대로 저장한다 — "개/고양이 미사용"은 입력 UI 규약이지 서버 거부 규약이 아니다
-- (2026-08-28 확정). enum 은 varchar 로 저장하고 앱 레이어에서만 검증한다(AGENTS §5).

alter table feeding_logs
    add column food_size varchar(1) null;
