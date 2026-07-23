package com.petkok.global.util.string;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** 마스킹처리 유틸리티 */
public class MaskingUtil {

  private MaskingUtil() {}

  /**
   * 이름 가운데 글자 마스킹
   *
   * @param name 이름
   * @return 마스킹된 이름
   */
  public static String maskingMiddleName(String name) {
    if (name == null) {
      return null;
    }
    // 한글만 (영어, 숫자 포함 이름은 제외)
    String regex = "(^[가-힣]+)$";

    Matcher matcher = Pattern.compile(regex).matcher(name);
    if (matcher.find()) {
      int length = name.length();

      String middleMask;
      if (length > 2) {
        middleMask = name.substring(1, length - 1);
      } else {
        // 이름이 외자
        middleMask = name.substring(1, length);
      }

      StringBuilder dot = new StringBuilder();
      for (int i = 0; i < middleMask.length(); i++) {
        dot.append("*");
      }

      if (length > 2) {
        return name.charAt(0)
            + middleMask.replace(middleMask, dot.toString())
            + name.substring(length - 1, length);
      } else {
        // 이름이 외자 마스킹 리턴
        return name.charAt(0) + middleMask.replace(middleMask, dot.toString());
      }
    }
    return name;
  }

  /**
   * Second 이름 마스킨
   *
   * @param name 이름
   * @return 마스킹된 이름
   */
  public static String maskingSecondName(String name) {
    if (StringUtils.isEmpty(name)) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(name.charAt(0));
    sb.append("*".repeat(name.length() - 1));
    return sb.toString();
  }

  /**
   * 핸드폰번호 가운데 마스킹
   *
   * @param phoneNo 전화번호
   * @return 마스킹된 전화번호
   */
  public static String maskingMiddlePhoneNo(String phoneNo) {
    String regex = "(\\d{2,3})-?(\\d{3,4})-?(\\d{4})$";

    Matcher matcher = Pattern.compile(regex).matcher(phoneNo);
    if (matcher.find()) {
      String target = matcher.group(2);
      int length = target.length();
      char[] c = new char[length];
      Arrays.fill(c, '*');

      return phoneNo.replace(target, String.valueOf(c));
    }
    return phoneNo;
  }
}
