package com.petkok.data.gallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * {@code POST /photos/presigned-url} 요청. 계획서 범위 — "요청에 `content_type`·`content_length`를 받아 검증". 허용
 * 타입·최대 크기는 {@link com.petkok.business.gallery.service.PhotoService} 가 판정한다(계획서 결정 —
 * "`image/jpeg`·`image/png`·`image/webp`만 허용, 최대 10MB").
 */
public record PhotoPresignedUrlRequest(
    @NotBlank String contentType, @NotNull @Positive Long contentLength) {}
