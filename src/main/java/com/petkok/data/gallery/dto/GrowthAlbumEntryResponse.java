package com.petkok.data.gallery.dto;

/** 성장 앨범의 월 1건 — {@code yearMonth}(예: {@code "2026-06"})의 대표 사진. REQ-22 신설. */
public record GrowthAlbumEntryResponse(String yearMonth, PhotoResponse photo) {}
