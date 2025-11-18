package org.walletservice.wallet_service.controller.internal;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.wallet.InternalWalletService;

@Hidden
@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private final InternalWalletService walletService;

    @Value("${internal.auth.token}")
    private String internalAuthToken;

    public InternalWalletController(InternalWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{walletId}/validate")
    public ResponseEntity<String> validateWallet(
            @RequestHeader("Internal-Token") String token,
            @PathVariable Long walletId) {

        if (!internalAuthToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        try {
            walletService.validateWallet(walletId);
            return ResponseEntity.ok("Wallet valid");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
