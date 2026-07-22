package com.example.community.filter;

import com.example.community.configuration.JwtAuthenticationEntryPoint;
import com.example.community.user.entity.CustomUserDetails;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Authorization 헤더가 존재하면 확인하고 아니면 그냥 보내기
        try {
            String access = extractJwtFromRequest(request);
            if (access != null && jwtUtil.isAccessToken(access)) {
                Long userNum = jwtUtil.getUserNumFromToken(access);
                Long profileId = jwtUtil.getProfileIdFromToken(access);
                String role = jwtUtil.getRoleFromToken(access);
                CustomUserDetails userDetails = new CustomUserDetails(userNum, profileId, role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch(ExpiredJwtException e){
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException(
                            "Access token has expired.",
                            e
                    )
            );
        } catch (JwtException e)  {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");

        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)){
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
