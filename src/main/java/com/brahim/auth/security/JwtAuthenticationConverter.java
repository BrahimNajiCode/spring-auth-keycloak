package com.brahim.auth.security;

import com.brahim.auth.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  THE KEY BRIDGE CLASS: JwtAuthenticationConverter
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *  PROBLEM IT SOLVES:
 *    Keycloak embeds roles differently than Spring expects.
 *    Spring looks for roles in "scope" or "authorities".
 *    Keycloak puts realm roles under:   jwt.realm_access.roles
 *    Keycloak puts client roles under:  jwt.resource_access.<client-id>.roles
 *
 *    Without this converter, Spring Security sees NO roles and every
 *    @PreAuthorize("hasRole('ADMIN')") silently fails with 403.
 *
 *  WHAT THIS CLASS DOES:
 *    1. Receives the validated Jwt object (already signature-verified).
 *    2. Reads both realm roles AND client-specific roles from the JWT.
 *    3. Converts them to Spring's SimpleGrantedAuthority("ROLE_XXX").
 *    4. Returns a JwtAuthenticationToken stored in SecurityContextHolder.
 *
 *  JWT STRUCTURE (Keycloak example):
 *  {
 *    "sub": "uuid-of-user",
 *    "preferred_username": "brahim",
 *    "email": "brahim@example.com",
 *    "realm_access": {
 *      "roles": ["ROLE_USER", "offline_access"]
 *    },
 *    "resource_access": {
 *      "my-app": {
 *        "roles": ["ROLE_ADMIN"]
 *      }
 *    }
 *  }
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final KeycloakProperties props;
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                extractRealmRoles(jwt).stream(),
                extractClientRoles(jwt).stream()
        ).toList();
        log.debug("JWT sub={} granted authorities={}",
                jwt.getSubject(), authorities);
        return new JwtAuthenticationToken(jwt, authorities, getPrincipalName(jwt));
    }

    // Realm roles: available to ALL clients in the realm
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(prefixRole(role)))
                .map(auth -> (GrantedAuthority) auth)
                .toList();
    }


    // Client roles: scoped to a specific client (e.g. "my-app")
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) return List.of();
        // Look for roles under the public client id (the front-end client)
        Map<String, Object> clientAccess =
                (Map<String, Object>) resourceAccess.get(props.publicClientId());
        if (clientAccess == null) return List.of();

        List<String> roles = (List<String>) clientAccess.getOrDefault("roles", List.of());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(prefixRole(role)))
                .map(auth -> (GrantedAuthority) auth)
                .toList();
    }


    /**
     * Spring Security's hasRole("ADMIN") automatically prepends "ROLE_".
     * If Keycloak already stores "ROLE_ADMIN" we do not double-prefix.
     */
    private String prefixRole(String role) {
        return role.startsWith("ROLE_") ? role: "ROLE_"+role;
    }

    private String getPrincipalName(Jwt jwt) {
        String username = jwt.getClaim("preferred_username");
        return username !=null ? username: jwt.getSubject();
    }
}
