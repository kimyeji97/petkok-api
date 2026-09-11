-- V5__rename_at_to_date_columns.sql : `_at`/`_date` 컬럼 네이밍 정합화 (REQ-17)
-- 컨벤션: `_at` = 시각(timestamptz) · `_date` = 날짜(date). weight_logs.measured_at · photos.taken_at
-- 이 두 컬럼만 이 규칙을 어기고 있었다 — 타입은 REQ-16/ADR-0002 에서 이미 date 로 확정됐고(시각
-- 개념이 없다), 이번엔 이름만 규칙에 맞춘다. 타입은 손대지 않는다.

alter table weight_logs rename column measured_at to measured_date;
alter index idx_weight_pet_measured_at rename to idx_weight_pet_measured_date;

alter table photos rename column taken_at to taken_date;
