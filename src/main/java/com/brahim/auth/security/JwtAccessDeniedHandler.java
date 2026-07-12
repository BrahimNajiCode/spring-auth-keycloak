package com.brahim.auth.security;

import com.brahim.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Called when a valid JWT is present but the user LACKS the required role.
 * Returns a clean JSON 403 — distinct from 401 (not authenticated).
 *
 * The 401 vs 403 distinction matters enormously in API design:
 *   401 = "I don't know who you are" → re-authenticate
 *   403 = "I know who you are, but you can't do this" → permission error
 */

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var error = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Access denied - insufficient permissions",
                request.getRequestURI(),
                Instant.now()
        );
        mapper.writeValue(response.getWriter(), error);
    }
}
