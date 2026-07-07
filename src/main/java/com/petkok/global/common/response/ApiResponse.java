package com.petkok.global.common.response;

/**
 * 표준 응답 래퍼. 성공/실패 모두 {data, error} 두 키를 유지한다. 성공: error=null / 실패: data=null
 * (GlobalExceptionHandler 가 구성).
 */
public record ApiResponse<T>(T data, ErrorResponse error) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(data, null);
  }

  public static ApiResponse<Void> error(ErrorResponse error) {
    return new ApiResponse<>(null, error);
  }
}
