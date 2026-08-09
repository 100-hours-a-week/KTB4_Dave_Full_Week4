package com.example.community.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    // IP별로 Bucket 관리 (메모리 기반)
    private static final int MAXIMUM_IP_BUCKETS = 100_000;
    private static final Duration BUCKET_IDLE_EXPIRATION =
            Duration.ofMinutes(10);

    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_IP_BUCKETS)
            .expireAfterAccess(BUCKET_IDLE_EXPIRATION)
            .build();
    //RateLimit 적용 대상 API 목록
    private static final String[] PROTECTED_PATHS = {
      "/users/email"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if(shouldRateLimit(requestURI)){
            String ip = getIP(request);
            Bucket bucket = bucketCache.get(ip, key -> createBucket());
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                writeTooManyRequestsResponse(response);
            }
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                .build();
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
        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
        {
          "code": "요청 횟수가 너무 많습니다.",
          "data": null
        }
        """);
    }
}
