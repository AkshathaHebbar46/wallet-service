package org.walletservice.wallet_service.controller.blacklist;

import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

import java.util.Map;

@RestController
@RequestMapping("/admin/wallets/blacklist")
public class WalletBlacklistController {

    private final WalletBlacklistService walletBlacklistService;

    public WalletBlacklistController(WalletBlacklistService walletBlacklistService) {
        this.walletBlacklistService = walletBlacklistService;
    }

    @PostMapping
    public void blacklistWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        walletBlacklistService.blacklistUserWallets(userId);
    }

    @PostMapping("/unblock")
    public void unblockWalletsByUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        walletBlacklistService.unblockUserWallets(userId);
    }

    /**
     * 🚫 Blacklist a specific wallet
     * Called by user-service for admin-level wallet block
     */
    @PostMapping("/wallet/{walletId}")
    public void blacklistWallet(@PathVariable Long walletId) {
        walletBlacklistService.blacklistWallet(walletId);
    }

    /**
     * ♻️ Unblock a specific wallet
     * Called by user-service for admin-level wallet unblock
     */
    @PostMapping("/wallet/{walletId}/unblock")
    public void unblockWallet(@PathVariable Long walletId) {
        walletBlacklistService.unblockWallet(walletId);
    }
}
