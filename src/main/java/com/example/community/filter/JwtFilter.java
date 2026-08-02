package com.example.community.filter;

import com.example.community.auth.dto.AccessTokenClaims;
import com.example.community.configuration.JwtAuthenticationEntryPoint;
import com.example.community.configuration.JwtAuthenticationException;
import com.example.community.user.entity.CustomUserDetails;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JWTUtil jwtUtil;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = extractJwtFromRequest(request);
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = jwtUtil.parseAccessToken(accessToken);
            CustomUserDetails userDetails = new CustomUserDetails(
                    claims.userNum(),
                    claims.profileId(),
                    claims.role()
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ExpiredJwtException e) {
            commenceUnauthorized(request, response, "ACCESS_TOKEN_EXPIRED", e);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            commenceUnauthorized(request, response, "INVALID_ACCESS_TOKEN", e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void commenceUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String code,
            Exception cause
    ) throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new JwtAuthenticationException(code, cause)
        );
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
