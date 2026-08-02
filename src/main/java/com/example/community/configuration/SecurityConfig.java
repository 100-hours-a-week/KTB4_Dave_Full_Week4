package com.example.community.configuration;

import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/auth/**").permitAll();
                    auth.requestMatchers("/images/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/users","/users/email", "/users/nickname", "/users/state").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/posts").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/posts/popular").permitAll();
                    auth.requestMatchers(regexMatcher(HttpMethod.GET, "^/posts/[0-9]+$")).permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/comments/list/**").permitAll();

                    auth.requestMatchers("/admin/**").hasRole("ADMIN");

                    auth.requestMatchers(HttpMethod.GET, "/users/myLike").authenticated();
                    auth.requestMatchers(HttpMethod.PATCH, "/users/info", "/users/password").authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, "/users").authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, "/users/state").authenticated();
                    auth.requestMatchers("/temporaryPost").authenticated();
                    auth.requestMatchers("/temporaryPost/**").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/posts/my").authenticated();
                    auth.requestMatchers(regexMatcher(HttpMethod.GET, "^/posts/[0-9]+/like$")).authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/posts", "/posts/**").authenticated();
                    auth.requestMatchers(HttpMethod.PATCH, "/posts/**").authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, "/posts/**").authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/comments/**").authenticated();
                    auth.requestMatchers(HttpMethod.PATCH, "/comments/**").authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, "/comments/**").authenticated();

                    auth.anyRequest().authenticated();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                )

                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
