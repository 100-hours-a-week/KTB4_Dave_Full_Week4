package com.example.community.interceptor;

import com.example.community.domain.exception.UnAuthorizedException;
import com.example.community.domain.user.UserRole;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SignInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        List<MethodParameter> signUserParameters = Arrays.stream(handlerMethod.getMethodParameters())
                .filter(this::isSignUserParameter)
                .toList();

        if (signUserParameters.isEmpty()) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnAuthorizedException("유효하지 않은 토큰");
        }

        String accessToken = header.substring(BEARER_PREFIX.length());
        try {
            if(jwtUtil.isTokenExpired(accessToken)){
                throw new UnAuthorizedException("유효하지 않은 토큰");
            }
            Long userNum = jwtUtil.getUserNumFromToken(accessToken);
            Long profileId = jwtUtil.getProfileIdFromToken(accessToken);
            UserRole userRole = jwtUtil.getRoleFromToken(accessToken);
            request.setAttribute("userNum", userNum);
            request.setAttribute("profileId", profileId);
            request.setAttribute("role", userRole);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("아마 파싱 에러");
            throw new UnAuthorizedException("유효하지 않은 토큰");
        }
    }
    private boolean isSignUserParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SignUser.class)
                && SignUserInfo.class.isAssignableFrom(parameter.getParameterType());
    }
}
