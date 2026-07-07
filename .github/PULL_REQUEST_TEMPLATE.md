## 관련 컨텍스트

<!-- 관련 이슈 / ADR(docs/adr) / 스펙 문서가 있으면 링크한다. 없으면 배경을 아래에 서술. -->
-

---

## 변경 요약

<!-- 무엇을 변경했는가? -->

## 변경 이유

<!-- 왜 변경했는가? -->

---

## 테스트 방법 및 결과

<!-- 어떻게 검증했는가? 실행 명령·결과·스크린샷 등을 기재한다. -->

---

## 체크리스트

- [ ] `./gradlew spotlessCheck` 통과 (포맷)
- [ ] `./gradlew checkstyleMain checkstyleTest -PciStrict` 통과 (네이밍·복잡도·미사용 import)
- [ ] `./gradlew test` 통과
- [ ] self-review를 완료했다 (코드를 다시 읽고 의도치 않은 변경이 없는지 확인)
