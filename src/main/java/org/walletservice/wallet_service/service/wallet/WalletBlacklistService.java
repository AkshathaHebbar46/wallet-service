package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;

/**
 * Service to handle blacklisting/unblocking of wallets.
 * Can operate on individual wallets or all wallets of a user.
 */
@Service
public class WalletBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(WalletBlacklistService.class);

    private final WalletRepository walletRepository;

    public WalletBlacklistService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Blacklist (deactivate) all wallets for a given user.
     * @param userId ID of the user whose wallets will be deactivated.
     */
    @Transactional
    public void blacklistUserWallets(Long userId) {
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        wallets.forEach(w -> w.setActive(false));
        log.info("Blacklisted {} wallets for userId={}", wallets.size(), userId);
    }

    /**
     * Unblock (activate) all wallets for a given user.
     * @param userId ID of the user whose wallets will be activated.
     */
    @Transactional
    public void unblockUserWallets(Long userId) {
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        wallets.forEach(w -> w.setActive(true));
        log.info("Unblocked {} wallets for userId={}", wallets.size(), userId);
    }

    /**
     * Blacklist (deactivate) a specific wallet.
     * @param walletId ID of the wallet to deactivate.
     */
    @Transactional
    public void blacklistWallet(Long walletId) {
        walletRepository.findById(walletId).ifPresentOrElse(
                w -> {
                    w.setActive(false);
                    log.info("Blacklisted walletId={}", walletId);
                },
                () -> log.warn("Attempted to blacklist non-existent walletId={}", walletId)
        );
    }

    /**
     * Unblock (activate) a specific wallet.
     * @param walletId ID of the wallet to activate.
     */
    @Transactional
    public void unblockWallet(Long walletId) {
        walletRepository.findById(walletId).ifPresentOrElse(
                w -> {
                    w.setActive(true);
                    log.info("Unblocked walletId={}", walletId);
                },
                () -> log.warn("Attempted to unblock non-existent walletId={}", walletId)
        );
    }
}
