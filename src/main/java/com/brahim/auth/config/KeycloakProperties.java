package com.brahim.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed binding for all custom keycloak.* properties in application.yml.
 *
 * WHY: @Value("${keycloak.server-url}") scattered across 10 classes = maintenance nightmare.
 *      A single @ConfigurationProperties bean is validated at startup and injected wherever needed.
 *      This is the approach used in production-grade Spring apps.
 */
@Validated
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    @NotBlank String serverUrl,
    @NotBlank String realm,
    @NotBlank String adminClientId,
    @NotBlank String adminClientSecret,
    @NotBlank String publicClientId
){}
