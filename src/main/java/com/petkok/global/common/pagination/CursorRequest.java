package com.petkok.global.common.pagination;

/** 커서 기반 페이지네이션 요청. limit 은 기본/최대값으로 보정된다. */
public record CursorRequest(String cursor, int limit) {

  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 100;

  public CursorRequest {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    } else if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }
  }

  public boolean hasCursor() {
    return cursor != null && !cursor.isBlank();
  }
}
