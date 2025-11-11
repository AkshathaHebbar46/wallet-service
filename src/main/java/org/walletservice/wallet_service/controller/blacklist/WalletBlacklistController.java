package org.walletservice.wallet_service.controller.blacklist;

import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

@RestController
@RequestMapping("/wallets/blacklist")
public class WalletBlacklistController {

    private final WalletBlacklistService walletBlacklistService;

    public WalletBlacklistController(WalletBlacklistService walletBlacklistService) {
        this.walletBlacklistService = walletBlacklistService;
    }

    /**
     * 🚫 Blacklist all wallets of a user
     * Called by user-service when admin blocks a user
     */
    @PostMapping("/user/{userId}")
    public void blacklistUserWallets(@PathVariable Long userId) {
        walletBlacklistService.blacklistUserWallets(userId);
    }

    /**
     * ♻️ Unblock all wallets of a user
     * Called by user-service when admin unblocks a user
     */
    @PostMapping("/user/{userId}/unblock")
    public void unblockUserWallets(@PathVariable Long userId) {
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
