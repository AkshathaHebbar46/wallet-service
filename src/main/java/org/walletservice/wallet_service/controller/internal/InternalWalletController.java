package org.walletservice.wallet_service.controller.internal;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

@Hidden
@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private final WalletRepository walletRepository;

    // Internal token configured in application.properties or environment
    @Value("${internal.auth.token}")
    private String internalAuthToken;

    public InternalWalletController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Internal endpoint to validate a wallet.
     * Only accessible with the internal token.
     */
    @GetMapping("/{walletId}/validate")
    public ResponseEntity<String> validateWallet(
            @RequestHeader("Internal-Token") String token,
            @PathVariable Long walletId) {

        // Reject if token does not match
        if (!internalAuthToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        // Fetch wallet
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        // Minimal validation: only check active status
        if (Boolean.FALSE.equals(wallet.getActive())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wallet inactive");
        }

        return ResponseEntity.ok("Wallet valid");
    }
}
