package com.madangido.global.common.pagination;

import java.util.List;

/**
 * 커서 기반 페이지 응답. next_cursor 는 opaque(base64) 문자열.
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new CursorPage<>(items, nextCursor, hasNext);
    }
}
