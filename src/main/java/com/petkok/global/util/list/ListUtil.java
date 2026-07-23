package com.petkok.global.util.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;

/** List 타입관련 유틸리티 */
public class ListUtil {

  private ListUtil() {}

  @FunctionalInterface
  public interface BaseProcessor<T> {
    void process(List<T> list);
  }

  /**
   * list를 size수로 분할 하여 반환하는 함수
   *
   * @param <T> 타입
   * @param list 대상 리스트
   * @param size 분할 크기
   * @return 분할된 리스트
   */
  public static <T> Collection<List<T>> partitionBasedOnSize(List<T> list, int size) {
    final AtomicInteger counter = new AtomicInteger(0);
    return list.stream()
        .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / size))
        .values();
  }

  /**
   * list를 size로 분할해 BaseProcessor 실행
   *
   * @param <T> 타입
   * @param list 대상 리스트
   * @param size 분할 크기
   * @param baseProcessor 처리기
   * @return 성공 여부
   */
  public static <T> boolean partitionBasedOnSize(
      List<T> list, int size, BaseProcessor<T> baseProcessor) {
    if (ObjectUtils.isEmpty(list)) {
      return true;
    }

    try {
      Collection<List<T>> listCollection =
          ListUtil.partitionBasedOnSize(list, (size < 1 ? 100 : size));
      for (List<T> listPartition : listCollection) {
        baseProcessor.process(listPartition);
      }
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * 병합한 리스트 반환
   *
   * @param toList 대상 리스트
   * @param fromList 병합할 리스트
   * @param <T> 타입
   * @return 병합 결과
   */
  public static <T> List<T> makeMergedList(List<T> toList, List<T> fromList) {
    List<T> result = new ArrayList<>();

    if (ObjectUtils.isNotEmpty(toList)) {
      result = toList;
    }

    if (ObjectUtils.isNotEmpty(fromList)) {
      result.addAll(fromList);
    }

    return result;
  }

  /**
   * formList를 toList에 머지.
   *
   * @param toList 대상 리스트
   * @param formList 병합할 리스트
   * @param <T> 타입
   */
  public static <T> void mergeAddAll(List<T> toList, List<T> formList) {
    if (toList == null || ObjectUtils.isEmpty(formList)) {
      return;
    }

    toList.addAll(formList);
  }

  /**
   * List에서 해당 index에 존재하는 값을 Object로 리턴
   *
   * @param list 리스트
   * @param index 반환 인덱스
   * @param <T> 리스트 요소 타입
   * @return 반환 인덱스의 값
   */
  public static <T> Object findListValueByIndex(List<T> list, Integer index) {
    if (ObjectUtils.isEmpty(list)) {
      throw new IllegalStateException("The find target list is empty.");
    }

    if (ObjectUtils.isEmpty(index)) {
      throw new IllegalStateException("The index is empty.");
    }

    if (list.size() <= index) {
      throw new IllegalStateException("index more then list size.");
    }
    return list.get(index);
  }
}
