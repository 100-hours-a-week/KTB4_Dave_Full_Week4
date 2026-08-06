package com.example.community.post.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "popular-post")
public record PopularPostProperties(Duration candidateMaxAge) {
    public PopularPostProperties {
        if (candidateMaxAge == null
                || candidateMaxAge.isZero()
                || candidateMaxAge.isNegative()) {
            throw new IllegalArgumentException(
                    "인기글 후보 기간은 0보다 커야 합니다."
            );
        }
    }
}
