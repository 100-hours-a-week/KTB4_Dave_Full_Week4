package com.example.community.post.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "popular-post.cache")
public class PopularPostCacheProperties {
    private boolean enabled = true;
    private Duration listTtl = Duration.ofMinutes(15);
    private Duration bodyIdleTtl = Duration.ofMinutes(10);
    private Duration bodyMaxTtl = Duration.ofMinutes(30);
    private Duration stateTtl = Duration.ofMinutes(1);
    private Duration commentIdleTtl = Duration.ofMinutes(10);
    private Duration commentMaxTtl = Duration.ofMinutes(30);
    private long bodyMaxWeightBytes = 4L * 1024 * 1024;
    private long commentMaxWeightBytes = 8L * 1024 * 1024;
    private long stateMaxSize = 20;
    private long commentIndexMaxSize = 20;
}
