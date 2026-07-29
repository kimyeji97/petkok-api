-- V2__refresh_tokens.sql : refresh 토큰 저장소 (REQ-07)
-- 저장소 = DB 확정 (2026-07-23). Redis 는 기각 — 로그아웃·탈퇴 시 즉시 무효화가 필요하고
-- 인프라를 늘릴 이유가 없다. Notion 「소스 구조」 §7 참조.
--
-- 스키마를 명시하지 않고 search_path 에 의존한다. V1 과 같은 이유로 의도적이다 —
-- Flyway default-schema(= db.schema) 가 잡아 주므로 하드코딩하면 환경별로 못 쓴다.

-- 10. refresh_tokens ----------------------------------------------------
create table refresh_tokens (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id),
    -- 토큰 원문은 저장하지 않는다. DB 유출 시 그대로 재사용 가능해지기 때문.
    -- SHA-256 hex 는 64자 고정이라 varchar(64) 면 충분하다.
    token_hash varchar(64) not null,
    expires_at timestamp   not null,
    -- 로테이션: refresh 호출마다 새 토큰 발급 + 기존 토큰 즉시 revoke.
    -- 재사용 감지: revoked_at 이 찍힌 토큰이 재제시되면 탈취로 간주해
    -- 해당 사용자 전체 revoke 후 INVALID_TOKEN(401). 전용 ErrorCode 는 두지 않는다(탐지 여부 노출 방지).
    revoked_at timestamp,
    created_at timestamp   not null default now(),
    constraint uq_refresh_tokens_token_hash unique (token_hash)
);
-- 재사용 감지 시 "해당 사용자 전체 revoke" 와 로그아웃이 user_id 로 조회한다.
create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
