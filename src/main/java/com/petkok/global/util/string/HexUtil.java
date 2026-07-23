package com.petkok.global.util.string;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/** HexUtil 클래스 HexCode와 관련된 유틸리티 */
public class HexUtil {

  private HexUtil() {}

  private static final char[] HEX_DATA = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };
  private static final int[] HEX_MAP_DATA = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, /* 0 ~ 9 */ -1, -1, -1, -1, -1, -1, -1, /* invalid char */ 10, 11,
    12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, /* A ~ Z */ -1, -1, -1, -1, -1, -1, /* invalid char */ 10, 11, 12, 13, 14, 15, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
  }; /* a ~ z */

  private static final byte[] BYTE_DATA = {
    0x00, 0X01, 0x02, 0x03, 0X04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
  };

  /**
   * byte값을 hexString으로 변경한다.
   *
   * @param data 변경할 byte
   * @param length length
   * @return hexString
   */
  public static String toHex(byte[] data, int length) {
    if (data == null || data.length == 0 || length <= 0) {
      return StringUtils.EMPTY;
    }

    StringBuilder buf = new StringBuilder();

    for (int i = 0; i != length; i++) {
      int v = data[i] & 0xff;

      buf.append(HEX_DATA[v >> 4]);
      buf.append(HEX_DATA[v & 0xf]);
    }

    return buf.toString();
  }

  /**
   * byte값을 hexString으로 변경한다.
   *
   * @param data 변경할 byte
   * @return hexString
   */
  public static String toHex(byte[] data) {
    if (data == null || data.length == 0) {
      return StringUtils.EMPTY;
    }

    return toHex(data, data.length);
  }

  /**
   * String 값을 hexString으로 변경한다.
   *
   * @param data 변경할 byte
   * @return hexString
   */
  public static String toHex(String data) {
    return toHex(data, "UTF-8");
  }

  /**
   * String 값을 hexString으로 변경한다.
   *
   * @param data 변경할 String
   * @param charsetName charset
   * @return hexString
   */
  public static String toHex(String data, String charsetName) {
    if (data == null || "".equals(data.trim())) {
      return StringUtils.EMPTY;
    }
    try {
      byte[] bytes = data.getBytes(charsetName);
      return toHex(bytes, bytes.length);
    } catch (Exception ex) {
      return StringUtils.EMPTY;
    }
  }

  /**
   * HexString 값을 byte로 변경한다.
   *
   * @param hexString 변경할 HexString
   * @return byte
   */
  public static byte[] toBytes(String hexString) {
    if (hexString == null || hexString.trim().equals("")) {
      return new byte[0];
    }

    byte[] originalBytes = new byte[hexString.length() / 2 + (hexString.length() % 2)];
    for (int i = 0; i < originalBytes.length; i++) {
      char hexChar1 = hexString.charAt(i * 2);
      char hexChar2 = hexString.charAt((i * 2) + 1);
      originalBytes[i] =
          (byte)
              ((BYTE_DATA[HEX_MAP_DATA[hexChar1 - 48]] << 4)
                  | BYTE_DATA[HEX_MAP_DATA[hexChar2 - 48]]);
    }
    return originalBytes;
  }

  /**
   * HexString Character 값을 byte로 변경한다.
   *
   * @param hexString 변경할 Character
   * @return byte
   */
  public static byte[] toBytes(char[] hexString) {
    if (hexString == null
        || hexString.length == 0
        || Arrays.toString(hexString).trim().equals("")) {
      return new byte[0];
    }

    byte[] originalBytes = new byte[hexString.length / 2 + (hexString.length % 2)];
    for (int i = 0; i < originalBytes.length; i++) {
      char hexChar1 = hexString[i * 2];
      char hexChar2 = hexString[(i * 2) + 1];
      originalBytes[i] =
          (byte)
              ((BYTE_DATA[HEX_MAP_DATA[hexChar1 - 48]] << 4)
                  | BYTE_DATA[HEX_MAP_DATA[hexChar2 - 48]]);
    }
    return originalBytes;
  }
}
