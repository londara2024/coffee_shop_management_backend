package com.aura.coffeeshop.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/** Turns on automatic created/updated date and actor columns for every {@code Auditable} entity. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    static final String SYSTEM_ACTOR = "system";

    /**
     * Who is making the change. There is no authentication yet, so every change is attributed to
     * {@code system}. Once Spring Security is added, return the signed-in user's name/id from the
     * {@code SecurityContextHolder} here; nothing else needs to change.
     */
    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(SYSTEM_ACTOR);
    }
}
