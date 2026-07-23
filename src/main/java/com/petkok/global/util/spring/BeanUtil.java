package com.petkok.global.util.spring;

import org.springframework.context.ApplicationContext;

/** {@link ApplicationContext}의 Bean관련 유틸리티 */
public class BeanUtil {

  private BeanUtil() {}

  public static <T> T getBean(Class<T> classType) {
    return ApplicationContextProvider.getApplicationContext().getBean(classType);
  }

  public static Object getBean(String name) {
    return ApplicationContextProvider.getApplicationContext().getBean(name);
  }
}
