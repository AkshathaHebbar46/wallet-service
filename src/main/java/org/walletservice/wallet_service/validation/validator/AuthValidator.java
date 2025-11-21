package org.walletservice.wallet_service.validation.validator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.exception.WalletNotFoundException;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.jwt.JwtService;

import java.util.Optional;

@Component
public class AuthValidator {

    private final WalletRepository walletRepository;
    private final JwtService jwtService;

    public AuthValidator(WalletRepository walletRepository,
                         JwtService jwtService) {
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

    public Long extractUserId(String token) {
        return jwtService.extractUserId(token);
    }


    public AuthContext getAuthContext(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));
        return new AuthContext(token, userId, isAdmin);
    }

    public boolean isAuthorizedForWallet(AuthContext auth, Long walletId) {
        Optional<WalletEntity> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            throw new WalletNotFoundException("Wallet with ID " + walletId + " does not exist.");
        }

        WalletEntity wallet = walletOpt.get();

        if (!wallet.getUserId().equals(auth.getUserId())) {
            throw new UnauthorizedAccessException("You are not allowed to access this wallet.");
        }

        if (!Boolean.TRUE.equals(wallet.getActive())) {
            throw new UnauthorizedAccessException("Wallet is inactive or blacklisted.");
        }

        return true;
    }
}