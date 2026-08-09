package com.example.community.configuration;

import com.example.community.post.configuration.PopularPostCacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(PopularPostCacheProperties.class)
public class CacheConfig {
}
