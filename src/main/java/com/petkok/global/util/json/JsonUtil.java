package com.petkok.global.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** JsonUtil 클래스 Json 변환에 관련된 유틸리티 */
@Slf4j
public class JsonUtil {

  private JsonUtil() {}

  /**
   * Object를 Json String으로 변경한다.
   *
   * @param obj 변경할 Object
   * @return Json String
   */
  public static String parseJsonObject(Object obj) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl();
    sp.setNullValueSerializer(new NullSerializer());
    mapper.setSerializerProvider(sp);
    return mapper.writeValueAsString(obj);
  }

  /**
   * Object를 Json String으로 변경한다.
   *
   * @param obj 변경할 Object
   * @return Json String
   */
  public static String parseJsonObjectNull(Object obj) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializerProvider(new DefaultSerializerProvider.Impl());
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper.writeValueAsString(obj);
  }

  /**
   * Object를 Json String으로 변경한다.
   *
   * @param obj 변경할 Object
   * @return Json String
   */
  public static String parseJsonObjectPrettyPrinter(Object obj) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl();
    sp.setNullValueSerializer(new NullSerializer());
    mapper.setSerializerProvider(sp);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  /**
   * Json string을 List&lt;Map&gt; 객체로 변경한다.
   *
   * @param str 변경할 Json String
   * @return List&lt;Map&gt;
   */
  public static List<Map<String, Object>> parseStringAsList(String str) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(str, new TypeReference<>() {});
  }

  /**
   * Json string을 List&lt;T&gt; 객체로 변경한다.
   *
   * @param <T> 타입
   * @param str 변경할 Json String
   * @param classType 대상 타입
   * @return List&lt;T&gt;
   */
  public static <T> List<T> parseStringAsList(String str, Class<T> classType) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(
        str, mapper.getTypeFactory().constructCollectionType(List.class, classType));
  }

  /**
   * Json string을 List&lt;T&gt; 객체로 변경한다. 에러 발생시 로그만 찍고 빈 리스트 리턴
   *
   * @param str 대상 문자열
   * @param classType 대상 타입
   * @param <T> 타입
   * @return 변환 결과
   */
  public static <T> List<T> parseStringAsListNotThrow(String str, Class<T> classType) {
    if (StringUtils.isEmpty(str)) {
      return new ArrayList<>();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(
          str, mapper.getTypeFactory().constructCollectionType(List.class, classType));
    } catch (IllegalArgumentException | IOException e) {
      log.error(e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * Json string을 JsonNode 객체로 변경한다.
   *
   * @param str 변경할 Json String
   * @return JsonNode
   */
  public static Map<String, String> parseStringToMap(String str) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(str, Map.class);
  }

  /**
   * Json string을 JsonNode 객체로 변경한다.
   *
   * @param str 변경할 Json String
   * @return JsonNode
   */
  public static JsonNode parseString(String str) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(str, JsonNode.class);
  }

  /**
   * Object를 JsonNode 객체로 변경한다.
   *
   * @param obj 변경할 Object
   * @return JsonNode
   */
  public static JsonNode parseObject(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.valueToTree(obj);
  }

  public static String convertObjectToString(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Object를 선언된 Type으로 변경한다.
   *
   * @param obj 변경할 Object
   * @param classType 변경할 Type
   * @return 변환 결과
   */
  public static <T> T convertObject(Object obj, Class<T> classType) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(obj, classType);
  }

  /**
   * Object를 선언된 Type으로 변경한다.
   *
   * @param obj 변경할 Object
   * @param classType 변경할 Type
   * @return 변환 결과
   */
  public static <T> T convertObjectWithUnkown(Object obj, Class<T> classType) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.convertValue(obj, classType);
  }

  /**
   * Object를 선언된 Type으로 변경한다.
   *
   * @param obj 변경할 Object
   * @param typeRef 변경할 Type
   * @return 변환 결과
   */
  public static <T> T convertObject(Object obj, TypeReference<T> typeRef) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(obj, typeRef);
  }

  /**
   * Object를 선언된 Type으로 변경한다.
   *
   * @param obj 변경할 Object
   * @param typeRef 변경할 Type
   * @return 변환 결과
   */
  public static <T> T convertObjectWithUnkown(Object obj, TypeReference<T> typeRef) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.convertValue(obj, typeRef);
  }

  /**
   * Object를 MultiValueMap&lt;String, String&gt;으로 변경한다.
   *
   * @param obj 변경할 Object
   * @return MultiValueMap&lt;String, String&gt;
   */
  public static MultiValueMap<String, String> convertObjectMultiValueMap(Object obj) {
    MultiValueMap<String, String> multiValueMaps = new LinkedMultiValueMap<>();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> maps = mapper.convertValue(obj, new TypeReference<>() {});
    multiValueMaps.setAll(maps);
    return multiValueMaps;
  }

  /**
   * JsonString을 선언된 Type으로 변경한다.
   *
   * @param str 변경할 JsonString
   * @param classType 변경할 Type
   * @return 변환 결과
   */
  public static <T> T parseString(String str, Class<T> classType) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(str, classType);
  }

  /**
   * Json string을 선언된 Type으로 변경한다. 에러 발생시 로그만 찍고 빈 객체를 리턴한다.
   *
   * @param str 대상 문자열
   * @param classType 대상 타입
   * @param <T> 타입
   * @return 변환 결과
   */
  public static <T> T parseStringNotThrow(String str, Class<T> classType) {
    T emptyObj;
    try {
      emptyObj = classType.getConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      log.error(e.getMessage(), e);
      throw new IllegalStateException("알수 없는 오류가 발생했습니다.");
    }

    if (StringUtils.isEmpty(str)) {
      return emptyObj;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(str, classType);
    } catch (IllegalArgumentException | IOException e) {
      log.error(e.getMessage());
      return emptyObj;
    }
  }

  /**
   * JsonString을 Java Domain Class 로 변경한다.
   *
   * @param jsonStr 변경할 JsonString
   * @param className Java Domain Class 명
   * @return Java Domain Class String
   */
  public static String parseToJavaObject(String jsonStr, String className) throws IOException {
    return parseToJavaObject(jsonStr, className, StringUtils.EMPTY);
  }

  /**
   * JsonString을 Java Domain Class 로 변경한다.
   *
   * @param jsonStr 변경할 JsonString
   * @param className Java Domain Class 명
   * @param packageName Java Domain Package 명
   * @return Java Domain Class String
   */
  public static String parseToJavaObject(String jsonStr, String className, String packageName)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readValue(jsonStr, JsonNode.class);

    return parseToJavaObject(rootNode, className, packageName);
  }

  /**
   * Map을 Java Domain Class 로 변경한다.
   *
   * @param json 변경할 Map
   * @param className Java Domain Class 명
   * @return Java Domain Class String
   */
  public static String parseToJavaObject(Map json, String className)
      throws JsonParseException, JsonMappingException, IOException {
    return parseToJavaObject(parseJsonObject(json), className, StringUtils.EMPTY);
  }

  /**
   * Map을 Java Domain Class 로 변경한다.
   *
   * @param json 변경할 JsonString
   * @param className Java Domain Class 명
   * @param packageName Java Domain Package 명
   * @return Java Domain Class String
   */
  public static String parseToJavaObject(Map json, String className, String packageName)
      throws IOException {
    return parseToJavaObject(parseJsonObject(json), className, packageName);
  }

  /**
   * JsonNode를 Java Domain Class 로 변경한다.
   *
   * @param rootNode 변경할 JsonNode
   * @param className Java Domain Class 명
   * @param packageName Java Domain Package 명
   * @return Java Domain Class String
   */
  public static String parseToJavaObject(JsonNode rootNode, String className, String packageName)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    StringBuilder subSb = new StringBuilder();

    if (packageName != null && !packageName.equals(StringUtils.EMPTY)) {
      sb.append("package ").append(packageName).append(";\n");
      sb.append("\n");
    }

    sb.append("@Data\n");
    sb.append("public class ");
    sb.append(className);
    sb.append(" {\n");

    Iterator<String> iter = rootNode.fieldNames();

    AtomicInteger indent = new AtomicInteger(1);
    while (iter.hasNext()) {
      String key = iter.next();
      appendRootField(rootNode, key, sb, subSb, indent);
    }

    if (subSb.length() > 0) {
      sb.append(subSb);
    }

    sb.append("}");

    return sb.toString();
  }

  private static void appendRootField(
      JsonNode rootNode, String key, StringBuilder sb, StringBuilder subSb, AtomicInteger indent) {
    JsonNode fieldNode = rootNode.get(key);
    if (fieldNode.isBoolean()) {
      sb.append("\tBoolean ").append(key).append(";\n");
    } else if (fieldNode.isDouble()) {
      sb.append("\tDouble ").append(key).append(";\n");
    } else if (fieldNode.isInt()) {
      sb.append("\tInteger ").append(key).append(";\n");
    } else if (fieldNode.isTextual()) {
      sb.append("\tString ").append(key).append(";\n");
    } else if (fieldNode.isLong()) {
      sb.append("\tLong ").append(key).append(";\n");
    } else if (fieldNode.isObject()) {
      String innerClassName = key.substring(0, 1).toUpperCase() + key.substring(1);
      sb.append("\t").append(innerClassName).append(" ").append(key).append(";\n");
      subSb.append("\n");
      subSb.append("\t@Data\n");
      subSb.append("\tpublic static class ").append(innerClassName).append(" {\n");

      objectTypeMapping(fieldNode, subSb, indent);
      subSb.append("\t}\n");
    } else if (fieldNode.isArray()) {
      String innerClassName = key.substring(0, 1).toUpperCase() + key.substring(1);
      sb.append("\t").append(innerClassName).append(" ").append(key).append(";\n");
      subSb.append("\n");
      subSb.append("\t@Data\n");
      subSb.append("\tpublic static class ").append(innerClassName).append(" {\n");

      objectTypeMapping(fieldNode.get(0), subSb, indent);
      subSb.append("\t}\n");
    }
  }

  private static void objectTypeMapping(JsonNode cellNode, StringBuilder sb, AtomicInteger indent) {
    Iterator<String> iter = cellNode.fieldNames();
    StringBuilder subSb = new StringBuilder();

    AtomicInteger cIndent = new AtomicInteger(indent.get());
    while (iter.hasNext()) {
      String key = iter.next();

      addIntent(sb, cIndent.get());

      JsonNode fieldNode = cellNode.get(key);
      if (fieldNode.isBoolean()) {
        sb.append("Boolean ").append(key).append(";\n");
      } else if (fieldNode.isDouble()) {
        sb.append("Double ").append(key).append(";\n");
      } else if (fieldNode.isInt()) {
        sb.append("Integer ").append(key).append(";\n");
      } else if (fieldNode.isTextual()) {
        sb.append("String ").append(key).append(";\n");
      } else if (fieldNode.isLong()) {
        sb.append("Long ").append(key).append(";\n");
      } else if (fieldNode.isObject()) {
        appendNestedObject(fieldNode, key, sb, subSb, cIndent);
      } else if (fieldNode.isArray()) {
        appendNestedArray(fieldNode, key, sb, subSb, cIndent);
      }
    }

    if (subSb.length() > 0) {
      sb.append(subSb);
    }

    cIndent.decrementAndGet();
  }

  private static void appendNestedObject(
      JsonNode fieldNode,
      String key,
      StringBuilder sb,
      StringBuilder subSb,
      AtomicInteger cIndent) {
    String className = key.substring(0, 1).toUpperCase() + key.substring(1);

    sb.append(className).append(" ").append(key).append(";\n");
    subSb.append("\n");

    addIntent(subSb, cIndent.get());
    subSb.append("@Data\n");
    addIntent(subSb, cIndent.get());
    subSb.append("public static class ").append(className).append(" {\n");

    objectTypeMapping(fieldNode, subSb, new AtomicInteger(cIndent.incrementAndGet()));

    addIntent(subSb, cIndent.get() - 1);

    subSb.append("}\n");

    cIndent.decrementAndGet();
  }

  private static void appendNestedArray(
      JsonNode fieldNode,
      String key,
      StringBuilder sb,
      StringBuilder subSb,
      AtomicInteger cIndent) {
    String className = key.substring(0, 1).toUpperCase() + key.substring(1);

    if (fieldNode.size() > 0 && fieldNode.get(0).isObject()) {
      addIntent(subSb, cIndent.get());
      sb.append("List<").append(className).append("> ").append(key).append(";\n");

      subSb.append("\n");
      addIntent(subSb, cIndent.get());
      subSb.append("@Data\n");
      addIntent(subSb, cIndent.get());
      subSb.append("public static class ").append(className).append(" {\n");

      objectTypeMapping(fieldNode.get(0), subSb, new AtomicInteger(cIndent.incrementAndGet()));
      addIntent(subSb, cIndent.get() - 1);
      subSb.append("}\n");
    } else {
      appendPrimitiveListType(fieldNode.get(0), key, sb);
    }

    cIndent.decrementAndGet();
  }

  private static void appendPrimitiveListType(
      JsonNode arrayChildNode, String key, StringBuilder sb) {
    if (arrayChildNode == null) {
      return;
    }

    if (arrayChildNode.isBoolean()) {
      sb.append("List<Boolean> ").append(key).append(";\n");
    } else if (arrayChildNode.isDouble()) {
      sb.append("List<Double> ").append(key).append(";\n");
    } else if (arrayChildNode.isInt()) {
      sb.append("List<Integer> ").append(key).append(";\n");
    } else if (arrayChildNode.isTextual()) {
      sb.append("List<String> ").append(key).append(";\n");
    } else if (arrayChildNode.isLong()) {
      sb.append("List<Long> ").append(key).append(";\n");
    }
  }

  private static void addIntent(StringBuilder sb, Integer indent) {
    for (int i = 0; i <= indent; i++) {
      sb.append("\t");
    }
  }

  /** NullSerializer 클래스 Json 변환 시 null일경우 빈 문자열로 치환 */
  private static class NullSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeString(StringUtils.EMPTY);
    }
  }
}
