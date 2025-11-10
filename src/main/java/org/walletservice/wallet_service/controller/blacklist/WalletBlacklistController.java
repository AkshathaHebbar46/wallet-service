package org.walletservice.wallet_service.controller.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.service.blacklist.WalletBlacklistService;

@RestController
@RequestMapping("/wallets/blacklist")
public class WalletBlacklistController {

    private static final Logger log = LoggerFactory.getLogger(WalletBlacklistController.class);

    private final WalletBlacklistService walletBlacklistService;

    public WalletBlacklistController(WalletBlacklistService walletBlacklistService) {
        this.walletBlacklistService = walletBlacklistService;
    }

    /** Blacklist a single wallet */
    @PostMapping("/wallet/{walletId}")
    public ResponseEntity<String> blacklistWallet(@PathVariable Long walletId) {
        walletBlacklistService.blacklistWallet(walletId);
        return ResponseEntity.ok("Wallet " + walletId + " has been blacklisted and set to inactive");
    }

    /** Blacklist all wallets of a user */
    @PostMapping("/user/{userId}")
    public ResponseEntity<String> blacklistUser(@PathVariable Long userId) {
        walletBlacklistService.blacklistUser(userId);
        return ResponseEntity.ok("All wallets of user " + userId + " have been blacklisted and set to inactive");
    }

    /** Blacklist wallet AND its user */
    @PostMapping("/wallet-and-user/{walletId}")
    public ResponseEntity<String> blacklistWalletAndUser(@PathVariable Long walletId) {
        walletBlacklistService.blacklistWalletAndUser(walletId);
        return ResponseEntity.ok("Wallet " + walletId + " and all wallets of the user have been blacklisted");
    }
}
