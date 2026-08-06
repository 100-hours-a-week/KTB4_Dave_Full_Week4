package com.example.community.auth.controller;

import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.auth.service.AuthService;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.resolver.SignUserArgumentResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                SignUserArgumentResolver.class,
                                JwtFilter.class,
                                RateLimitFilter.class
                        }
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebConfig.class
                )
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "authService")
    private AuthService authService;

    @Test
    @DisplayName("리프레시 성공 시 access token과 새 refresh cookie를 반환한다")
    void refreshSuccess() throws Exception {
        when(authService.refresh(OLD_REFRESH_TOKEN)).thenReturn(
                new RefreshResponse("access-token", "new-refresh-token")
        );

        mockMvc.perform(post("/auth/token")
                        .cookie(new Cookie("refresh", OLD_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("리프레시 성공"))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("access-token"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("refresh=new-refresh-token")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Path=/")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=1800")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("HttpOnly")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));

        verify(authService).refresh(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh cookie가 없으면 401 응답을 반환한다")
    void refreshReturnsUnauthorizedWhenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/auth/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("Refresh token이 존재하지 않습니다."));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("refresh cookie가 빈 값이면 401 응답을 반환한다")
    void refreshReturnsUnauthorizedWhenCookieIsEmpty() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .cookie(new Cookie("refresh", "")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("Refresh token이 존재하지 않습니다."));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("refresh 타입이 아닌 토큰이면 400 응답을 반환한다")
    void refreshReturnsBadRequestForInvalidTokenType() throws Exception {
        when(authService.refresh(OLD_REFRESH_TOKEN))
                .thenThrow(new BadRequestException("refresh토큰이 아닙니다."));

        mockMvc.perform(post("/auth/token")
                        .cookie(new Cookie("refresh", OLD_REFRESH_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("refresh토큰이 아닙니다."));

        verify(authService).refresh(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 401 응답을 반환한다")
    void refreshReturnsUnauthorizedForExpiredToken() throws Exception {
        when(authService.refresh(OLD_REFRESH_TOKEN))
                .thenThrow(new UnAuthorizedException("로그인이 필요합니다."));

        mockMvc.perform(post("/auth/token")
                        .cookie(new Cookie("refresh", OLD_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("로그인이 필요합니다."));

        verify(authService).refresh(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("DB에 저장된 리프레시 토큰이 없으면 404 응답을 반환한다")
    void refreshReturnsNotFoundWhenTokenDoesNotExist() throws Exception {
        when(authService.refresh(OLD_REFRESH_TOKEN)).thenThrow(
                new NotFoundException("유효하지 않은 리프레시 토큰")
        );

        mockMvc.perform(post("/auth/token")
                        .cookie(new Cookie("refresh", OLD_REFRESH_TOKEN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("유효하지 않은 리프레시 토큰"));

        verify(authService).refresh(OLD_REFRESH_TOKEN);
    }
}
