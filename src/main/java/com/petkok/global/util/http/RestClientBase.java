package com.petkok.global.util.http;

import com.petkok.global.exception.BusinessException;
import com.petkok.global.exception.ErrorCode;
import java.net.URI;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RestClientBase {

  @Autowired protected RestTemplate restTemplate;

  public static String getHttpErrorMessage(RestClientException e) {
    String message = e.getMessage();
    if (e instanceof RestClientResponseException ex) {
      if (StringUtils.isNotBlank(ex.getResponseBodyAsString())) {
        message = ex.getResponseBodyAsString();
      }
    }
    return message;
  }

  /**
   * 외부 API 호출 실패를 petkok의 BusinessException으로 변환해서 던진다. 4xx/5xx 세부 분기가 지금 당장 필요하지 않아 하나의 ErrorCode로
   * 통일한다. 원래 상태코드/메시지는 로그로 남겨 디버깅에 활용한다.
   *
   * @param httpStatus 외부 API가 응답한 상태 코드 (없을 수 있음)
   * @param ex 원본 예외
   */
  public void handleException(HttpStatus httpStatus, Exception ex) {
    String errorMessage =
        ex instanceof RestClientResponseException responseEx
            ? getHttpErrorMessage(responseEx)
            : ex.getMessage();

    log.error("External API call failed. status={}, message={}", httpStatus, errorMessage, ex);
    throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, errorMessage);
  }

  protected HttpHeaders createHeader() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.clear();
    requestHeaders.setContentType(MediaType.valueOf("application/json; charset=UTF-8"));
    requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return requestHeaders;
  }

  public <T, I> ResponseEntity<T> exchange(
      String targetUrl,
      HttpMethod method,
      HttpHeaders requestHeaders,
      I requestBody,
      Class<T> responseType) {
    log.debug("request uri: {}. method: {}, body: {}", targetUrl, method, requestBody);

    ResponseEntity<T> responseEntity;

    try {
      RequestEntity<I> requestEntity =
          new RequestEntity<>(requestBody, requestHeaders, method, URI.create(targetUrl));
      responseEntity = restTemplate.exchange(requestEntity, responseType);
    } catch (RestClientException e) {
      HttpStatus statusCode = null;
      if (e instanceof HttpStatusCodeException httpStatusCodeException) {
        statusCode = HttpStatus.valueOf(httpStatusCodeException.getStatusCode().value());
      }

      log.error("Request > RestClientException. Error Message:{}", e.getMessage(), e);
      handleException(statusCode, e);
      return null;
    } catch (Exception e) {
      log.error("Request > Exception.", e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, e.getMessage());
    }

    log.debug("responseEntity : {}", responseEntity);

    return responseEntity;
  }

  public <T, I> ResponseEntity<T> exchange(
      String targetUrl, HttpMethod method, I requestBody, Class<T> responseType) {
    return exchange(targetUrl, method, createHeader(), requestBody, responseType);
  }

  public <T> ResponseEntity<T> exchange(
      String targetUrl, HttpMethod method, Class<T> responseType) {
    return exchange(targetUrl, method, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> get(
      String targetUrl, HttpHeaders requestHeaders, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.GET, requestHeaders, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> get(String targetUrl, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.GET, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> post(
      String targetUrl, HttpHeaders requestHeaders, I requestBody, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, requestHeaders, requestBody, responseType);
  }

  public <T, I> ResponseEntity<T> post(String targetUrl, I requestBody, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, requestBody, responseType);
  }

  public <T> ResponseEntity<T> post(String targetUrl, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> put(
      String targetUrl, HttpHeaders requestHeaders, I requestBody, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, requestHeaders, requestBody, responseType);
  }

  public <T, I> ResponseEntity<T> put(String targetUrl, I requestBody, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, requestBody, responseType);
  }

  public <T> ResponseEntity<T> put(String targetUrl, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> del(
      String targetUrl, HttpHeaders requestHeaders, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.DELETE, requestHeaders, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> del(String targetUrl, Class<T> responseType) {
    return exchange(targetUrl, HttpMethod.DELETE, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> exchange(
      String targetUrl,
      HttpMethod method,
      HttpHeaders requestHeaders,
      I requestBody,
      ParameterizedTypeReference<T> responseType) {
    log.debug("request uri: {}. method: {}, body: {}", targetUrl, method, requestBody);

    ResponseEntity<T> responseEntity;

    try {
      RequestEntity<I> requestEntity =
          new RequestEntity<>(requestBody, requestHeaders, method, URI.create(targetUrl));
      responseEntity = restTemplate.exchange(requestEntity, responseType);
    } catch (RestClientException e) {
      HttpStatus statusCode = null;
      if (e instanceof HttpStatusCodeException httpStatusCodeException) {
        statusCode = HttpStatus.valueOf(httpStatusCodeException.getStatusCode().value());
      }

      log.error("Request > exception. Error Message:{}", e.getMessage(), e);
      handleException(statusCode, e);
      return null;
    } catch (Exception e) {
      log.error("Request > exception.", e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, e.getMessage());
    }

    log.debug("responseEntity : {}", responseEntity);

    return responseEntity;
  }

  public <T> ResponseEntity<T> exchange(
      String targetUrl, HttpMethod method, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, method, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> exchange(
      String targetUrl,
      HttpMethod method,
      I requestBody,
      ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, method, createHeader(), requestBody, responseType);
  }

  public <T> ResponseEntity<T> get(
      String targetUrl, HttpHeaders requestHeaders, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.GET, requestHeaders, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> get(String targetUrl, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.GET, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> post(
      String targetUrl,
      HttpHeaders requestHeaders,
      I requestBody,
      ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, requestHeaders, requestBody, responseType);
  }

  public <T, I> ResponseEntity<T> post(
      String targetUrl, I requestBody, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, requestBody, responseType);
  }

  public <T> ResponseEntity<T> post(String targetUrl, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.POST, (Void) null, responseType);
  }

  public <T, I> ResponseEntity<T> put(
      String targetUrl,
      HttpHeaders requestHeaders,
      I requestBody,
      ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, requestHeaders, requestBody, responseType);
  }

  public <T, I> ResponseEntity<T> put(
      String targetUrl, I requestBody, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, requestBody, responseType);
  }

  public <T> ResponseEntity<T> put(String targetUrl, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.PUT, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> del(
      String targetUrl, HttpHeaders requestHeaders, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.DELETE, requestHeaders, (Void) null, responseType);
  }

  public <T> ResponseEntity<T> del(String targetUrl, ParameterizedTypeReference<T> responseType) {
    return exchange(targetUrl, HttpMethod.DELETE, (Void) null, responseType);
  }
}
