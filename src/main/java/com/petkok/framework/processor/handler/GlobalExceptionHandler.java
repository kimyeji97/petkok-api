package com.petkok.framework.processor.handler;

import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
    ErrorCode code = e.getErrorCode();
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.error(new ErrorResponse(code.getCode(), e.getMessage())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ApiResponse.error(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message)));
  }

  /**
   * 본문을 읽지 못한 경우 — 깨진 JSON, 타입 불일치, <b>정의되지 않은 enum 값</b>.
   *
   * <p>⚠️ <b>이게 없으면 500 이 나간다.</b> Jackson 의 {@code InvalidFormatException} 은 {@code
   * MethodArgumentNotValidException} 이 아니라 {@link HttpMessageNotReadableException} 으로 올라오므로 위 핸들러에
   * 걸리지 않고 {@code Exception} 핸들러까지 떨어진다. 2026-08-10 실측 — {@code POST /pets} 에 {@code
   * "species":"HAMSTER"} 를 보내자 <b>400 이 아니라 500</b> 이었다.
   *
   * <p><b>클라이언트 입력 오류를 서버 오류로 보고하면 안 된다</b>(AGENTS.md §5). 같은 계열의 함정을 REQ-08 D7 에서 한 번 밟았다 — 그때는
   * {@code @Size} 누락으로 101자 입력이 500 이 됐다.
   *
   * <p>원인 메시지를 그대로 노출하지 않는다 — Jackson 예외 문구에는 <b>클래스명·필드 경로가 들어 있어</b> 내부 구조가 샌다.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException e) {
    log.warn("Malformed request body: {}", e.getMessage());
    ErrorCode code = ErrorCode.INVALID_INPUT;
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.error(new ErrorResponse(code.getCode(), code.getMessage())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unhandled exception", e);
    ErrorCode code = ErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.error(new ErrorResponse(code.getCode(), code.getMessage())));
  }
}
