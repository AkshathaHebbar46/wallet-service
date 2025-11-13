package org.walletservice.wallet_service.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.walletservice.wallet_service.dto.response.ErrorResponseDTO;

import java.io.IOException;

/**
 * Handles Spring Security authentication and authorization exceptions.
 * - AuthenticationEntryPoint: triggered when an unauthenticated user tries to access a secured endpoint.
 * - AccessDeniedHandler: triggered when an authenticated user tries to access a resource without proper roles/permissions.
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Called when an unauthenticated user tries to access a secured endpoint.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthorized access attempt to '{}': {}", request.getRequestURI(), authException.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "You must be logged in to access this resource."
        );
        writeResponse(response, error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Called when an authenticated user tries to access a resource they don't have permission for.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied to '{}': {}", request.getRequestURI(), accessDeniedException.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource."
        );
        writeResponse(response, error, HttpStatus.FORBIDDEN);
    }

    /**
     * Writes the ErrorResponseDTO as JSON to the HttpServletResponse.
     */
    private void writeResponse(HttpServletResponse response, ErrorResponseDTO error, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
