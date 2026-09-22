-- V6__environment_logs.sql : 게코 사육 환경(온습도) 기록 테이블 신설 (REQ-20 Phase 1, PLAN-REQ-20)
-- 하루 여러 건 기록 가능한 개별 로그 방식(다른 record 도메인과 동일) — measured_at 은 시각까지
-- 구분해야 하므로 timestamptz 로 둔다(REQ-17/CLAUDE.md `_at`=시각 계약). V3 이후 신설되는 테이블이라
-- timestamp 경유 없이 timestamptz 로 바로 만든다.

create table environment_logs (
    id          uuid          primary key default gen_random_uuid(),
    pet_id      uuid          not null references pets (id),
    temperature decimal(4,1)  not null,
    humidity    decimal(4,1)  not null,
    measured_at timestamptz   not null,
    memo        varchar(500),
    created_at  timestamptz   not null default now()
);
create index idx_environment_pet_measured_at on environment_logs (pet_id, measured_at desc);
