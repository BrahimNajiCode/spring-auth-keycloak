package com.brahim.auth.security;


import com.brahim.auth.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.Objects;

/**
 * Utility bean for accessing the current authenticated user's JWT claims
 * from anywhere in the application layer.
 *
 * USAGE IN SERVICE:
 *   String userId = securityContextHelper.getCurrentUserId();
 *   String email  = securityContextHelper.getCurrentUserEmail();
 *
 * SENIOR TIP:
 *   Never inject HttpServletRequest into @Service classes to extract auth info.
 *   The SecurityContextHolder is the correct place to read identity.
 *   Keep service free of HTTP concerns — they are not controller.
 */
@Component
public class SecurityContextHelper {
    public Jwt getCurrentJwt(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        /** Verify and cast in the same time.
         * It's equivalent to:
         * if (auth instanceof JwtAuthenticationToken) {
         *     JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
         * }
         * */
        if(auth instanceof JwtAuthenticationToken jwtAuth){
            return jwtAuth.getToken();
        }
        throw new UnauthorizedException("No authenticated JWT found in SecurityContext");
    }

    /** Returns the Keycloak user UUID (sub claim). */
    public String getCurrentUserId(){
        return getCurrentJwt().getSubject();
    }

    /** Returns the human-readable username. */
    public String getCurrentUsername(){
        return getCurrentJwt().getClaim("preferred_username");
    }

    /** Returns the user's email from the JWT. */
    public String getCurrentEmail(){
        return getCurrentJwt().getClaim("email");
    }

    /** Checks if the current user has a specific realm role. */
    public boolean hasRole(String role){
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                .getAuthorities().stream()
                .anyMatch(a-> Objects.equals(a.getAuthority(), "ROLE_" + role));
    }
}
