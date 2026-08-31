package com.petkok.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

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
 * <p><b>분석 대상은 프로덕션 코드뿐이다</b>({@code DoNotIncludeTests}, 2026-08-04). 이 규칙들은 전부 프로덕션 구조에 대한 것인데, 기본
 * 설정은 테스트 클래스까지 끌어와 <b>미러 패키지에 테스트를 두는 순간 이름 규칙이 오발한다.</b> 실제로 {@code
 * com.petkok.data.user.dto.UserUpdateRequestTest} 가 {@link #DTO_NAMING} 에 걸렸다 — {@code ..dto..} ·
 * {@code ..controller..} 에 테스트가 처음 들어온 시점이라 그전까지는 드러나지 않았다. 완화가 아니라 <b>범위 정정</b>이며, 프로덕션 커버리지는
 * 그대로다.
 *
 * <p>필드명은 Checkstyle {@code ConstantName} 을 따르고, 사람이 읽을 설명은 {@code .as(...)} 가 담당한다 — 위반 메시지에 그대로
 * 출력된다.
 */
@AnalyzeClasses(
    packages = ArchitectureTest.ROOT,
    importOptions = ImportOption.DoNotIncludeTests.class)
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

  /**
   * 시각 획득 경로 통일 — 무인자 {@code now()} 직접 호출 금지. 검증 계약 REQ-16-10 (PLAN-REQ-16 § 검증 계약, D5).
   *
   * <p>{@code now()} 는 <b>JVM 기본 타임존에 암묵 의존</b>한다. 배포 환경에 {@code TZ} 가 없으면 값이 9시간 어긋난 채 <b>에러
   * 없이</b> 저장된다 — 이 프로젝트가 반복해 밟은 "조용한 실패"와 같은 얼굴이다({@code .env} 빈 값 · {@code db.schema} 한쪽만 배선).
   * {@code Clock} 을 주입하면 테스트에서 시각을 고정할 수도 있다.
   *
   * <p>⚠️ <b>다섯 타입을 전부 거는 이유.</b> 계획서 문구는 {@code LocalDateTime.now()} 하나였는데, Phase 1 이 대상을 전부
   * {@code OffsetDateTime} 으로 바꿔 <b>그 문구대로 쓰면 이 규칙이 공허해진다</b> — {@code AuthService} 두 곳이 규칙 밖으로 빠져
   * {@code Clock} 주입을 안 해도 초록불이 된다(2026-08-31 실측). {@code LocalDate} · {@code Instant} · {@code
   * ZonedDateTime} 은 아직 호출부가 없지만 D4(달력 판정 = KST)가 REQ-10 에서 {@code LocalDate} 를 쓰게 되므로 미리 막는다.
   *
   * <p><b>{@code now(Clock)} 오버로드는 걸리지 않는다</b> — 파라미터 없는 시그니처만 지정했기 때문이고, 그게 이 규칙이 열어 두려는 정확한 통로다.
   *
   * <p><b>{@code data} 는 범위 밖이다</b>(2026-08-31 결정). {@code BaseSoftDeleteEntity.softDelete()} 가
   * {@code OffsetDateTime.now()} 를 부르지만 JPA 엔티티라 빈을 주입할 수 없고, {@code deleted_at} 은 벽시계 파생이 아니라
   * <b>순간</b>이라 D4 의 자리가 아니다 — Phase 1 이 {@code OffsetDateTime} 으로 바꾼 시점에 TZ 위험은 이미 사라졌다. 남는 이득은
   * 테스트 고정 하나뿐이라 엔티티 시그니처를 바꿀 값을 못 한다.
   */
  @ArchTest
  static final ArchRule REQ_16_10_NO_DIRECT_NOW =
      noClasses()
          .that()
          .resideInAnyPackage(BUSINESS, FRAMEWORK)
          .should()
          .callMethod(LocalDateTime.class, "now")
          .orShould()
          .callMethod(OffsetDateTime.class, "now")
          .orShould()
          .callMethod(LocalDate.class, "now")
          .orShould()
          .callMethod(Instant.class, "now")
          .orShould()
          .callMethod(ZonedDateTime.class, "now")
          .as("[REQ-16-10] business·framework 는 무인자 now() 를 직접 부르지 않는다 (Clock 주입)")
          .allowEmptyShould(false);
}
