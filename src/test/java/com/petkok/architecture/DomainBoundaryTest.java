package com.petkok.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

import com.tngtech.archunit.core.importer.ImportOption;
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
@AnalyzeClasses(
    packages = {"com.petkok.business", "com.petkok.data"},
    importOptions = ImportOption.DoNotIncludeTests.class)
final class DomainBoundaryTest {

  private DomainBoundaryTest() {}

  /**
   * 예외 4건 + 소유권 앵커 3건. <b>전부 "설계상 옳다"로 확정된 것이며 임시 완화가 아니다</b> (2026-07-31 정리, REQ-08 Phase 0 ·
   * 2026-08-28 추가, REQ-10 Phase 0).
   *
   * <ul>
   *   <li>{@code data.common} — 베이스 엔티티 등 도메인 공용. 누구나 참조할 수 있다
   *   <li>{@code business.timeline} — 여러 도메인 Repository 를 조합하는 read 전용 모델. 유일한 cross-domain 허용처. 다만
   *       <b>대상 코드가 아직 0개라 REQ-12 까지는 공허하다</b> — 알고 두는 것과 모르고 두는 것은 다르므로 남긴다
   *   <li>{@code framework} — <b>도메인이 아니다.</b> 아래 참고
   *   <li>{@code business.auth → data.user} — 소셜 자동가입. 아래 참고
   *   <li>{@code * → business.pet.service · data.pet.dto · data.pet.enums} — {@code PetAccessGuard}
   *       소비 (PLAN-REQ-09 D3). 하위 도메인이 가드·{@code OwnedPetResponse}·{@code Species} 만 보게 <b>좁게</b>
   *       연다. 규칙 정의부 주석 참고
   * </ul>
   *
   * <p><b>REQ-08 은 이 목록을 늘리지 않았다.</b> 탈퇴 시 refresh revoke 를 생략한 것(D5)도, 필터의 활성 검사를 직참조가 아닌 {@code
   * UserStatusChecker} 포트로 만든 것(D2)도 예외를 늘리지 않기 위한 선택이다. 여기에 5번째 줄이 생기면 그 선택들의 근거가 무너진 것이므로, 추가 전에
   * PLAN-REQ-08 「결정」을 다시 볼 것.
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
          // ⬇️ 설계 결정 (2026-07-31 승격, PLAN-REQ-08 D4). 원래 "임시" 딱지가 붙어 있었다.
          //    소셜 자동가입은 본질적으로 user 프로비저닝이므로 auth 가 users 행을 만드는 것은
          //    우회가 아니라 제 일이다. 이 참조를 없애려면 business/user 에 프로비저닝 진입점을
          //    두어야 하는데, 그러면 business/auth → business/user 예외가 대신 생겨
          //    개수는 그대로인 채 간접층만 는다. 그래서 없애지 않는다.
          .ignoreDependency(
              resideInAPackage("com.petkok.business.auth.."),
              resideInAPackage("com.petkok.data.user.."))
          // ⬇️ 설계 결정 (2026-08-28 추가, PLAN-REQ-09 D3 · PLAN-REQ-10 Phase 0). 소유권 앵커.
          //    /pets/{petId}/... 하위 도메인(weight·activity·feeding·shed·diary·gallery)은 진입 시
          //    PetAccessGuard 를 통과해야 하므로 business.pet.service 와, 가드가 돌려주는
          //    OwnedPetResponse(data.pet.dto)·Species(data.pet.enums) 를 누구나 참조할 수 있다.
          //    ⚠️ 일부러 좁게 열었다 — data.pet 전체가 아니라 세 패키지만이다. entity·repository 는
          //    여전히 닫혀 있어 PetRepository 직접 주입(우회)과 Pet 엔티티 누출이 둘 다 위반으로 잡힌다
          //    (2026-08-10 · 2026-08-28 프로브 실측, REQ-10-01~03). 넓히면 가드를 두는 이유가 사라진다.
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.business.pet.service.."))
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.data.pet.dto.."))
          .ignoreDependency(alwaysTrue(), resideInAPackage("com.petkok.data.pet.enums.."))
          .as("도메인 간 참조 금지 (business/{d} 와 data/{d} 는 같은 슬라이스)")
          .allowEmptyShould(false);
}
