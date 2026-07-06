-- V1__init.sql : 초기 스키마 (PetKok)
-- PostgreSQL 15+ (Supabase). PK = uuid gen_random_uuid().
-- 주의: updated_at 은 앱(JPA Auditing @LastModifiedDate)이 관리한다. DB 트리거를 두지 않는다.
-- 소프트 딜리트는 users, pets 에만 적용 (deleted_at).

-- 1. users --------------------------------------------------------------
create table users (
    id                uuid         primary key default gen_random_uuid(),
    nickname          varchar(100) not null,
    email             varchar(255),
    profile_image_url varchar(500),
    created_at        timestamp    not null default now(),
    updated_at        timestamp    not null default now(),
    deleted_at        timestamp
);
create index idx_users_email       on users (email)      where deleted_at is null;
create index idx_users_deleted_at  on users (deleted_at) where deleted_at is not null;

-- 2. user_social_accounts ----------------------------------------------
create table user_social_accounts (
    id               uuid         primary key default gen_random_uuid(),
    user_id          uuid         not null references users (id),
    provider         varchar(20)  not null,   -- 앱 검증: KAKAO | GOOGLE | APPLE
    provider_user_id varchar(255) not null,
    created_at       timestamp    not null default now(),
    constraint uq_social_provider unique (provider, provider_user_id)
);
create index idx_social_accounts_user_id on user_social_accounts (user_id);

-- 3. pets ---------------------------------------------------------------
create table pets (
    id                uuid         primary key default gen_random_uuid(),
    user_id           uuid         not null references users (id),
    name              varchar(100) not null,
    species           varchar(50)  not null,   -- 앱 검증: CRESTED_GECKO | DOG | CAT
    breed             varchar(100),
    gender            varchar(20),             -- 앱 검증: MALE | FEMALE | UNKNOWN
    birthday          date,
    adoption_date     date,
    profile_image_url varchar(500),
    created_at        timestamp    not null default now(),
    updated_at        timestamp    not null default now(),
    deleted_at        timestamp
);
create index idx_pets_user_id      on pets (user_id)    where deleted_at is null;
create index idx_pets_deleted_at   on pets (deleted_at) where deleted_at is not null;

-- 4. diary_entries ------------------------------------------------------
create table diary_entries (
    id            uuid         primary key default gen_random_uuid(),
    pet_id        uuid         not null references pets (id),
    title         varchar(200),
    content       text,
    condition_tag varchar(50),               -- 앱 검증: 정상 | 활발 | 거꾸리 | 구토
    entry_date    date         not null,
    created_at    timestamp    not null default now(),
    updated_at    timestamp    not null default now()
);
create index idx_diary_pet_date      on diary_entries (pet_id, entry_date desc);
create index idx_diary_pet_condition on diary_entries (pet_id, condition_tag, entry_date desc)
    where condition_tag is not null;

-- 5. feeding_logs -------------------------------------------------------
create table feeding_logs (
    id          uuid          primary key default gen_random_uuid(),
    pet_id      uuid          not null references pets (id),
    food_type   varchar(100),
    amount      decimal(8,2),
    amount_unit varchar(20),
    is_refused  boolean       not null default false,
    fed_at      timestamp     not null,
    memo        text,
    created_at  timestamp     not null default now()
);
create index idx_feeding_pet_fed_at  on feeding_logs (pet_id, fed_at desc);
create index idx_feeding_pet_refused on feeding_logs (pet_id, fed_at desc) where is_refused = true;

-- 6. activity_logs ------------------------------------------------------
create table activity_logs (
    id               uuid         primary key default gen_random_uuid(),
    pet_id           uuid         not null references pets (id),
    activity_type    varchar(50)  not null,   -- 앱 검증: WALK | PLAY | GROOMING | TRAINING | HANDLING
    duration_minutes int,
    distance_km      decimal(6,2),            -- 실내 활동/게코는 NULL
    memo             text,
    logged_at        timestamp    not null,
    created_at       timestamp    not null default now()
);
create index idx_activity_pet_logged_at on activity_logs (pet_id, logged_at desc);

-- 7. weight_logs --------------------------------------------------------
create table weight_logs (
    id          uuid         primary key default gen_random_uuid(),
    pet_id      uuid         not null references pets (id),
    weight_g    int          not null,        -- 그램 단위 통일
    measured_at date         not null,
    memo        varchar(500),
    created_at  timestamp    not null default now()
);
create index idx_weight_pet_measured_at on weight_logs (pet_id, measured_at desc);

-- 8. shed_records (🦎 게코 전용) ----------------------------------------
create table shed_records (
    id          uuid         primary key default gen_random_uuid(),
    pet_id      uuid         not null references pets (id),
    shed_date   date         not null,
    is_complete boolean      not null default true,
    is_assisted boolean      not null default false,
    memo        varchar(500),
    created_at  timestamp    not null default now()
);
create index idx_shed_pet_date on shed_records (pet_id, shed_date desc);

-- 9. photos -------------------------------------------------------------
create table photos (
    id             uuid          primary key default gen_random_uuid(),
    pet_id         uuid          not null references pets (id),
    diary_entry_id uuid          references diary_entries (id),  -- NULL = 단독 갤러리
    image_url      varchar(1000) not null,                      -- R2 Custom Domain URL
    caption        varchar(500),
    taken_at       date,
    created_at     timestamp     not null default now()
);
create index idx_photos_pet_created_at on photos (pet_id, created_at desc);
create index idx_photos_diary_entry_id on photos (diary_entry_id) where diary_entry_id is not null;
