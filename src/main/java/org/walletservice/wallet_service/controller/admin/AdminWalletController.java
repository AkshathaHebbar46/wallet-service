package org.walletservice.wallet_service.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminWalletController {

    private static final Logger log = LoggerFactory.getLogger(AdminWalletController.class);

    private final WalletService walletService;
    private final AuthValidator authValidator;

    public AdminWalletController(WalletService walletService, AuthValidator authValidator) {
        this.walletService = walletService;
        this.authValidator = authValidator;
    }

    @PostMapping("/all-wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WalletResponseDTO>> getAllWalletsForUser(
            @RequestBody UserIdRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);

        if (!auth.isAdmin()) {
            return ResponseEntity.status(403).build(); // Forbidden for non-admins
        }

        Long userId = request.getUserId();
        log.info("Admin fetching wallets for userId={}", userId);

        List<WalletResponseDTO> wallets = walletService.getWalletsByUser(userId);

        if (wallets.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 if no wallets
        }

        return ResponseEntity.ok(wallets);
    }
}
