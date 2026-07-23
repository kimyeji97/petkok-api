package com.petkok.global.util.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** Date 변환 유틸리티 */
public class DateConvertUtil {

  private DateConvertUtil() {}

  public static Calendar convertDateToCalendar(Date date) {
    if (date == null) {
      return null;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    return cal;
  }

  @Deprecated
  public static Date convertCalendarToDate(Calendar cal) {
    if (cal == null) {
      return null;
    }
    return new Date(cal.getTimeInMillis());
  }

  /**
   * Date to LocalDate
   *
   * @param date 변환할 날짜
   * @return LocalDate
   */
  public static LocalDate convertDateToLocalDate(Date date) {
    if (date == null) {
      return null;
    }

    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  /**
   * Date to LocalDate with time zone
   *
   * @param date 변환할 날짜
   * @param timeZone 타임존
   * @return LocalDate
   */
  public static LocalDate convertDateToLocalDate(Date date, TimeZone timeZone) {
    if (date == null) {
      return null;
    }

    return date.toInstant().atZone(timeZone.toZoneId()).toLocalDate();
  }

  /**
   * Date to LocalDateTime
   *
   * @param date 변환할 날짜
   * @return LocalDateTime
   */
  public static LocalDateTime convertDateToLocalDateTime(Date date) {
    if (date == null) {
      return null;
    }

    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  /**
   * Date to LocalDateTime with time zone
   *
   * @param date 변환할 날짜
   * @param timeZone 타임존
   * @return LocalDateTime
   */
  public static LocalDateTime convertDateToLocalDateTime(Date date, TimeZone timeZone) {
    if (date == null) {
      return null;
    }

    return date.toInstant().atZone(timeZone.toZoneId()).toLocalDateTime();
  }
}
