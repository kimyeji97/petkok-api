package com.petkok.global.util.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.google.common.base.CaseFormat;
import com.petkok.global.util.ReflectionUtil;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
public class MapUtil {

  private MapUtil() {}

  public static <T> T convertMapToObject(Map<String, String> map, Class<T> clazz) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializerProvider(new DefaultSerializerProvider.Impl());
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return objectMapper.convertValue(map, clazz);
  }

  /**
   * Object를 Map으로 변환한다.
   *
   * @param obj 변환 대상 객체
   * @return Map&lt;String, String&gt;
   */
  public static Map<String, Object> converObjectToMap(Object obj) {
    return converObjectToMap(obj, ReflectionUtil.getPropertyDescriptors(obj.getClass()), false);
  }

  public static Map<String, Object> converObjectToSnakeCaseMap(Object obj) {
    return converObjectToMap(obj, ReflectionUtil.getPropertyDescriptors(obj.getClass()), true);
  }

  public static Map<String, Object> converObjectToMap(
      Object obj, List<PropertyDescriptor> propertyDescriptors, boolean isConvertSnake) {
    if (obj == null) {
      return new HashMap<>();
    }
    Map<String, Object> resultMap = new HashMap<>();
    try {
      if (propertyDescriptors == null) {
        return resultMap;
      }

      for (PropertyDescriptor pd : propertyDescriptors) {
        Object value = ReflectionUtil.invokeGetMethod(obj, pd.getReadMethod());
        String name =
            isConvertSnake
                ? CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pd.getName())
                : pd.getName();
        resultMap.put(name, value);
      }
    } catch (IllegalArgumentException e) {
      log.trace(e.getMessage(), e);
    }
    return resultMap;
  }

  /**
   * 두개의 Map을 비교해서 다른 new값을 리턴한다.
   *
   * @param map1 (old)
   * @param map2 (new)
   * @return Map&lt;String, Object&gt;
   */
  public static Map<String, Object> compareMapObject(
      Map<String, Object> map1, Map<String, Object> map2) {
    Map<String, Object> result = new HashMap<>();
    // map2를 map1과 비교해서 다른 (map2)값을 리턴한다.
    if (ObjectUtils.isEmpty(map1) || ObjectUtils.isEmpty(map2)) {
      return Map.of();
    }

    for (Map.Entry<String, Object> entry1 : map1.entrySet()) {
      String key1 = entry1.getKey();
      if (!String.valueOf(entry1.getValue()).equals(String.valueOf(map2.get(key1)))) {
        result.put(key1, map2.get(key1));
      }
    }

    return result;
  }
}
