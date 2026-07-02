package com.example.community.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    //RateLimit 적용 대상 API 목록
    private static final String[] PROTECTED_PATHS = {
      "/users/email"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if(shouldRateLimit(requestURI)){
            String ip = getIP(request);

        }
    }

    private boolean shouldRateLimit(String uri){
        for(String path : PROTECTED_PATHS){
            if(uri.equals(path)){
                return true;
            }
        }
        return false;
    }

    private String getIP(HttpServletRequest request){
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
