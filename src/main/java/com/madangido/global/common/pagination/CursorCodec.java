package com.madangido.global.common.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madangido.global.exception.BusinessException;
import com.madangido.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 정렬 키(예: (sortedAt, id))를 JSON→base64 opaque 문자열로 인코딩/디코딩.
 * 각 도메인 repository 의 keyset 조회에서 재사용.
 */
@Component
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public CursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("cursor encode failed", e);
        }
    }

    public <T> T decode(String cursor, Class<T> type) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}
