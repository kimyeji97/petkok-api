package com.petkok.global.exception;

/**
 * 도메인/비즈니스 예외의 단일 베이스. ErrorCode 를 보유한다. 예: throw new BusinessException(ErrorCode.PET_NOT_FOUND);
 */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
