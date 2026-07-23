package com.petkok.global.util.spring;

import com.petkok.global.util.ReflectionUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/** Spring 주석 관련 유틸리티 */
@Slf4j
public class AnnotationUtil {

  private AnnotationUtil() {}

  /**
   * Annotation을 찾는다.
   *
   * @param className 클래스명
   * @param annotationType annotationType
   * @return 찾은 annotation
   * @throws ClassNotFoundException 클래스를 찾지 못한 경우
   */
  public static <A extends Annotation> A findAnnotation(String className, Class<A> annotationType)
      throws ClassNotFoundException {
    try {
      Class<?> clazz = Class.forName(className);
      return clazz.getAnnotation(annotationType);
    } catch (ClassNotFoundException e) {
      log.error("Cannot find class -> {} ", className);
      throw e;
    }
  }

  /**
   * Annotation을 찾는다.
   *
   * @param className 클래스명
   * @param annotationType annotationType
   * @return 찾은 annotation 배열
   * @throws ClassNotFoundException 클래스를 찾지 못한 경우
   */
  public static <A extends Annotation> A[] findAnnotations(
      String className, Class<A> annotationType) throws ClassNotFoundException {
    try {
      Class<?> clazz = Class.forName(className);
      return clazz.getAnnotationsByType(annotationType);
    } catch (ClassNotFoundException e) {
      log.error("Cannot find class -> {} ", className);
      throw e;
    }
  }

  /**
   * 해당 클래스의 해당 key가 가진 모든 Annotation을 리턴
   *
   * @param t 객체
   * @param key 필드 이름
   * @param <T> 클래스
   * @return 소유한 주석
   */
  public static <T> List<Annotation> getFieldAnnotations(T t, String key) {
    if (t == null || ObjectUtils.isEmpty(key)) {
      return null;
    }

    List<Field> lf = ReflectionUtil.findAllFields(t);
    for (Field f : lf) {
      if (f.getName().equals(key)) {
        return Arrays.asList(f.getAnnotations());
      }
    }

    return null;
  }
}
