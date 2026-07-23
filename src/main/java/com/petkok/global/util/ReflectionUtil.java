package com.petkok.global.util;

import com.petkok.global.util.map.MapUtil;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/** Java reflection 기능 사용 유틸리티 */
@Slf4j
public class ReflectionUtil {

  private ReflectionUtil() {}

  private static final String PATTERN_LIST_INDEX = "\\[[0-9]+\\]";
  private static final String SPLIT_CHAR_KEY = "\\.";
  private static final Class<?>[] EMPTY_CLAZZ = new Class<?>[] {};

  /**
   * String 및 String 관련 클래스 확인.
   *
   * @param clz 확인할 클래스
   * @return String, StringBuffer 또는 StringBuilder 클래스인경우 true, 아니면 false.
   */
  public static boolean isStringType(Class<?> clz) {
    if (clz == null) {
      return false;
    }
    return clz == String.class || clz == StringBuffer.class || clz == StringBuilder.class;
  }

  /**
   * Boolean유형의 클래스인지 확인한다.
   *
   * @param clz 클래스.
   * @return Boolean 또는 boolean이면 true.
   */
  public static boolean isBooleanType(Class<?> clz) {
    return clz == boolean.class || clz == Boolean.class;
  }

  /**
   * Date 유형의 클래스 인지 확인하여 줍니다.
   *
   * @param clz 비교 클래스.
   * @return Date, Calendar 또는 Calendar에 할당할 수 있는 클래스 이면 true.
   */
  public static boolean isDateType(Class<?> clz) {
    return clz == Date.class || clz == java.sql.Date.class || Calendar.class.isAssignableFrom(clz);
  }

  /**
   * Array또는 List인지 확인하여 줍니다.
   *
   * @param clz 비교 클래스.
   * @return Array 또는 List객체이면 true.
   */
  public static boolean isArrayOrListType(Class<?> clz) {
    return List.class.isAssignableFrom(clz) || clz.isArray();
  }

  /**
   * String class도 primitive 타입으로 처리한다.
   *
   * @param clz 비교 클래스.
   * @return String, primitive 타입 또는 primitive wrapper 클래스인 경우 true 를 리턴, 아니면 false.
   */
  public static boolean isPrimitive(Class<?> clz) {
    if (clz.isPrimitive()) {
      return true;
    }
    return clz == Integer.class
        || clz == Byte.class
        || clz == Short.class
        || clz == Double.class
        || clz == Float.class
        || clz == Long.class
        || clz == Boolean.class
        || clz == String.class;
  }

  /**
   * 클래스에 속한 Property Descriptor 목록을 구해 줍니다.
   *
   * @param clazz 검색하는 클래스.
   * @return 클래스에 속한 모든 프로퍼티 목록 ({@link PropertyDescriptor} List)
   */
  public static List<PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) {
    if (clazz == null) {
      return null;
    }

    if (ReflectionUtil.isPrimitive(clazz)) {
      return null;
    }

    List<PropertyDescriptor> propertyList = new ArrayList<>();

    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
      if (beanInfo != null) {
        PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        if (descriptors != null) {
          for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getPropertyType() == Class.class) {
              continue;
            }
            propertyList.add(descriptor);
          }
        }
      }
    } catch (IntrospectionException e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    }

    return propertyList;
  }

  /**
   * 이름 기반으로 필드 객체를 찾아준다.
   *
   * @param clz 클래스 객체.
   * @param fieldName 찾고자 하는 필드 객체.
   * @return 필드 데이터.
   * @throws NoSuchFieldException 필드를 찾지 못한경우 예외를 발생.
   */
  public static Field findField(Class<?> clz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clz;
    while (current != null && current != Object.class) {
      Field[] fs = current.getDeclaredFields();
      for (Field f : fs) {
        if (f.getName().equals(fieldName)) {
          f.setAccessible(true);
          return f;
        }
      }
      current = current.getSuperclass();
    }
    String clzName = clz == null ? "null class" : clz.getName();
    throw new NoSuchFieldException(
        "Not found field. fieldName:" + fieldName + " in class:" + clzName);
  }

  /**
   * 해당 클래스가 가진 모든 필드를 리스트로 리턴
   *
   * @param t 대상 객체
   * @param <T> 타입
   * @return 필드 리스트
   */
  public static <T> List<Field> findAllFields(T t) {
    Objects.requireNonNull(t);

    Class<?> clazz = t.getClass();
    List<Field> fields = new ArrayList<>();
    while (clazz != null) {
      // 1. 상위 클래스가 null 이 아닐때까지 모든 필드를 list 에 담는다.
      fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    return fields;
  }

  /**
   * static 으로 선언되지 않은 모든 필드를 구해준다.
   *
   * @param clz 클래스 객체
   * @return static으로 선언되지 않은 모든 필드 리스트.
   */
  public static List<Field> findNonStaticFields(Class<?> clz) {
    List<Field> fields = new LinkedList<>();
    Class<?> current = clz;
    while (current != Object.class) {
      Field[] fs = current.getDeclaredFields();
      for (Field f : fs) {
        if (Modifier.isStatic(f.getModifiers())) {
          continue;
        }
        fields.add(f);
      }
      if (current.getSuperclass() == null) {
        log.debug("Super class is null for class > {}", current.getName());
        break;
      }
      current = current.getSuperclass();
    }
    return fields;
  }

  /**
   * 필드에 정의된 파라미터 타입을 찾아 준다.
   *
   * @param f Field객체.
   * @return Field객체가 parameterized type일 경우 정의된 클래스 배열, 그렇지 않은경우 EMPTY Array 가 반환된다.
   */
  public static Class<?>[] getParameterizedTypes(Field f) {
    Type type = f.getGenericType();
    return getParameterizedTypes(type);
  }

  public static Class<?>[] getParameterizedTypes(Type type) {
    if (type instanceof ParameterizedType parameterizedType) {
      Type[] pTypes = parameterizedType.getActualTypeArguments();
      Class<?>[] arr = new Class<?>[pTypes.length];
      int i = 0;
      for (Type pType : pTypes) {
        arr[i++] = (Class<?>) pType;
      }
      return arr;
    }
    return EMPTY_CLAZZ;
  }

  /**
   * object에서 path의 값을 구한다. path가 존재하지 않는다면 {@link IllegalStateException} throw한다. ex) path :
   * key1.key2[0].key3
   *
   * @param obj 대상 객체
   * @param path 경로
   * @return 값
   */
  public static Object getValueByPath(Object obj, String path) {
    Object value = obj;

    String[] paths = path.split(SPLIT_CHAR_KEY);
    Pattern pattern = Pattern.compile(PATTERN_LIST_INDEX);

    for (String p : paths) {
      value = resolvePathSegment(value, p, pattern);
    }
    return value;
  }

  private static Object resolvePathSegment(Object value, String p, Pattern pattern) {
    Matcher matcher = pattern.matcher(p);
    if (!matcher.find()) {
      if (value instanceof Map) {
        return ((Map<?, ?>) value).get(p);
      }
      throw new IllegalStateException("The target object is not Map.");
    }

    String mapKey = p.substring(0, matcher.start());
    Object current = value;
    if (StringUtils.isNotBlank(mapKey)) {
      if (current instanceof Map) {
        current = ((Map<?, ?>) current).get(mapKey);
      } else {
        throw new IllegalStateException("The target object is not Map.");
      }
    }

    matcher.reset();
    while (matcher.find()) {
      String group = matcher.group();
      int idx = Integer.parseInt(group.substring(1, group.length() - 1));
      if (current instanceof List) {
        if (((List<?>) current).size() <= idx) {
          throw new IllegalStateException("index more then list size.");
        }
        current = ((List<?>) current).get(idx);
      } else {
        throw new IllegalStateException("The target object is not List.");
      }
    }
    return current;
  }

  /**
   * Object의 Getter 메소드 실행
   *
   * @param o 대상 객체
   * @param m getter 메서드
   * @return 실행 결과
   */
  public static Object invokeGetMethod(Object o, Method m) {
    if (o == null || m == null) {
      return null;
    }
    try {
      Object obj = m.invoke(o);
      if (ObjectUtils.isEmpty(obj)) {
        return null;
      } else {
        return obj;
      }
    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
      log.error(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * 특정 이름을 가진 필드의 Getter 메소드 실행
   *
   * @param obj 객체
   * @param field 이름
   * @return 반환 값
   */
  public static Object invokeGetMethod(Object obj, String field) {
    List<PropertyDescriptor> pdList = ReflectionUtil.getPropertyDescriptors(obj.getClass());
    return invokeGetMethod(obj, field, pdList);
  }

  /**
   * 특정 이름을 가진 필드의 Getter 메소드 실행
   *
   * @param obj 객체
   * @param field 이름
   * @param pdList 속성
   * @return 반환 값
   */
  public static Object invokeGetMethod(Object obj, String field, List<PropertyDescriptor> pdList) {
    for (PropertyDescriptor pd : pdList) {
      if (pd.getName().equals(field)) {
        return invokeGetMethod(obj, pd.getReadMethod());
      }
    }
    return null;
  }

  /**
   * Object의 특정 Setter 메소드 실행
   *
   * @param obj 대상 객체
   * @param m setter 메서드
   * @param val 설정할 값
   */
  public static void invokeMethodSiently(Object obj, Method m, Object val) {
    if (obj == null || m == null) {
      return;
    }
    try {
      m.invoke(obj, val);
    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  /**
   * 해당 이름을 가진 필드의 Setter 메소드 실행
   *
   * @param obj 객체
   * @param field 이름
   * @param value 파라미터 값
   */
  public static void invokeSetMethod(Object obj, String field, Object value) {
    List<PropertyDescriptor> pdList = ReflectionUtil.getPropertyDescriptors(obj.getClass());
    invokeSetMethod(obj, field, value, pdList);
  }

  /**
   * 해당 이름을 가진 필드의 Setter 메소드 실행
   *
   * @param obj 객체
   * @param field 이름
   * @param value 파라미터
   * @param pdList 속성
   */
  public static void invokeSetMethod(
      Object obj, String field, Object value, List<PropertyDescriptor> pdList) {
    for (PropertyDescriptor pd : pdList) {
      if (pd.getName().equals(field)) {
        invokeMethodSiently(obj, pd.getWriteMethod(), value);
        return;
      }
    }
  }

  /**
   * @param src 원본 객체
   * @param target 대상 객체
   */
  public static void deepCopyObject(Object src, Object target) {
    if (src == null || target == null) {
      return;
    }

    List<PropertyDescriptor> srcPropertyDescriptors =
        ReflectionUtil.getPropertyDescriptors(src.getClass());
    List<PropertyDescriptor> targetPropertydescriptors =
        ReflectionUtil.getPropertyDescriptors(target.getClass());

    deepCopyObject(srcPropertyDescriptors, src, targetPropertydescriptors, target);
  }

  /**
   * @param srcPdList 원본 속성 목록
   * @param src 원본 객체
   * @param targetPdList 대상 속성 목록
   * @param target 대상 객체
   */
  public static void deepCopyObject(
      List<PropertyDescriptor> srcPdList,
      Object src,
      List<PropertyDescriptor> targetPdList,
      Object target) {
    if (targetPdList == null || srcPdList == null || target == null || src == null) {
      return;
    }

    Map<String, PropertyDescriptor> tPdMap = new HashMap<>();
    for (PropertyDescriptor tPd : targetPdList) {
      tPdMap.put(tPd.getName(), tPd);
    }

    for (PropertyDescriptor sPd : srcPdList) {
      if (!tPdMap.containsKey(sPd.getName()) || sPd.getReadMethod() == null) {
        continue;
      }

      PropertyDescriptor propertyDescriptor = tPdMap.get(sPd.getName());
      try {
        Object value = sPd.getReadMethod().invoke(src);
        propertyDescriptor.getWriteMethod().invoke(target, value);
      } catch (IllegalAccessException | InvocationTargetException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * 두개의 Object를 비교해서 다른 new값을 리턴한다.
   *
   * @param obj1 (old)
   * @param obj2 (new)
   * @return Map&lt;String, String&gt;
   */
  public static Map<String, Object> compareObject(Object obj1, Object obj2) {
    if (ObjectUtils.isEmpty(obj1)
        || ObjectUtils.isEmpty(obj2)
        || obj1.getClass() != obj2.getClass()) {
      return Map.of();
    }
    return MapUtil.compareMapObject(
        MapUtil.converObjectToMap(obj1), MapUtil.converObjectToMap(obj2));
  }
}
