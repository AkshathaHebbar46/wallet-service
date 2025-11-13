package org.walletservice.wallet_service.service.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.walletservice.wallet_service.util.JwtUtil;

/**
 * Service to handle JWT operations using JwtUtil:
 * - validation
 * - extraction of claims like email, userId, and role
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Validates the token.
     * @param token JWT string
     * @return true if valid, false if invalid
     */
    public boolean isTokenValid(String token) {
        boolean valid = jwtUtil.validateToken(token);
        if (valid) {
            log.debug("✅ Token is valid");
        } else {
            log.warn("❌ Invalid JWT token");
        }
        return valid;
    }

    /**
     * Extracts email (subject) from JWT token.
     */
    public String extractEmail(String token) {
        String email = jwtUtil.extractUsername(token);
        log.debug("Extracted email from token: {}", email);
        return email;
    }

    /**
     * Extracts userId from JWT claims.
     */
    public Long extractUserId(String token) {
        Long userId = jwtUtil.extractUserId(token);
        log.debug("Extracted userId from token: {}", userId);
        return userId;
    }

    /**
     * Extracts role from JWT claims.
     */
    public String extractRole(String token) {
        String role = jwtUtil.extractRole(token);
        log.debug("Extracted role from token: {}", role);
        return role;
    }
}
