package com.petkok.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import jakarta.persistence.Entity;

/**
 * 구조 규칙을 테스트로 고정한다. 산문 규칙(AGENTS.md §3·§5)은 시간이 지나면 깨지므로 여기서 강제한다.
 *
 * <p><b>{@code allowEmptyShould} 는 전부 껐다 (2026-08-03, REQ-08 Phase 0).</b> 도입 당시(2026-07-29)에는 도메인
 * 코드가 없어 규칙 대부분이 빈 집합을 대상으로 했고, 켜 두지 않으면 ArchUnit 이 "검사 대상 없음"을 실패로 처리했다. REQ-07 로
 * controller·service·repository·entity·dto 가 모두 들어와 <b>이제 8개 규칙 전부 실제 대상을 갖는다</b> — 끈 상태로도 통과하는 것을
 * 확인하고 되돌렸다.
 *
 * <p>⚠️ <b>다시 {@code true} 로 되돌리지 말 것.</b> 새 규칙을 넣었는데 대상이 0개라 실패하면 완화 신호가 아니라 <b>규칙이 시기상조라는
 * 신호</b>다. 다만 이것이 막는 것은 "{@code that()} 이 0개를 매칭하는" 경우 하나뿐이다 — 패턴은 매칭되는데 슬라이스 키가 틀린 종류(2026-07-29
 * 괄호 사고, {@link DomainBoundaryTest} 참고)는 여전히 잡지 못한다. 규칙을 고쳤으면 <b>일부러 위반을 심어</b> 빨간불이 되는지 확인해야 한다
 * (CLAUDE.md).
 *
 * <p>필드명은 Checkstyle {@code ConstantName} 을 따르고, 사람이 읽을 설명은 {@code .as(...)} 가 담당한다 — 위반 메시지에 그대로
 * 출력된다.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT)
final class ArchitectureTest {

  static final String ROOT = "com.petkok";

  private static final String BUSINESS = ROOT + ".business..";
  private static final String DATA = ROOT + ".data..";
  private static final String FRAMEWORK = ROOT + ".framework..";

  private ArchitectureTest() {}

  /**
   * 레이어 방향 — Controller → Service → Repository 단방향.
   *
   * <p><b>이 규칙은 "어느 레이어에도 속하지 않는 클래스"의 접근도 잡는다.</b> {@code framework.processor.filter} 는 정의된 세 레이어
   * 어디에도 없으므로, 필터가 {@code data..repository..} 를 직접 참조하면 {@code Repository} 레이어의 {@code
   * mayOnlyBeAccessedByLayers("Service")} 에 걸린다. 2026-08-03 프로브에서 실측했다 — {@code
   * JwtAuthenticationFilter} 에 {@code UserRepository} 를 심자 {@link #FRAMEWORK_MUST_NOT_KNOW_DOMAIN}
   * 과 <b>이 규칙이 함께</b> 빨간불이 됐다.
   *
   * <p>즉 REQ-08 Phase 3 의 {@code UserStatusChecker} 포트는 규칙 #4 하나가 아니라 <b>두 규칙을 동시에 만족시키기 위한
   * 것</b>이다. 규칙 #4 만 열어서는 직참조가 여전히 통과하지 못한다.
   */
  @ArchTest
  static final ArchRule LAYER_DIRECTION =
      Architectures.layeredArchitecture()
          .consideringAllDependencies()
          .layer("Controller")
          .definedBy(ROOT + ".business..controller..")
          .layer("Service")
          .definedBy(ROOT + ".business..service..")
          .layer("Repository")
          .definedBy(ROOT + ".data..repository..")
          .whereLayer("Controller")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Service")
          .mayOnlyBeAccessedByLayers("Controller")
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Service")
          .withOptionalLayers(false)
          .as("레이어 방향 — Controller → Service → Repository 단방향");

  /** Entity 누출 금지 — AGENTS.md §5 "Entity 는 Service 밖으로 나가지 않는다" 의 강제. */
  @ArchTest
  static final ArchRule NO_ENTITY_IN_CONTROLLER =
      noClasses()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".data..entity..")
          .as("Controller 는 Entity 를 참조하지 않는다 (응답은 DTO)")
          .allowEmptyShould(false);

  /** 트리 단방향 — data 는 business 를 참조하지 않는다. */
  @ArchTest
  static final ArchRule DATA_MUST_NOT_KNOW_BUSINESS =
      noClasses()
          .that()
          .resideInAPackage(DATA)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BUSINESS)
          .as("data → business 참조 금지")
          .allowEmptyShould(false);

  /** 트리 단방향 — framework 는 도메인을 모른다. */
  @ArchTest
  static final ArchRule FRAMEWORK_MUST_NOT_KNOW_DOMAIN =
      noClasses()
          .that()
          .resideInAPackage(FRAMEWORK)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(BUSINESS, DATA)
          .as("framework → business·data 참조 금지")
          .allowEmptyShould(false);

  /** 네이밍 — controller 패키지의 클래스는 Controller 로 끝난다. */
  @ArchTest
  static final ArchRule CONTROLLER_NAMING =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .haveSimpleNameEndingWith("Controller")
          .as("controller 패키지의 클래스는 Controller 로 끝난다")
          .allowEmptyShould(false);

  /** 네이밍 — dto 패키지의 클래스는 Request 또는 Response 로 끝난다. */
  @ArchTest
  static final ArchRule DTO_NAMING =
      classes()
          .that()
          .resideInAPackage("..dto..")
          .should()
          .haveSimpleNameEndingWith("Request")
          .orShould()
          .haveSimpleNameEndingWith("Response")
          .as("dto 패키지의 클래스는 Request 또는 Response 로 끝난다")
          .allowEmptyShould(false);

  /** 위치 — {@code @Entity} 는 data/{도메인}/entity 아래에만 둔다. */
  @ArchTest
  static final ArchRule ENTITY_LOCATION =
      classes()
          .that()
          .areAnnotatedWith(Entity.class)
          .should()
          .resideInAPackage(ROOT + ".data..entity..")
          .as("@Entity 는 data/{도메인}/entity 에만 둔다")
          .allowEmptyShould(false);
}
