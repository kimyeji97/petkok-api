package com.petkok.global.util.file;

import com.petkok.global.util.os.SystemUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/** 파일과 관련된 유틸리티 */
@Slf4j
public class FileUtil {

  private FileUtil() {}

  public static final String SORT_NAME = "name";
  public static final String SORT_MODIFIED_DT = "date";

  /**
   * 파일의 확장자를 추출한다.
   *
   * @param fileName 파일명
   * @return 파일 확장자명
   */
  public static String getExtension(String fileName) {
    if (fileName != null && !fileName.trim().equals("")) {
      String fileSeparator = "\\"; // Windows format
      if (fileName.startsWith("/")) { // Unix format
        fileSeparator = "/";
      }
      if (fileName.lastIndexOf(fileSeparator) != -1) {
        fileName = fileName.substring(fileName.lastIndexOf(fileSeparator) + 1);
      }
      if (fileName.lastIndexOf(".") != -1) {
        return fileName.substring(fileName.lastIndexOf("."));
      }
    }

    return "";
  }

  /**
   * 파일명을 추출한다.
   *
   * @param path 경로
   * @param withExtension 확자자 포함 여부
   * @return 파일명
   */
  public static String getFileName(String path, boolean withExtension) {
    if (StringUtils.isBlank(path)) {
      return null;
    }

    if (!path.contains(".")) {
      log.error("This is directory path.");
    }

    SystemUtil.OsType osType = SystemUtil.getOsType();
    String fileSeparator = osType == SystemUtil.OsType.WINDOWS ? "\\" : "/";
    int nameStartIdx = path.lastIndexOf(fileSeparator);

    return withExtension
        ? path.substring(nameStartIdx + 1)
        : path.substring(nameStartIdx + 1, path.lastIndexOf("."));
  }

  /**
   * 디렉토리 경로를 추출
   *
   * @param path 경로
   * @return 디렉토리 경로
   */
  public static String getDirPath(String path) {
    if (StringUtils.isBlank(path)) {
      return null;
    }

    SystemUtil.OsType osType = SystemUtil.getOsType();
    String fileSeparator = osType == SystemUtil.OsType.WINDOWS ? "\\" : "/";

    String dirPath = path;
    if (path.contains(".")) {
      int nameStartIdx = path.lastIndexOf(fileSeparator);
      dirPath = path.substring(0, nameStartIdx);
    }

    return dirPath.endsWith(fileSeparator) ? dirPath : dirPath + fileSeparator;
  }

  /**
   * 파일을 복사 한다.
   *
   * @param srcFp 원본 파일
   * @param destFp 복사 대상 파일
   * @return 성공여부 (boolean)
   */
  public static boolean copy(File srcFp, File destFp) {
    try (FileInputStream in = new FileInputStream(srcFp);
        FileOutputStream out = new FileOutputStream(destFp);
        BufferedInputStream inBuffer = new BufferedInputStream(in);
        BufferedOutputStream outBuffer = new BufferedOutputStream(out)) {

      int numofbytes;
      byte[] buffer = new byte[8192];
      while ((numofbytes = inBuffer.read(buffer, 0, buffer.length)) > -1) {
        outBuffer.write(buffer, 0, numofbytes);
      }
      outBuffer.flush();
      // cleanup if files are not the same length
      if (srcFp.length() != destFp.length()) {
        destFp.delete();
        return false;
      }

      return true;
    } catch (Exception e) {
      destFp.delete();
      return false;
    }
  }

  /**
   * 파일을 이동 한다.
   *
   * @param srcFp 원본 파일
   * @param destFp 이동 대상 파일
   * @return 성공여부 (boolean)
   */
  public static boolean move(File srcFp, File destFp) {
    if (srcFp.renameTo(destFp)) {
      return true;
    }

    try {
      if (destFp.createNewFile()) {
        // delete if copy was successful, otherwise move will fail
        if (copy(srcFp, destFp)) {
          return srcFp.delete();
        }
      }
    } catch (IOException ex) {
      return false;
    }

    return false;
  }

  /**
   * 파일을 이동 한다.
   *
   * @param fp 원본 파일
   * @param path 이동 대상 Path
   * @return 성공여부 (boolean)
   */
  public static boolean move(File fp, String path) {
    File newFp = new File(path);
    if (fp.renameTo(newFp)) {
      return true;
    }

    try {
      if (newFp.createNewFile()) {
        // delete if copy was successful, otherwise move will fail
        if (copy(fp, newFp)) {
          return fp.delete();
        }
      }
    } catch (IOException ex) {
      return false;
    }

    return false;
  }

  /**
   * 디렉토리를 생성한다. (하위 디렉토리가 없다면 생성한다.)
   *
   * @param path 생성할 디렉토리 path
   * @return 성공여부 (boolean)
   */
  public static boolean makeDirs(String path) {
    if (path == null) {
      return false;
    }
    File fp = new File(path);
    if (fp.exists()) {
      return true;
    }
    return fp.mkdirs();
  }

  /**
   * 디렉토리를 생성한다.
   *
   * @param path 생성할 디렉토리 path
   * @param create true이면 하위 디렉토리가 없다면 생성하고, false면 생성하지 않는다.
   * @return 성공여부 (boolean)
   */
  public static boolean makeDir(String path, boolean create) {
    if (path == null) {
      return false;
    }
    File fp = new File(path);

    if (create) {
      if (fp.exists()) {
        return true;
      }
      return fp.mkdirs();
    } else {
      return fp.mkdir();
    }
  }

  /**
   * 파일을 삭제한다.
   *
   * @param file 삭제할 파일 path
   * @return 성공여부 (boolean)
   */
  public static boolean remove(String file) {
    if (file == null) {
      return true;
    }
    File fp = new File(file);

    if (fp.exists()) {
      return fp.delete();
    }
    return true;
  }

  /**
   * 파일을 삭제한다.
   *
   * @param fp 삭제할 파일
   * @return 성공여부 (boolean)
   */
  public static boolean remove(File fp) {
    if (fp != null && fp.exists()) {
      return fp.delete();
    }
    return true;
  }

  /**
   * 파일이 존재하는 지 확인한다.
   *
   * @param file 확인할 파일 Path
   * @return 존재 여부 (boolean)
   */
  public static boolean exists(String file) {
    if (file == null) {
      return false;
    }
    File fp = new File(file);
    return fp.exists();
  }

  /**
   * 파일이 존재하는 지 확인한다.
   *
   * @param file 파일 객체
   * @return 존재 여부 (boolean)
   */
  public static boolean exists(File file) {
    return file != null && file.exists();
  }

  /**
   * 존재하는 파일 반환
   *
   * <pre>
   *     존재하지 않는 경우 null을 반환합니다.
   * </pre>
   *
   * @param path 대상 경로
   * @return File or null
   */
  public static File getExistingFile(String path) {
    if (path == null) {
      return null;
    }
    File fp = new File(path);
    return fp.exists() ? fp : null;
  }

  /**
   * 파일의 크기를 확인한다.
   *
   * @param file 확인할 파일 Path
   * @return 파일크기
   */
  public static long size(String file) {
    if (file == null) {
      return 0;
    }
    File fp = new File(file);
    if (fp.exists()) {
      return fp.length();
    }
    return 0;
  }

  /**
   * MD5 체크섬을 확인한다.
   *
   * @param fp 확인할 파일
   * @return 체크섬 byte
   */
  public static byte[] getMd5(File fp) {
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fp))) {
      MessageDigest md = MessageDigest.getInstance("MD5");
      int numofbytes;
      byte[] buffer = new byte[8192];
      while ((numofbytes = in.read(buffer, 0, buffer.length)) > -1) {
        md.update(buffer, 0, numofbytes);
      }
      return md.digest();
    } catch (Exception ex) {
      return new byte[0];
    }
  }

  /**
   * MD5 체크섬을 확인한다.
   *
   * @param filePath 확인할 파일 Path
   * @return 체크섬 byte
   */
  public static byte[] getMd5(String filePath) {
    return getMd5(new File(filePath));
  }

  /**
   * 파일의 내용을 byte로 리턴한다.
   *
   * @param path 확인할 파일 Path
   * @return 파일 내용 byte
   */
  public static byte[] toBytes(String path) throws Exception {
    if (path == null) {
      return new byte[0];
    }
    try (BufferedInputStream bsi = new BufferedInputStream(new FileInputStream(path));
        ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int numOfBytes;
      while ((numOfBytes = bsi.read(buffer, 0, buffer.length)) != -1) {
        bao.write(buffer, 0, numOfBytes);
      }
      return bao.toByteArray();
    }
  }

  /**
   * 파일의 내용을 byte로 리턴한다.
   *
   * @param in 확인할 파일의 InputStream
   * @return 파일 내용 byte
   */
  public static byte[] streamToBytes(InputStream in) throws IOException {
    try (BufferedInputStream bsi = new BufferedInputStream(in);
        ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int numOfBytes;
      while ((numOfBytes = bsi.read(buffer, 0, buffer.length)) != -1) {
        bao.write(buffer, 0, numOfBytes);
      }
      return bao.toByteArray();
    } catch (Exception ex) {
      return new byte[0];
    }
  }

  /**
   * 파일의 내용을 byte로 리턴한다.
   *
   * @param fp 확인할 파일
   * @return 파일 내용 byte
   */
  public static byte[] fileToBytes(File fp) throws IOException {
    try (BufferedInputStream bsi = new BufferedInputStream(new FileInputStream(fp));
        ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int numOfBytes;
      while ((numOfBytes = bsi.read(buffer, 0, buffer.length)) != -1) {
        bao.write(buffer, 0, numOfBytes);
      }
      return bao.toByteArray();
    } catch (Exception ex) {
      return new byte[0];
    }
  }

  /**
   * 파일의 내용을 String 으로 리턴한다.
   *
   * @param fp 확인할 파일
   * @return 파일 내용
   */
  public static String fileToString(File fp) throws IOException {
    return fileToString(fp, Charset.forName("UTF-8"));
  }

  /**
   * 파일의 내용을 String 으로 리턴한다.
   *
   * @param fp 확인할 파일
   * @param charset charset
   * @return 파일 내용
   */
  public static String fileToString(File fp, Charset charset) throws IOException {
    try (BufferedInputStream bsi = new BufferedInputStream(new FileInputStream(fp));
        ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int numOfBytes;
      while ((numOfBytes = bsi.read(buffer, 0, buffer.length)) != -1) {
        bao.write(buffer, 0, numOfBytes);
      }
      return new String(bao.toByteArray(), charset);
    } catch (Exception ex) {
      return "";
    }
  }

  /**
   * 해당 파일을 line 단위로 읽어서 처리한다.
   *
   * @param fp 파일
   * @param process line 단위로 읽어서 처리하는 프로세서
   * @throws IOException 예외
   */
  public static void readAndProcessByLine(File fp, Consumer<String> process) throws IOException {
    try (BufferedReader bis = new BufferedReader(new FileReader(fp))) {
      String line;
      while ((line = bis.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        process.accept(line);
      }
    } catch (IOException ex) {
      log.error(ex.getMessage());
      throw ex;
    }
  }

  /**
   * 파일을 저장한다.
   *
   * @param path 저장할 파일 path
   * @param bytes 파일의 내용
   */
  public static void saveBytesToFile(String path, byte[] bytes) throws Exception {
    saveBytesToFile(path, bytes, false);
  }

  /**
   * 파일에 바이트를 쓰고 저장한다.
   *
   * <pre>
   *     BufferedOutputStream를 사용
   *     파일에 byte를 쓰고 저장
   * </pre>
   *
   * @param path 저장할 파일 path
   * @param bytes 파일의 내용
   * @param append 존재하는 파일에 내용을 append 할지 여부
   */
  public static void saveBytesToFile(String path, byte[] bytes, boolean append) throws Exception {
    if (path == null) {
      return;
    }
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path, append))) {
      bos.write(bytes);
      bos.flush();
    }
  }

  /**
   * 파일에 Stream에 담긴 데이터를 쓰고 저장한다.
   *
   * <pre>
   *     BufferedOutputStream, BufferedInputStream 사용
   *     파일에 Stream에 담긴 데이터를 byte로 쓴다.
   * </pre>
   *
   * @param path 저장할 파일 path
   * @param in 파일 InputStream
   */
  public static void saveStreamToFile(String path, InputStream in) throws IOException {
    if (in == null) {
      return;
    }
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
        BufferedInputStream inBuffer = new BufferedInputStream(in)) {
      int numofbytes;
      byte[] buffer = new byte[8192];
      while ((numofbytes = inBuffer.read(buffer, 0, buffer.length)) > -1) {
        bos.write(buffer, 0, numofbytes);
      }
      bos.flush();
    }
  }

  /**
   * 파일에 텍스트를 쓰고 저장한다.
   *
   * <pre>
   *     PrintWriter 사용
   *     파일에 텍스트를 입력
   * </pre>
   *
   * @param fullPath 파일 전체 경로
   * @param list 작성 대상 리스트
   * @param convertLine String으로 변환
   * @param <T> 리스트 요소 타입
   */
  public static <T> void saveListToFile(
      String fullPath, List<T> list, Function<T, String> convertLine) {
    if (StringUtils.isEmpty(fullPath)) {
      log.error("file write target fullPath is empty.");
      return;
    }
    if (convertLine == null) {
      log.error("convertLine is null.");
      return;
    }

    try (FileWriter fw = new FileWriter(fullPath);
        PrintWriter pw = new PrintWriter(fw)) {
      for (T obj : list) {
        String line = convertLine.apply(obj);
        if (StringUtils.isBlank(line)) {
          continue;
        }
        pw.println(line);
      }
      pw.flush();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * 파일의 스트림을 닫는다.
   *
   * @param in 파일 InputStream
   */
  public static void closeStream(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (Exception ex) {
      log.debug("stream close 실패", ex);
    }
  }

  /**
   * 파일의 스트림을 닫는다.
   *
   * @param out 파일 OutputStream
   */
  public static void closeStream(OutputStream out) {
    if (out == null) {
      return;
    }
    try {
      out.flush();
    } catch (Exception ignored) {
      log.debug("stream flush 실패", ignored);
    }
    try {
      out.close();
    } catch (Exception ignored) {
      log.debug("stream close 실패", ignored);
    }
  }

  /**
   * 파일의 Reader를 닫는다.
   *
   * @param in 파일 Reader
   */
  public static void closeStream(Reader in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (Exception ex) {
      log.debug("reader close 실패", ex);
    }
  }

  /**
   * 파일의 Writer를 닫는다.
   *
   * @param out 파일 Writer
   */
  public static void closeStream(Writer out) {
    if (out == null) {
      return;
    }
    try {
      out.flush();
    } catch (Exception ignored) {
      log.debug("writer flush 실패", ignored);
    }
    try {
      out.close();
    } catch (Exception ignored) {
      log.debug("writer close 실패", ignored);
    }
  }

  /**
   * 파일의 Path를 UnixFormat 으로 Convert한다.
   *
   * @param path 변경할 Path
   * @return 변경된 path
   */
  public static String convertPathToUnixFormat(String path) {
    if (path == null) {
      return "";
    }
    path = path.trim();
    path = path.replaceAll("\\\\", "/");
    path = path.replaceAll("(/)+", "/");
    return path;
  }

  /**
   * read files in directory
   *
   * @param path 경로
   * @param filter 필터
   * @return 파일 목록
   */
  public static File[] readFiles(String path, FilenameFilter filter) {
    if (StringUtils.isEmpty(path)) {
      return null;
    }

    File dir = new File(path);

    if (!dir.exists()) {
      return null;
    }

    File[] files = filter == null ? dir.listFiles() : dir.listFiles(filter);

    log.info(path + " > find file count : " + (files == null ? 0 : files.length));
    return files;
  }

  /**
   * 파일 리스트를 이름 또는 수정날짜로 정렬한다. desc가 true이면 내림차순, false면 오름차순 정렬 type은 {@link FileUtil#SORT_NAME} 이름
   * 또는 {@link FileUtil#SORT_MODIFIED_DT} 수정날짜
   *
   * @param files 대상 파일 목록
   * @param type 정렬 기준
   * @param desc 내림차순 여부
   */
  public static void sortFiles(File[] files, String type, boolean desc) {
    if (ObjectUtils.isEmpty(files) || files.length == 1) {
      return;
    }

    Arrays.sort(
        files,
        (arg0, arg1) -> {
          String s1 = "";
          String s2 = "";

          if (type.equals(SORT_NAME)) {
            s1 = arg0.getName();
            s2 = arg1.getName();
          } else if (type.equals(SORT_MODIFIED_DT)) {
            s1 = String.valueOf(arg0.lastModified());
            s2 = String.valueOf(arg1.lastModified());
          }

          return desc ? s2.compareTo(s1) : s1.compareTo(s2);
        });
  }

  /**
   * 파일들을 해당 경로로 이동시킨다. -&gt; 디렉토리가 존재하지 않으면 생성
   *
   * @param path 대상 경로
   * @param listFile 이동할 파일 목록
   */
  public static void moveFiles(String path, List<File> listFile) {
    if (StringUtils.isEmpty(path)) {
      log.error("file move target path is empty.");
      return;
    }

    if (!FileUtil.exists(path)) {
      FileUtil.makeDir(path, true);
    }

    if (ObjectUtils.isEmpty(listFile)) {
      return;
    }

    for (File file : listFile) {
      try {
        Files.move(Paths.get(file.getAbsolutePath()), Paths.get(path, file.getName()));
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        return;
      }
    }
  }

  /**
   * 파일을 해당 경로로 이동시킨다. -&gt; 디렉토리가 존재하지 않으면 생성한다.
   *
   * @param path 대상 경로
   * @param file 대상 파일
   */
  public static void moveFile(String path, File file) {
    if (StringUtils.isEmpty(path)) {
      log.error("file move target path is empty.");
      return;
    }

    if (!FileUtil.exists(path)) {
      FileUtil.makeDir(path, true);
    }

    try {
      Files.move(Paths.get(file.getAbsolutePath()), Paths.get(path, file.getName()));
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }
}
