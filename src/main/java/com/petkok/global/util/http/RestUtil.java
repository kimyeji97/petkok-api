package com.petkok.global.util.http;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

/**
 * Rest req, res 유틸리티
 *
 * @author yjkim
 */
@Slf4j
public class RestUtil {

  private RestUtil() {}

  private static final String ERROR_FILE_NM = "error.txt";
  private static final String ERROR_FILE_MSG = "file download error.";

  /**
   * 파일명 헤더 HttpHeaders.CONTENT_DISPOSITION HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
   *
   * @param response 응답
   * @param fileName 파일명
   */
  public static void setHeaderFileName(HttpServletResponse response, String fileName) {
    if (StringUtils.isEmpty(fileName)) {
      return;
    }

    String encodFileName = RestUtil.encodeFileName(fileName);
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; fileName=\"" + encodFileName + "\"; charset=UTF-8");
  }

  /**
   * 바이너리 content 헤더
   *
   * <p>HttpHeaders.CONTENT_TYPE HttpHeaders.CONTENT_ENCODING
   *
   * @param response 응답
   */
  public static void setHeaderBinary(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
    response.setHeader(HttpHeaders.CONTENT_ENCODING, "binary");
  }

  /**
   * Zip content 헤더
   *
   * @param response 응답
   */
  public static void setHeaderZip(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/zip");
  }

  /**
   * 텍스트 content 헤더
   *
   * <p>HttpHeaders.CONTENT_TYPE
   *
   * @param response 응답
   */
  public static void setHeaderText(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
  }

  /**
   * 텍스트 content 헤더
   *
   * <p>HttpHeaders.CONTENT_TYPE
   *
   * @param response 응답
   */
  public static void setHeaderAudio(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CONTENT_TYPE, "audio/wav");
  }

  /**
   * 바이너리 파일 헤더
   *
   * @param response 응답
   * @param fileName 파일명
   */
  public static void setBinaryFileHeader(HttpServletResponse response, String fileName) {
    RestUtil.setHeaderFileName(response, fileName);
    RestUtil.setHeaderBinary(response);
  }

  /**
   * Zip 파일 헤더
   *
   * @param response 응답
   * @param fineName 파일명
   */
  public static void setZipFileHeader(HttpServletResponse response, String fineName) {
    RestUtil.setHeaderFileName(response, fineName);
    RestUtil.setHeaderZip(response);
  }

  /**
   * 텍스트 파일 헤더
   *
   * @param response 응답
   * @param fileName 파일명
   */
  public static void setTextFileHeader(HttpServletResponse response, String fileName) {
    RestUtil.setHeaderText(response);
    RestUtil.setHeaderFileName(response, fileName);
  }

  /**
   * 텍스트 파일 헤더
   *
   * @param response 응답
   * @param fileName 파일명
   * @param bytes 내용
   */
  public static void setTextFileHeader(HttpServletResponse response, String fileName, byte[] bytes)
      throws IOException {
    RestUtil.setHeaderText(response);
    RestUtil.setHeaderFileName(response, fileName);
    response.getOutputStream().write(bytes);
  }

  public static void setWavFileHeader(HttpServletResponse response, String fileName, byte[] bytes)
      throws IOException {
    setHeaderAudio(response);
    setHeaderFileName(response, fileName);
    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
    response.getOutputStream().write(bytes);
  }

  /**
   * 파일명 인코딩.
   *
   * @param fileName 파일명
   * @return 인코딩된 파일명
   */
  public static String encodeFileName(String fileName) {
    String encodeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    return encodeFileName.replaceAll("\\+", "%20");
  }

  /**
   * 에러 response 파일명 : error.txt
   *
   * @param response 응답
   */
  public static void responseError(HttpServletResponse response) {
    RestUtil.responseError(response, null);
  }

  /**
   * 에러 response 파일명 : error.txt
   *
   * @param response 응답
   * @param errMsg 에러 메시지
   */
  public static void responseError(HttpServletResponse response, String errMsg) {
    String cErrMsg = errMsg;
    if (StringUtils.isEmpty(cErrMsg)) {
      cErrMsg = RestUtil.ERROR_FILE_MSG;
    }

    RestUtil.setTextFileHeader(response, RestUtil.ERROR_FILE_NM);

    try {
      PrintWriter printWriter = response.getWriter();
      printWriter.println(cErrMsg);
      response.flushBuffer();
    } catch (IOException ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}
