package org.walletservice.wallet_service.service.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;

@Service
public class WalletBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(WalletBlacklistService.class);

    private final WalletRepository walletRepository;

    public WalletBlacklistService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Blacklist a single wallet → sets inactive and logs.
     */
    @Transactional
    public void blacklistWallet(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        wallet.setActive(false);
        walletRepository.save(wallet);

        log.info("Wallet {} has been blacklisted and set to inactive", walletId);
    }

    /**
     * Blacklist all wallets of a user → sets inactive
     */
    @Transactional
    public void blacklistUser(Long userId) {
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);

        if (wallets.isEmpty()) {
            log.warn("No wallets found for user {}", userId);
            return;
        }

        for (WalletEntity wallet : wallets) {
            wallet.setActive(false);
        }

        walletRepository.saveAll(wallets);
        log.info("All wallets of user {} have been blacklisted and set to inactive", userId);
    }

    /**
     * Blacklist a wallet and automatically blacklist the user as well
     */
    @Transactional
    public void blacklistWalletAndUser(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        Long userId = wallet.getUserId();
        blacklistWallet(walletId);   // sets this wallet inactive
        blacklistUser(userId);       // sets all user's wallets inactive

        log.info("Wallet {} and all wallets of user {} have been blacklisted", walletId, userId);
    }
}
