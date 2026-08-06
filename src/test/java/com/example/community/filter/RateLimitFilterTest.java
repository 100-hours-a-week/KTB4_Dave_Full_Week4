package com.example.community.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("이메일 중복 확인은 같은 IP에서 분당 10회까지 허용한다")
    void emailDuplicateCheckAllowsTenRequestsPerIp() throws Exception {
        for (int requestCount = 0; requestCount < 10; requestCount++) {
            rateLimitFilter.doFilter(
                    request("/users/email", "127.0.0.1"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("이메일 중복 확인은 같은 IP의 11번째 요청에 429를 응답한다")
    void emailDuplicateCheckRejectsEleventhRequestPerIp() throws Exception {
        for (int requestCount = 0; requestCount < 10; requestCount++) {
            rateLimitFilter.doFilter(
                    request("/users/email", "127.0.0.1"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                request("/users/email", "127.0.0.1"),
                response,
                filterChain
        );

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains(
                "\"code\": \"요청 횟수가 너무 많습니다.\"",
                "\"data\": null"
        );
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rate Limit 대상이 아닌 경로는 요청 횟수를 제한하지 않는다")
    void unprotectedPathIsNotRateLimited() throws Exception {
        for (int requestCount = 0; requestCount < 11; requestCount++) {
            rateLimitFilter.doFilter(
                    request("/users/nickname", "127.0.0.1"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        verify(filterChain, times(11)).doFilter(any(), any());
    }

    @Test
    @DisplayName("이메일 중복 확인의 요청 횟수는 IP별로 구분한다")
    void emailDuplicateCheckUsesSeparateBucketPerIp() throws Exception {
        for (int requestCount = 0; requestCount < 10; requestCount++) {
            rateLimitFilter.doFilter(
                    request("/users/email", "127.0.0.1"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        rateLimitFilter.doFilter(
                request("/users/email", "127.0.0.2"),
                new MockHttpServletResponse(),
                filterChain
        );

        verify(filterChain, times(11)).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String uri, String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
