package com.petkok.data.pet.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Pet} 의 구조 계약. 검증 계약 REQ-09-18 (PLAN-REQ-09 § 검증 계약).
 *
 * <p><b>"쓰지 않기로 한 결정"은 코드에 흔적이 없다.</b> D6 은 {@code @SQLDelete}·{@code @SQLRestriction} 을 쓰지 않기로
 * 했는데, 그 결정은 애노테이션의 <b>부재</b>로만 나타나므로 테스트가 없으면 지켜지는지 알 수 없다.
 *
 * <p>⚠️ <b>깨지는 방향이 중요하다.</b> 누군가 "소프트 딜리트 필터를 자동화하자"며 애노테이션을 붙이면 이 테스트가 빨간불이 된다 — 그게 의도다. 자동 필터는
 * 조용하고 네이티브 쿼리·조인에서 안 걸려 <b>삭제된 행이 에러 없이 샌다</b>(Notion §6 자신이 경고하는 함정).
 */
class PetTest {

  @Test
  @DisplayName("[REQ-09-18] Pet 에 @SQLRestriction 이 붙어 있지 않다")
  void req_09_18_petHasNoSqlRestriction() {
    assertThat(Pet.class.isAnnotationPresent(SQLRestriction.class)).isFalse();
  }

  @Test
  @DisplayName("[REQ-09-18] Pet 에 @SQLDelete 가 붙어 있지 않다")
  void req_09_18_petHasNoSqlDelete() {
    assertThat(Pet.class.isAnnotationPresent(SQLDelete.class)).isFalse();
  }

  @Test
  @DisplayName("[REQ-09-17] updateProfile 은 species 를 받지 않는다")
  void req_09_17_updateProfileDoesNotAcceptSpecies() {
    boolean acceptsSpecies =
        Arrays.stream(Pet.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("updateProfile"))
            .flatMap(m -> Arrays.stream(m.getParameterTypes()))
            .anyMatch(t -> t.getSimpleName().equals("Species"));

    assertThat(acceptsSpecies).isFalse();
  }
}
