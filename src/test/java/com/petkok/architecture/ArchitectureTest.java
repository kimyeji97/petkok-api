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
 * <p>현재는 도메인 코드가 없어 대부분의 규칙이 빈 집합을 대상으로 한다. 그래서 {@code allowEmptyShould(true)} 를 붙였다 — 없으면 ArchUnit
 * 이 "검사 대상 없음"을 실패로 처리한다. 도메인이 들어오면 자동으로 실제 검사가 된다.
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

  /** 레이어 방향 — Controller → Service → Repository 단방향. */
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
          .withOptionalLayers(true)
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
          .allowEmptyShould(true);

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
          .allowEmptyShould(true);

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
          .allowEmptyShould(true);

  /** 네이밍 — controller 패키지의 클래스는 Controller 로 끝난다. */
  @ArchTest
  static final ArchRule CONTROLLER_NAMING =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .haveSimpleNameEndingWith("Controller")
          .as("controller 패키지의 클래스는 Controller 로 끝난다")
          .allowEmptyShould(true);

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
          .allowEmptyShould(true);

  /** 위치 — {@code @Entity} 는 data/{도메인}/entity 아래에만 둔다. */
  @ArchTest
  static final ArchRule ENTITY_LOCATION =
      classes()
          .that()
          .areAnnotatedWith(Entity.class)
          .should()
          .resideInAPackage(ROOT + ".data..entity..")
          .as("@Entity 는 data/{도메인}/entity 에만 둔다")
          .allowEmptyShould(true);
}
