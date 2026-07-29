package com.petkok.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * 도메인 간 참조 금지 — {@code business}/{@code data} 3분할을 고른 핵심 이유다.
 *
 * <p>이 규칙만 별도 클래스로 분리한 이유는 {@link AnalyzeClasses} 범위를 {@code business}·{@code data} 로 좁혀야 하기 때문이다.
 * {@code framework} 가 섞이면 {@code framework.config} 같은 패키지가 "config" 슬라이스로 잡혀 규칙이 엉뚱해진다.
 *
 * <p><b>⚠️ 슬라이스 패턴에 괄호를 잘못 치면 규칙이 정반대로 동작한다.</b> {@code com.petkok.*.(*)..} 에서 첫 {@code *} 는
 * business|data 를 <em>캡처하지 않고</em> 넘겨야 도메인명만 슬라이스 키가 된다. 이를 {@code
 * com.petkok.(business|data).(*)..} 로 쓰면 트리 이름까지 키에 포함돼 {@code business/feeding} 과 {@code
 * data/feeding} 이 서로 다른 슬라이스가 되고, <b>같은 도메인 참조까지 위반으로 잡힌다.</b> 실제로 그렇게 작성했다가 프로브 테스트에서 발견했다.
 *
 * <p>따라서 {@code business/{도메인}} 과 {@code data/{도메인}} 은 <b>반드시 같은 이름</b>이어야 한다. 이름이 어긋나면 이 규칙은 에러 없이
 * 조용히 무력화된다(서로 다른 슬라이스로 잡혀 규칙은 통과하는데 경계는 안 지켜진다).
 */
@AnalyzeClasses(packages = {"com.petkok.business", "com.petkok.data"})
final class DomainBoundaryTest {

  private DomainBoundaryTest() {}

  /**
   * 예외 2건.
   *
   * <ul>
   *   <li>{@code data.common} — 베이스 엔티티 등 도메인 공용. 누구나 참조할 수 있다
   *   <li>{@code business.timeline} — 여러 도메인 Repository 를 조합하는 read 전용 모델. 유일한 cross-domain 허용처
   * </ul>
   */
  @ArchTest
  static final ArchRule NO_CROSS_DOMAIN_DEPENDENCY =
      SlicesRuleDefinition.slices()
          .matching("com.petkok.*.(*)..")
          .should()
          .notDependOnEachOther()
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.data.common.."))
          .ignoreDependency(resideInAPackage("com.petkok.business.timeline.."), alwaysTrue())
          .as("도메인 간 참조 금지 (business/{d} 와 data/{d} 는 같은 슬라이스)")
          .allowEmptyShould(true);
}
