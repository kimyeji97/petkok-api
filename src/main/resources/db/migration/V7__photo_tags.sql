-- V7__photo_tags.sql : 사진 자유 태그 + 월별 대표 사진 (REQ-22 Phase 1, PLAN-REQ-22)
-- 태그는 고정 목록이 아니라 사용자가 그때그때 입력하는 자유 텍스트라(인스타 해시태그 유비),
-- photos 컬럼 하나가 아니라 자식 테이블로 둔다 — 태그 단위 필터·중복 방지가 필요하기 때문이다.
-- photo_tags 는 photos 처럼 pet_id 를 중복시키지 않는다(펫 소유권은 photos 에서 이미 검증됨).
--
-- is_representative 는 월별 대표 사진의 "수동 지정" 여부만 저장한다. 자동 기본값(그 달 첫 업로드)은
-- 저장하지 않고 조회 시 계산한다(계획서 결정 — weight_change_rate 등과 같은 원칙).

create table photo_tags (
    id         uuid         primary key default gen_random_uuid(),
    photo_id   uuid         not null references photos (id),
    tag        varchar(50)  not null,
    created_at timestamptz  not null default now()
);
create unique index uq_photo_tags_photo_id_tag on photo_tags (photo_id, tag);
create index idx_photo_tags_tag on photo_tags (tag);

alter table photos
    add column is_representative boolean not null default false;
