package com.example.community.filter.bucket;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BucketConfig {
    private final BucketConfiguration configuration;

    public BucketConfig(){
        configuration = BucketConfiguration.builder()
                .addLimit(limit ->
                        limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)).id("emailCheck"))
                .build();
    }

}
