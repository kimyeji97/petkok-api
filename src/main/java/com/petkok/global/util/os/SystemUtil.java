package com.petkok.global.util.os;

import org.apache.commons.lang3.StringUtils;

public class SystemUtil {

  private SystemUtil() {}

  public enum OsType {
    WINDOWS,
    MAC,
    LINUX,
    UNIX,
    SOLARIS,
    UNKNOWN
  }

  public static String getOsNameUpper() {
    return System.getProperty("os.name").toUpperCase();
  }

  public static OsType getOsType() {
    return switch (SystemUtil.getOsNameUpper()) {
      case "WINDOWS" -> OsType.WINDOWS;
      case "MAC" -> OsType.MAC;
      case "LINUX" -> OsType.LINUX;
      case "UNIX" -> OsType.UNIX;
      case "SOLARIS" -> OsType.SOLARIS;
      default -> OsType.UNKNOWN;
    };
  }

  public static boolean isLocal() {
    String property = System.getProperty("spring.profiles.active");
    return StringUtils.equalsIgnoreCase(property, "local");
  }
}
