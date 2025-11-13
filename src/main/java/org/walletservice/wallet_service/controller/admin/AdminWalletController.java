package org.walletservice.wallet_service.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletFreezeService;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminWalletController {

    private static final Logger log = LoggerFactory.getLogger(AdminWalletController.class);

    private final WalletService walletService;
    private final AuthValidator authValidator;
    private final WalletFreezeService walletFreezeService;
    private final WalletTransactionService walletTransactionService;


    public AdminWalletController(WalletService walletService, AuthValidator authValidator, WalletFreezeService walletFreezeService, WalletTransactionService walletTransactionService) {
        this.walletService = walletService;
        this.authValidator = authValidator;
        this.walletFreezeService = walletFreezeService;
        this.walletTransactionService = walletTransactionService;
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

    /**
     * Freeze a wallet (admin only)
     */
    @PostMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<String> freezeWallet(@PathVariable Long walletId, HttpServletRequest httpRequest) {
        AuthContext auth = authValidator.getAuthContext(httpRequest);
        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        // Fetch WalletEntity from WalletService
        WalletEntity wallet = walletService.getWalletById(walletId);

        // Freeze the wallet
        walletFreezeService.freezeWallet(wallet);

        log.info("Wallet {} frozen by admin", walletId);
        return ResponseEntity.ok("Wallet " + walletId + " frozen successfully");
    }

    /**
     * Unfreeze a wallet (admin only)
     */
    @PostMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<String> unfreezeWallet(@PathVariable Long walletId, HttpServletRequest httpRequest) {
        AuthContext auth = authValidator.getAuthContext(httpRequest);
        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        // Fetch WalletEntity from WalletService
        WalletEntity wallet = walletService.getWalletById(walletId);

        // Unfreeze the wallet
        walletFreezeService.unfreezeWallet(wallet);

        log.info("Wallet {} unfrozen by admin", walletId);
        return ResponseEntity.ok("Wallet " + walletId + " unfrozen successfully");
    }

    /**
     * Get all transactions for a specific wallet
     */
    @GetMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> getWalletTransactions(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);

        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        log.info("Admin fetching transactions for walletId={}", walletId);

        List<WalletTransactionResponseDTO> transactions = walletTransactionService.listTransactions(walletId);

        if (transactions.isEmpty()) {
            throw new org.walletservice.wallet_service.exception.WalletNotFoundException(
                    "No transactions found for wallet " + walletId
            );
        }

        return ResponseEntity.ok(transactions);
    }
}
