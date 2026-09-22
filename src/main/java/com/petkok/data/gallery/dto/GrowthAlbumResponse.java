package com.petkok.data.gallery.dto;

import java.util.List;

/**
 * {@code GET /pets/{pet_id}/photos/growth-album} 응답 — 월별 대표 사진 목록, 시간순(오래된 달 → 최근 달). REQ-22 신설. 월
 * 단위 집계라 건수가 작아 페이지네이션이 없다(계획서 결정).
 */
public record GrowthAlbumResponse(List<GrowthAlbumEntryResponse> items) {}
