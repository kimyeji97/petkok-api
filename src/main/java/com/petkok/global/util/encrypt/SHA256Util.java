package com.petkok.global.util.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA256 암호화 유틸리티 */
public class SHA256Util {

  private SHA256Util() {}

  public static final String ALGORITHM = "SHA-256";

  /**
   * 암호화
   *
   * @param str 대상 문자열
   * @return 암호화된 문자열
   * @throws NoSuchAlgorithmException 예외
   */
  public static String encrypt(String str) throws NoSuchAlgorithmException {
    StringBuilder encryptData = new StringBuilder();
    MessageDigest sha = MessageDigest.getInstance(ALGORITHM);
    sha.update(str.getBytes());

    byte[] digest = sha.digest();
    for (byte b : digest) {
      encryptData.append(String.format("%02x", b & 0xFF));
    }
    return encryptData.toString();
  }
}
