package com.brahim.auth.service.impl;

import com.brahim.auth.config.KeycloakProperties;
import com.brahim.auth.dto.response.TokenResponse;
import com.brahim.auth.exception.BusinessException;
import com.brahim.auth.service.KeycloakTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static com.brahim.auth.exception.ErrorCode.AUTH_FAILED;
import static com.brahim.auth.exception.ErrorCode.EMPTY_RESPONSE;


/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  BRIDGE IMPLEMENTATION: HTTP calls to Keycloak's token endpoint
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *  Keycloak exposes a standard OAuth2 token endpoint:
 *    POST /realms/{realm}/protocol/openid-connect/token
 *    Content-Type: application/x-www-form-urlencoded
 *
 *  This service wraps that endpoint for three flows:
 *    1. ROPC (login with username + password)
 *    2. Refresh token grant
 *    3. Token revocation (logout)
 *
 *  USES Spring Boot 3.2's RestClient (the modern replacement for RestTemplate).
 *  RestClient is fluent, synchronous, and request-scoped — perfect here.
 *  For reactive apps, use WebClient instead.
 */
@Slf4j
@Service
public class KeycloakTokenServiceImpl implements KeycloakTokenService {
    private final RestClient restClient;
    private final KeycloakProperties props;
    private final String tokenEndpoint;
    private final String logoutEndpoint;

    public KeycloakTokenServiceImpl(KeycloakProperties props) {
        this.props = props;
        this.tokenEndpoint = props.serverUrl()+"/realms/"+props.realm()
                + "/protocol/openid-connect/token";
        this.logoutEndpoint = props.serverUrl() + "/realms/" + props.realm()
                + "/protocol/openid-connect/logout" ;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


    // Login: Resource Owner Password Credentials (ROPC)
    @Override
    public TokenResponse obtainToken(String username, String password) {
        log.debug("Requesting token for user '{}'", username);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.publicClientId());
        form.add("client_secret", props.adminClientSecret());
        form.add("username", username);
        form.add("password", password);
        return postToTokenEndpoint(form);

    }


    // Refresh: exchange refresh token for new token pair
    @Override
    public TokenResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "refresh_token");
        form.add("client_id",     props.publicClientId());
        form.add("client_secret", props.adminClientSecret());
        form.add("refresh_token", refreshToken);

        return postToTokenEndpoint(form);
    }

    // Logout: revoke the refresh token in Keycloak
    @Override
    public void revokeToken(String refreshToken) {
        log.debug("Revoking refresh token");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id",     props.publicClientId());
        form.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(logoutEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();   // 204 No Content on success
        } catch (RestClientResponseException ex) {
            log.warn("Token revocation failed: {}", ex.getMessage());
            // Don't re-throw — logout is best-effort
        }
    }

    // Private helpers
    @SuppressWarnings("unchecked")
    private TokenResponse postToTokenEndpoint(MultiValueMap<String, String> form) {
        try{
            Map<String, Object> body = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class); // parse raw JSON map
            if(body == null) throw new BusinessException(EMPTY_RESPONSE);
            return new TokenResponse(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token"),
                    (String) body.getOrDefault("token_type", "Bearer"),
                    toLong(body.get("expires_in")),
                    toLong(body.get("refresh_expires_in"))
            );
        }catch(RestClientResponseException ex){
            log.error("Keycloak token endpoint returned {}: {}"
                    , ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(AUTH_FAILED, ex.getMessage());
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}
