package com.example.community.configuration;

import com.example.community.post.configuration.PopularPostProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PopularPostProperties.class)
public class SchedulingConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
