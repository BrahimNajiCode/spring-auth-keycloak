package com.brahim.auth.security;

import com.brahim.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;

/**
 * Called when a request reaches a protected endpoint with NO valid JWT.
 * Returns a clean JSON 401 instead of Spring's default HTML error page.
 *
 * SENIOR TIP:
 *   Never let Spring return HTML error pages to API clients.
 *   Always wire custom entry points for AuthenticationException (401)
 *   and AccessDeniedException (403) separately — they have different semantics.
 */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var error = new ApiErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication required - provide a valid Bearer Token",
            request.getRequestURI(),
            Instant.now()
        );
        mapper.writeValue(response.getWriter(), error);
    }
}
