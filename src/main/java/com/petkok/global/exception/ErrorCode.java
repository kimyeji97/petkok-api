package com.petkok.global.exception;

import org.springframework.http.HttpStatus;

/** 예외 코드 단일 정의. HTTP status 로 예외 "계열"을 표현한다. (별도 예외 서브클래스 트리 대신 코드+status 로 관리) */
public enum ErrorCode {

  // 400
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다."),
  INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "커서 값이 올바르지 않습니다."),
  INVALID_SPECIES_ACTIVITY(
      HttpStatus.BAD_REQUEST, "INVALID_SPECIES_ACTIVITY", "해당 종에서 지원하지 않는 활동 유형입니다."),
  SHED_NOT_SUPPORTED_SPECIES(
      HttpStatus.BAD_REQUEST, "SHED_NOT_SUPPORTED_SPECIES", "탈피 기록은 크레스티드 게코만 지원합니다."),

  // 401
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),

  // 403
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),
  PET_FORBIDDEN(HttpStatus.FORBIDDEN, "PET_FORBIDDEN", "해당 반려동물에 대한 권한이 없습니다."),

  // 404
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
  PET_NOT_FOUND(HttpStatus.NOT_FOUND, "PET_NOT_FOUND", "반려동물을 찾을 수 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),

  // 409
  SOCIAL_ALREADY_LINKED(HttpStatus.CONFLICT, "SOCIAL_ALREADY_LINKED", "이미 연결된 소셜 계정입니다."),

  // 500
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),

  // 502
  EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", "외부 API 연동 중 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
