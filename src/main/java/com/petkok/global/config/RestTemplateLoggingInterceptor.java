package com.petkok.global.config;

import com.petkok.global.util.string.MaskingUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;

@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {
  private static final int RESPONSE_LOGGING_LENGTH = 500;

  /** 값을 그대로 남기면 안 되는 헤더 (소문자 비교). AGENTS.md §5 민감정보 마스킹 규칙. */
  private static final Set<String> SENSITIVE_HEADERS =
      Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key");

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    traceRequest(request, body);
    ClientHttpResponse response = execution.execute(request, body);
    response = new BufferingClientHttpResponseWrapper(response);
    traceResponse(response);

    return response;
  }

  private void traceRequest(HttpRequest request, byte[] body) {
    log.info("[===========================REQUEST=============================================]");
    log.info("[Method : {}, URI : {}]", request.getMethod(), request.getURI());
    log.info("[Request Headers]");
    for (Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
      log.info("  -> {}: {}", entry.getKey(), maskIfSensitive(entry.getKey(), entry.getValue()));
    }
    log.info("[Request Body : {}]", new String(body, StandardCharsets.UTF_8));
  }

  /** 민감 헤더면 값을 마스킹해 반환한다. 그 외에는 원본을 그대로 반환한다. */
  private static List<String> maskIfSensitive(String name, List<String> values) {
    if (name == null || !SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
      return values;
    }
    return values.stream().map(MaskingUtil::maskingCredential).toList();
  }

  /** 민감 헤더 값을 마스킹한 사본을 만든다. 원본 HttpHeaders 는 변경하지 않는다. */
  private static HttpHeaders maskSensitiveHeaders(HttpHeaders headers) {
    if (headers == null) {
      return null;
    }
    HttpHeaders masked = new HttpHeaders();
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      masked.addAll(entry.getKey(), maskIfSensitive(entry.getKey(), entry.getValue()));
    }
    return masked;
  }

  private void traceResponse(ClientHttpResponse response) throws IOException {
    String contentType = "";
    if (response.getHeaders() != null && response.getHeaders().getContentType() != null) {
      contentType = response.getHeaders().getContentType().toString();
    }

    StringBuilder inputStringBuilder = new StringBuilder();
    if (response.getBody() != null && !contentType.contains("image")) {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
      char[] buffer =
          StringUtils.contains(contentType, "text/html") ? new char[1024] : new char[1024 * 10];

      int len = bufferedReader.read(buffer, 0, buffer.length);
      if (len != -1) {
        inputStringBuilder.append(buffer, 0, len);
      }
    }

    log.info("[============================RESPONSE==========================================");
    log.info("[Status : {} {}]", response.getStatusCode(), response.getStatusText());
    log.info("[Headers : {}   ]", maskSensitiveHeaders(response.getHeaders()));
    if (inputStringBuilder.length() > RESPONSE_LOGGING_LENGTH) {
      log.info(
          "[Response Body : {} ...]",
          StringUtils.substring(inputStringBuilder.toString(), 0, RESPONSE_LOGGING_LENGTH));
    } else {
      log.info("[Response Body : {}]", inputStringBuilder);
    }
    log.info("[=========================REST TEMPLATE END====================================]");

    inputStringBuilder.setLength(0);
    inputStringBuilder.trimToSize();
  }

  /** Response wrapper 클래스. response 객체의 body 내용을 복사해두어 재사용 가능하도록 처리한다. */
  public static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
    private final ClientHttpResponse response;

    private byte[] body;

    BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
      this.response = response;
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
      // HttpStatus.valueOf(int) 는 비표준 코드(예: Cloudflare 520)에서 IllegalArgumentException 을
      // 던지므로 원본 응답의 HttpStatusCode 를 그대로 위임한다.
      return this.response.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
      return this.response.getStatusText();
    }

    @Override
    public HttpHeaders getHeaders() {
      return this.response.getHeaders();
    }

    @Override
    public InputStream getBody() throws IOException {
      if (this.body == null && this.response.getBody() != null) {
        this.body = FileCopyUtils.copyToByteArray(this.response.getBody());
      }
      return (this.body == null) ? null : new ByteArrayInputStream(this.body);
    }

    @Override
    public void close() {
      this.response.close();
    }
  }
}
