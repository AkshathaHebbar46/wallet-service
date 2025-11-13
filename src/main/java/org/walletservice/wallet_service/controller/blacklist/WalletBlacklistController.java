package org.walletservice.wallet_service.controller.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

import java.util.Map;

@RestController
@RequestMapping("/admin/wallets/blacklist")
public class WalletBlacklistController {

    private static final Logger log = LoggerFactory.getLogger(WalletBlacklistController.class);

    private final WalletBlacklistService walletBlacklistService;

    public WalletBlacklistController(WalletBlacklistService walletBlacklistService) {
        this.walletBlacklistService = walletBlacklistService;
    }

    /**
     * Blacklist all wallets for a specific user
     * Called by Admin or user-service
     */
    @PostMapping
    public void blacklistWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Request received to blacklist all wallets for userId={}", userId);

        walletBlacklistService.blacklistUserWallets(userId);

        log.info("All wallets for userId={} successfully blacklisted", userId);
    }

    /**
     * Unblock all wallets for a specific user
     * Called by Admin or user-service
     */
    @PostMapping("/unblock")
    public void unblockWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Request received to unblock all wallets for userId={}", userId);

        walletBlacklistService.unblockUserWallets(userId);

        log.info("All wallets for userId={} successfully unblocked", userId);
    }

    /**
     * Blacklist a specific wallet
     * Called by user-service during admin-level wallet block
     */
    @PostMapping("/wallet/{walletId}")
    public void blacklistWallet(@PathVariable Long walletId) {
        log.info("Request received to blacklist walletId={}", walletId);

        walletBlacklistService.blacklistWallet(walletId);

        log.info("Wallet with id={} successfully blacklisted", walletId);
    }

    /**
     * Unblock a specific wallet
     * Called by user-service during admin-level wallet unblock
     */
    @PostMapping("/wallet/{walletId}/unblock")
    public void unblockWallet(@PathVariable Long walletId) {
        log.info("Request received to unblock walletId={}", walletId);

        walletBlacklistService.unblockWallet(walletId);

        log.info("Wallet with id={} successfully unblocked", walletId);
    }
}
