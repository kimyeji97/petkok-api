package com.petkok.global.util.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/** Zip (압축) 클래스 Zip 기능을 제공한다. */
@Slf4j
public class ZipUtil {

  private ZipUtil() {}

  /**
   * 압축을 해제한다.
   *
   * @param zippedFile 압축 파일
   */
  public static void unzip(File zippedFile) throws IOException {
    unzip(zippedFile, Charset.defaultCharset().name());
  }

  /**
   * 압축을 해제한다.
   *
   * @param zippedFile 압축 파일
   * @param charsetName charsetName
   */
  public static void unzip(File zippedFile, String charsetName) throws IOException {
    unzip(zippedFile, zippedFile.getParentFile(), charsetName);
  }

  /**
   * 압축을 해제한다.
   *
   * @param zippedFile 압축 파일
   * @param destDir 압축 해제 위치
   */
  public static void unzip(File zippedFile, File destDir) throws IOException {
    unzip(new FileInputStream(zippedFile), destDir, Charset.defaultCharset().name());
  }

  /**
   * 압축을 해제한다.
   *
   * @param zippedFile 압축 파일
   * @param destDir 압축 해제 위치
   * @param charsetName charsetName
   */
  public static void unzip(File zippedFile, File destDir, String charsetName) throws IOException {
    unzip(new FileInputStream(zippedFile), destDir, charsetName);
  }

  /**
   * 압축을 해제한다.
   *
   * @param is 압축 파일 InputStream
   * @param destDir 압축 해제 위치
   */
  public static void unzip(InputStream is, File destDir) throws IOException {
    unzip(is, destDir, Charset.defaultCharset().name());
  }

  /**
   * 압축을 해제한다.
   *
   * @param is 압축 파일 InputStream
   * @param destDir 압축 해제 위치
   * @param charsetName charsetName
   */
  public static void unzip(InputStream is, File destDir, String charsetName) throws IOException {
    try (ZipArchiveInputStream zis = new ZipArchiveInputStream(is, charsetName, false)) {
      ZipArchiveEntry entry;
      while ((entry = zis.getNextZipEntry()) != null) {
        unzipEntry(zis, destDir, entry);
      }
    }
  }

  private static void unzipEntry(ZipArchiveInputStream zis, File destDir, ZipArchiveEntry entry)
      throws IOException {
    String name = entry.getName();
    File target = new File(destDir, name);
    if (entry.isDirectory()) {
      log.info("dir  : " + name);
      log.info(target.getAbsolutePath());
      target.mkdirs();
      return;
    }

    File parent = target.getParentFile();
    if (!parent.exists()) {
      parent.mkdirs();
    }

    target.createNewFile();
    byte[] buf = new byte[1024 * 8];
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target))) {
      int nWritten;
      while ((nWritten = zis.read(buf)) >= 0) {
        bos.write(buf, 0, nWritten);
      }
    } catch (IOException e) {
      log.error(e.toString(), e);
    }
    log.debug("file : " + name);
  }

  /**
   * 파일 or 디렉토리를 압축한다.
   *
   * @param src file or directory
   * @throws IOException 예외
   */
  public static void zip(File src) throws IOException {
    zip(src, Charset.defaultCharset().name(), true);
  }

  /**
   * 파일 or 디렉토리를 압축한다.
   *
   * @param src file or directory to compress
   * @param includeSrc if true and src is directory, then src is not included in the compression. if
   *     false, src is included.
   * @throws IOException 예외
   */
  public static void zip(File src, boolean includeSrc) throws IOException {
    zip(src, Charset.defaultCharset().name(), includeSrc);
  }

  /**
   * 파일 or 디렉토리를 압축한다. with the given encoding
   *
   * @param src 대상
   * @param charSetName 문자셋
   * @param includeSrc 포함 여부
   * @throws IOException 예외
   */
  public static void zip(File src, String charSetName, boolean includeSrc) throws IOException {
    zip(src, src.getParentFile(), charSetName, includeSrc);
  }

  /**
   * 파일 or 디렉토리를 압축한다. writes to the given output stream
   *
   * @param src 대상
   * @param os 출력 스트림
   * @throws IOException 예외
   */
  public static void zip(File src, OutputStream os) throws IOException {
    zip(src, os, Charset.defaultCharset().name(), true);
  }

  /**
   * 파일 or 디렉토리를 압축한다. create the compressed file under the given destDir.
   *
   * @param src 대상
   * @param destDir 대상 디렉토리
   * @param charSetName 문자셋
   * @param includeSrc 포함 여부
   * @throws IOException 예외
   */
  public static void zip(File src, File destDir, String charSetName, boolean includeSrc)
      throws IOException {
    String fileName = src.getName();
    if (!src.isDirectory()) {
      int pos = fileName.lastIndexOf(".");
      if (pos > 0) {
        fileName = fileName.substring(0, pos);
      }
    }
    fileName += ".zip";

    File zippedFile = new File(destDir, fileName);
    if (!zippedFile.exists()) {
      zippedFile.createNewFile();
    }
    zip(src, new FileOutputStream(zippedFile), charSetName, includeSrc);
  }

  /**
   * 파일 or 디렉토리를 압축한다. writes to the given output stream
   *
   * @param src 대상
   * @param os 출력 스트림
   * @param charsetName 문자셋
   * @param includeSrc 포함 여부
   * @throws IOException 예외
   */
  public static void zip(File src, OutputStream os, String charsetName, boolean includeSrc)
      throws IOException {
    try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(os)) {
      zos.setEncoding(charsetName);

      Stack<File> stack = new Stack<>();
      File root;
      if (src.isDirectory()) {
        if (includeSrc) {
          stack.push(src);
          root = src.getParentFile();
        } else {
          for (File f : src.listFiles()) {
            stack.push(f);
          }
          root = src;
        }
      } else {
        stack.push(src);
        root = src.getParentFile();
      }

      while (!stack.isEmpty()) {
        zipEntry(zos, root, stack.pop(), stack);
      }
    }
  }

  private static void zipEntry(ZipArchiveOutputStream zos, File root, File f, Stack<File> stack)
      throws IOException {
    String name = toPath(root, f);
    if (f.isDirectory()) {
      log.debug("dir  : " + name);
      for (File child : f.listFiles()) {
        if (child.isDirectory()) {
          stack.push(child);
        } else {
          stack.add(0, child);
        }
      }
      return;
    }

    log.debug("file : " + name);
    ZipArchiveEntry ze = new ZipArchiveEntry(name);
    zos.putArchiveEntry(ze);
    try (FileInputStream fis = new FileInputStream(f)) {
      byte[] buf = new byte[8 * 1024];
      int length;
      while ((length = fis.read(buf, 0, buf.length)) >= 0) {
        zos.write(buf, 0, length);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      zos.closeArchiveEntry();
    }
  }

  private static String toPath(File root, File dir) {
    String path = dir.getAbsolutePath();
    path = path.substring(root.getAbsolutePath().length()).replace(File.separatorChar, '/');
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if (dir.isDirectory() && !path.endsWith("/")) {
      path += "/";
    }
    return path;
  }
}
