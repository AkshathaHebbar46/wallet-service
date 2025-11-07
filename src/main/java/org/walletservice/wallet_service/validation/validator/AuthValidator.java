package org.walletservice.wallet_service.validation.validator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.jwt.JwtService;

@Component
public class AuthValidator {

    private final WalletRepository walletRepository;
    private final JwtService jwtService;

    public AuthValidator(WalletRepository walletRepository, JwtService jwtService) {
        this.walletRepository = walletRepository;
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

    public boolean isAuthorizedForWallet(AuthContext auth, Long walletId) {
        // Admins can access any wallet
        if (auth.isAdmin()) {
            return true;
        }

        // Normal users can only access their own wallets
        return walletRepository.findById(walletId)
                .map(wallet -> wallet.getUserId().equals(auth.getUserId()))
                .orElse(false);
    }
}
