package com.example.community.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
            .build();
    // IP별로 Bucket 관리 (메모리 기반)
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    //RateLimit 적용 대상 API 목록
    private static final String[] PROTECTED_PATHS = {
      "/users/email"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if(shouldRateLimit(requestURI)){
            String ip = getIP(request);
            Bucket bucket = bucketCache.computeIfAbsent(ip, key -> createBucket());
            // tryConsume returns false immediately if no tokens available with the bucket
            if (bucket.tryConsume(1)) {
                // the limit is not exceeded
                filterChain.doFilter(request, response);
            } else {
                // limit is exceeded=
                response.setContentType("text/plain");
                response.setStatus(429);
                response.getWriter().append("Too many requests");
            }
        }
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
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
