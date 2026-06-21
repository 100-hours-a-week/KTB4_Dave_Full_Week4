package com.example.community.interceptor;

import com.example.community.domain.exception.UnAuthorizedException;
import com.example.community.domain.user.UserRole;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

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

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnAuthorizedException("유효하지 않은 토큰");
        }

        String accessToken = header.substring(BEARER_PREFIX.length());
        if(jwtUtil.isTokenExpired(accessToken)){
            throw new UnAuthorizedException("유효하지 않은 토큰");
        }
        try {
            Long userNum = jwtUtil.getUserNumFromToken(accessToken);
            Long profileId = jwtUtil.getProfileIdFromToken(accessToken);
            UserRole userRole = jwtUtil.getRoleFromToken(accessToken);
            request.setAttribute("userNum", userNum);
            request.setAttribute("profileId", profileId);
            request.setAttribute("role", userRole);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnAuthorizedException("유효하지 않은 토큰");
        }
    }
}
