-- V3__time_to_timestamptz.sql : 시각 컬럼 19개를 timestamp -> timestamptz (REQ-16)
-- 저장하는 것은 "벽시계 문자열"이 아니라 순간(instant)이다. 타임존 해석은 저장이 아니라
-- 경계에서 한다 — 노출은 KST(+09:00), 달력 판정은 Asia/Seoul. 근거는 ADR-0002.
--
-- ⚠️ USING <컬럼> AT TIME ZONE 'UTC' 를 모든 변환에 붙인다 (D6).
--    빠뜨리면 Postgres 가 세션 타임존으로 해석해 배포 환경마다 결과가 달라지는데,
--    문법 오류가 아니라 에러 없이 값만 어긋난다. 기존 행이 UTC 인 것은 실측으로 확인했다
--    (2026-08-28 Phase 0 — KST 벽시계 18:00 을 넣으면 09:00 이 저장된다).
--
-- ⚠️ date 컬럼(entry_date · measured_at · shed_date · birthday · adoption_date)은
--    건드리지 않는다. 날짜만 있는 값에는 타임존 개념이 없고, 바꾸면 커서 정렬(REQ-10 D8)과
--    체중 파생 필드 정의가 흔들린다.
--
-- default now() 는 손대지 않는다 (D8). timestamptz 컬럼에서 now() 는 세션 타임존과 무관하게
-- 올바른 순간을 반환하므로, 2026-07-30 에 목격한 "앱이 쓴 행과 now() 로 심은 행이 9시간
-- 어긋난다"는 함정이 이 전환으로 사라진다.
--
-- 스키마를 명시하지 않고 search_path 에 의존한다. V1·V2 와 같은 이유로 의도적이다.

-- 1. users (3) ----------------------------------------------------------
alter table users
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column updated_at type timestamptz using updated_at at time zone 'UTC',
    alter column deleted_at type timestamptz using deleted_at at time zone 'UTC';

-- 2. user_social_accounts (1) -------------------------------------------
alter table user_social_accounts
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 3. pets (3) -----------------------------------------------------------
alter table pets
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column updated_at type timestamptz using updated_at at time zone 'UTC',
    alter column deleted_at type timestamptz using deleted_at at time zone 'UTC';

-- 4. diary_entries (2) --------------------------------------------------
alter table diary_entries
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column updated_at type timestamptz using updated_at at time zone 'UTC';

-- 5. feeding_logs (2) ---------------------------------------------------
alter table feeding_logs
    alter column fed_at type timestamptz using fed_at at time zone 'UTC',
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 6. activity_logs (2) --------------------------------------------------
alter table activity_logs
    alter column logged_at type timestamptz using logged_at at time zone 'UTC',
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 7. weight_logs (1) ----------------------------------------------------
alter table weight_logs
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 8. shed_records (1) ---------------------------------------------------
alter table shed_records
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 9. photos (1) ---------------------------------------------------------
alter table photos
    alter column created_at type timestamptz using created_at at time zone 'UTC';

-- 10. refresh_tokens (3) ------------------------------------------------
alter table refresh_tokens
    alter column expires_at type timestamptz using expires_at at time zone 'UTC',
    alter column revoked_at type timestamptz using revoked_at at time zone 'UTC',
    alter column created_at type timestamptz using created_at at time zone 'UTC';
