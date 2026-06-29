package com.example.community.configuration;

import com.example.community.interceptor.SignInterceptor;
import com.example.community.resolver.SignUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final SignInterceptor signInterceptor;
    private final SignUserArgumentResolver signUserArgumentResolver;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://127.0.0.1:5500")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(signInterceptor)
                .addPathPatterns(
                        "/posts/**",
                        "/comments/**",
                        "/temporaryPost/**",
                        "/users/**"
                )
                .excludePathPatterns(
                        "/users/email",
                        "/users/nickname"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

        resolvers.add(signUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadRoot = Paths.get(System.getProperty("user.dir"), "app", "uploads")
                .toUri()
                .toString();

        registry.addResourceHandler("/images/profiles/**")
                .addResourceLocations(uploadRoot +"profiles/");
        registry.addResourceHandler("/images/posts/**")
                .addResourceLocations(uploadRoot + "posts/");
    }
}
