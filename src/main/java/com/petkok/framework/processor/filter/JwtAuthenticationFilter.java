package com.petkok.framework.processor.filter;

import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 매 요청마다 Access Token 검증 → SecurityContext 에 AuthPrincipal 주입.
 *
 * <p><b>서명·타입만으로는 부족하다.</b> 토큰이 유효해도 그 사이 탈퇴했을 수 있고, JWT 는 상태를 모르므로 탈퇴 후에도 최대 {@code
 * JWT_ACCESS_TTL}(기본 30분) 동안 통과한다. 그래서 {@link UserStatusChecker} 로 활성 여부를 확인한다 (PLAN-REQ-08 D2).
 *
 * <p>⚠️ <b>{@code UserRepository} 를 직접 참조하지 말 것.</b> 구조 규칙 두 개가 동시에 깨진다 — 이유는 {@link
 * UserStatusChecker} 에 적어 두었다. 포트를 쓰는 것이 이 필터 설계의 전부다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private final JwtTokenProvider tokenProvider;
  private final UserStatusChecker userStatusChecker;

  public JwtAuthenticationFilter(
      JwtTokenProvider tokenProvider, UserStatusChecker userStatusChecker) {
    this.tokenProvider = tokenProvider;
    this.userStatusChecker = userStatusChecker;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);
    if (token != null
        && tokenProvider.validate(token)
        && tokenProvider.isAccessToken(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      UUID userId = tokenProvider.getUserId(token);
      // 탈퇴 사용자면 인증을 세팅하지 않고 그대로 통과시킨다. 검증 계약 REQ-08-16 · 18.
      //
      // ⚠️ 여기서 예외를 던지면 안 된다. 필터는 DispatcherServlet 앞이라 GlobalExceptionHandler 에
      //    닿지 않고, 응답 형태가 인증 실패 규격과 갈린다. 세팅하지 않고 넘기면
      //    SecurityConfig 의 authenticationEntryPoint 가 기존 규격대로
      //    ApiResponse.error(UNAUTHORIZED) + 401 을 내려준다.
      if (!userStatusChecker.isActive(userId)) {
        filterChain.doFilter(request, response);
        return;
      }
      AuthPrincipal principal = new AuthPrincipal(userId);
      var authentication =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HEADER);
    if (header != null && header.startsWith(PREFIX)) {
      return header.substring(PREFIX.length());
    }
    return null;
  }
}
