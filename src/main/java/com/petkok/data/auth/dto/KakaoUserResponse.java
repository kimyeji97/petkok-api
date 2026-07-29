package com.petkok.data.auth.dto;

/**
 * 카카오 사용자 정보 응답 ({@code GET kapi.kakao.com/v2/user/me}).
 *
 * <p>2026-07-29 실측으로 확인된 것 —
 *
 * <ul>
 *   <li><b>{@code email} 은 항상 {@code null} 이다.</b> 비즈니스 앱 전환 + 검수가 필요한 동의항목이라 현재 앱에서는 받을 수 없다
 *   <li>{@code profileImageUrl} 이 {@code http://} 스킴으로 온다. 그대로 내려주면 iOS ATS·Android cleartext 정책에
 *       막힌다
 *   <li>{@code id} 는 Long 이다. petkok 은 {@code provider_user_id varchar(255)} 에 문자열로 담는다
 * </ul>
 *
 * <p>⚠️ <b>이 호스트({@code kapi})에만 콘솔 「허용 IP 주소」가 걸린다.</b> 토큰 교환({@code kauth})은 성공하는데 여기서만 {@code
 * {"code":-401,"msg":"ip mismatched!"}} 로 거부되면 키가 아니라 IP 문제다 — 토큰이 발급됐다면 키 3개는 정상이다.
 */
public record KakaoUserResponse(Long id, KakaoAccountResponse kakaoAccount) {

  /**
   * {@code kakao_account}. 동의항목별로 필드가 통째로 빠질 수 있어 전부 nullable 로 다룬다.
   *
   * <p>중첩 record 도 {@code Response} 로 끝나야 한다 — ArchUnit {@code DTO_NAMING} 이 {@code ..dto..} 의 모든
   * 클래스를 검사한다.
   */
  public record KakaoAccountResponse(String email, ProfileResponse profile) {}

  /** {@code kakao_account.profile}. */
  public record ProfileResponse(
      String nickname, String profileImageUrl, String thumbnailImageUrl) {}
}
