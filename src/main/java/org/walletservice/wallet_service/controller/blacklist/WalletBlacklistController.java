package org.walletservice.wallet_service.controller.blacklist;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

import java.util.Map;

@RestController
@RequestMapping("/admin/wallets/blacklist")
@Tag(name = "Wallet Blacklist APIs", description = "Endpoints for blacklisting/unblocking wallets")
public class WalletBlacklistController {

    private static final Logger log = LoggerFactory.getLogger(WalletBlacklistController.class);

    private final WalletBlacklistService walletBlacklistService;

    public WalletBlacklistController(WalletBlacklistService walletBlacklistService) {
        this.walletBlacklistService = walletBlacklistService;
    }

    @Operation(summary = "Blacklist all wallets of a user", description = "Blacklists all wallets for a specific user. Can be called by Admin or user-service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets blacklisted successfully")
    })
    @PostMapping
    public void blacklistWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Request received to blacklist all wallets for userId={}", userId);

        walletBlacklistService.blacklistUserWallets(userId);

        log.info("All wallets for userId={} successfully blacklisted", userId);
    }

    @Operation(summary = "Unblock all wallets of a user", description = "Unblocks all wallets for a specific user. Can be called by Admin or user-service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets unblocked successfully")
    })
    @PostMapping("/unblock")
    public void unblockWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Request received to unblock all wallets for userId={}", userId);

        walletBlacklistService.unblockUserWallets(userId);

        log.info("All wallets for userId={} successfully unblocked", userId);
    }

    @Operation(summary = "Blacklist a specific wallet", description = "Blacklist a single wallet by its ID. Called by user-service during admin-level wallet block.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet blacklisted successfully")
    })
    @PostMapping("/wallet/{walletId}")
    public void blacklistWallet(@PathVariable Long walletId) {
        log.info("Request received to blacklist walletId={}", walletId);

        walletBlacklistService.blacklistWallet(walletId);

        log.info("Wallet with id={} successfully blacklisted", walletId);
    }

    @Operation(summary = "Unblock a specific wallet", description = "Unblock a single wallet by its ID. Called by user-service during admin-level wallet unblock.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet unblocked successfully")
    })
    @PostMapping("/wallet/{walletId}/unblock")
    public void unblockWallet(@PathVariable Long walletId) {
        log.info("Request received to unblock walletId={}", walletId);

        walletBlacklistService.unblockWallet(walletId);

        log.info("Wallet with id={} successfully unblocked", walletId);
    }
}
