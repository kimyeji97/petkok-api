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
   * 예외 3건.
   *
   * <ul>
   *   <li>{@code data.common} — 베이스 엔티티 등 도메인 공용. 누구나 참조할 수 있다
   *   <li>{@code business.timeline} — 여러 도메인 Repository 를 조합하는 read 전용 모델. 유일한 cross-domain 허용처
   *   <li>{@code framework} — <b>도메인이 아니다.</b> 아래 참고
   * </ul>
   *
   * <p><b>⚠️ framework 예외는 "허용 범위를 넓힌 것"이 아니라 규칙의 오발을 막는 것이다.</b> 슬라이스 패턴 {@code
   * com.petkok.*.(*)..} 는 <em>의존 대상</em>에도 그대로 적용되므로 {@code com.petkok.framework.config} 가 "config"
   * 슬라이스로 잡힌다. 그러면 {@code business/auth → framework/config} 가 "도메인 auth 가 도메인 config 를 참조" 로 읽혀 위반이
   * 된다. {@code @AnalyzeClasses} 범위를 좁히는 것으로는 막을 수 없다 — 분석 대상이 아니라 <em>의존 방향의 끝</em>이 문제이기 때문이다.
   *
   * <p>AGENTS.md §5 는 {@code business}·{@code data} → {@code framework} 를 <b>허용</b>한다(금지되는 것은 그 반대
   * 방향이고, 그쪽은 {@code ArchitectureTest} 의 트리 단방향 규칙이 따로 잡는다). 이 예외가 없으면 {@code ApiResponse} 를 쓰는
   * 컨트롤러가 전부 위반으로 잡혀 규칙을 아예 못 쓴다.
   *
   * <p>이 결함은 2026-07-29 Phase 4 에서 {@code business} 첫 클래스가 들어오자마자 드러났다. 그전까지는 <b>검사 대상이 없어 통과</b>하고
   * 있었다.
   */
  @ArchTest
  static final ArchRule NO_CROSS_DOMAIN_DEPENDENCY =
      SlicesRuleDefinition.slices()
          .matching("com.petkok.*.(*)..")
          .should()
          .notDependOnEachOther()
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.data.common.."))
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.framework.."))
          .ignoreDependency(resideInAPackage("com.petkok.business.timeline.."), alwaysTrue())
          // ⬇️ 임시 — 개선 방향 논의 대상 (2026-07-29 결정, PLAN-REQ-07 「미결 질문」 참고).
          //    auth 자동가입이 users 행을 만들기 때문에 생긴 참조다. 위 셋과 달리 "설계상 옳다"고
          //    확정된 예외가 아니다. 좁히거나 없애는 방향을 따로 논의한다.
          .ignoreDependency(
              resideInAPackage("com.petkok.business.auth.."),
              resideInAPackage("com.petkok.data.user.."))
          .as("도메인 간 참조 금지 (business/{d} 와 data/{d} 는 같은 슬라이스)")
          .allowEmptyShould(true);
}
