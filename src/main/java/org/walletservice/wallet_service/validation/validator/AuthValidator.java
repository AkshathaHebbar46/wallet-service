package org.walletservice.wallet_service.validation.validator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.jwt.JwtService;

@Component
public class AuthValidator {

    private final JwtService jwtService;

    public AuthValidator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        return header.substring(7);
    }

    public AuthContext getAuthContext(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));
        return new AuthContext(token, userId, isAdmin);
    }

    public boolean isAuthorized(AuthContext ctx, Long targetUserId) {
        return ctx.isAdmin() || ctx.getUserId().equals(targetUserId);
    }
}
