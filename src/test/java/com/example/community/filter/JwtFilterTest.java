package com.example.community.filter;

import com.example.community.auth.dto.AccessTokenClaims;
import com.example.community.configuration.JwtAuthenticationEntryPoint;
import com.example.community.configuration.JwtAuthenticationException;
import com.example.community.user.entity.CustomUserDetails;
import com.example.community.user.entity.UserRole;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {
    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 인증 정보를 저장하고 다음 필터를 실행한다")
    void validAccessTokenSetsAuthenticationAndContinuesChain() throws Exception {
        MockHttpServletRequest request = requestWithBearerToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtUtil.parseAccessToken("valid-token"))
                .thenReturn(new AccessTokenClaims(1L, 2L, UserRole.USER));

        jwtFilter.doFilter(request, response, filterChain);

        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        assertThat(principal.getUserNum()).isEqualTo(1L);
        assertThat(principal.getProfileId()).isEqualTo(2L);
        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(eq(request), eq(response), argThat(e -> true));
    }

    @Test
    @DisplayName("토큰이 없으면 인증 정보 없이 다음 필터를 실행한다")
    void missingTokenContinuesChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Basic credentials"
    })
    @DisplayName("Authorization 헤더가 비어 있거나 Bearer로 시작하지 않으면 토큰이 없는 것으로 처리한다")
    void invalidAuthorizationHeaderContinuesChainWithoutAuthentication(
            String authorizationHeader
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorizationHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, authenticationEntryPoint);
    }

    @Test
    @DisplayName("만료된 토큰이면 만료 오류를 응답하고 다음 필터를 실행하지 않는다")
    void expiredTokenReturnsExpiredCodeWithoutContinuingChain() throws Exception {
        MockHttpServletRequest request = requestWithBearerToken("expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtUtil.parseAccessToken("expired-token"))
                .thenThrow(mock(ExpiredJwtException.class));

        jwtFilter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(
                eq(request),
                eq(response),
                argThat(exception -> hasCode(exception, "ACCESS_TOKEN_EXPIRED"))
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 토큰이면 유효하지 않은 토큰 오류를 응답하고 다음 필터를 실행하지 않는다")
    void invalidTokenReturnsInvalidCodeWithoutContinuingChain() throws Exception {
        MockHttpServletRequest request = requestWithBearerToken("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtUtil.parseAccessToken("invalid-token"))
                .thenThrow(new MalformedJwtException("invalid"));

        jwtFilter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(
                eq(request),
                eq(response),
                argThat(exception -> hasCode(exception, "INVALID_ACCESS_TOKEN"))
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithBearerToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private boolean hasCode(Exception exception, String code) {
        return exception instanceof JwtAuthenticationException jwtException
                && jwtException.getCode().equals(code);
    }
}
