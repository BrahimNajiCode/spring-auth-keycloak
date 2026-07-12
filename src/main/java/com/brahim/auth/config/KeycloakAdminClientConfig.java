package com.brahim.auth.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  THE BRIDGE: KeycloakAdminClientConfig
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *  This is the first half of the bridge. It creates a Keycloak admin client
 *  that your service layer uses to:
 *    • Create / update / delete users in Keycloak
 *    • Assign roles
 *    • Send verification emails
 *    • Reset passwords
 *    • List users
 *
 *  HOW IT WORKS:
 *    KeycloakBuilder.builder().build() builds an HTTP client configured with
 *    CLIENT_CREDENTIALS grant. When your service first calls it, the client
 *    fetches an admin token silently and caches it (auto-renews on expiry).
 *
 *  SENIOR TIP:
 *    The Keycloak bean is @Bean-scoped as singleton — the admin client is
 *    thread-safe and expensive to create, so one instance per app is correct.
 *    Never instantiate Keycloak inside a @Service method.
 */

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminClientConfig {
    @Bean
    public Keycloak KeycloakAdminClient(KeycloakProperties props){
        return KeycloakBuilder.builder()
                .serverUrl(props.serverUrl())
                .realm(props.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(props.adminClientId())
                .clientSecret(props.adminClientSecret())
                .build();
    }
}
