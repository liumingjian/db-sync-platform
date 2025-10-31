package com.dbsync.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing configuration
 *
 * @author DB Sync Platform
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides current auditor (user) for JPA auditing
     * TODO: Integrate with Spring Security to get actual user
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // For now, return system user
            // In production, this should return authenticated user from SecurityContext
            return Optional.of("system");
        };
    }
}
