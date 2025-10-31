package com.dbsync.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 *
 * @author DB Sync Platform
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 开发环境安全配置 - 禁用认证,方便开发和测试
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 生产环境安全配置 - 启用JWT认证
     * TODO: Week 5-6 实现完整的JWT认证和授权
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 公开端点
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // 其他端点需要认证
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
