package com.example.community.configuration;

import com.example.community.interceptor.SignInterceptor;
import com.example.community.resolver.SignUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final SignInterceptor signInterceptor;
    private final SignUserArgumentResolver signUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(signInterceptor)
                .addPathPatterns(
                        "/posts/**",
                        "/comments/**",
                        "/temporaryPost/**",
                        "/users/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

        resolvers.add(signUserArgumentResolver);
    }

}
