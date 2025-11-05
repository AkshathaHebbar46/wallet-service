package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.WalletFrozenException;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class WalletValidationService {

    private static final Logger log = LoggerFactory.getLogger(WalletValidationService.class);

    private static final double DAILY_LIMIT = 50000.0;
    private static final long FREEZE_DURATION_MINUTES = 2;

    private final WalletRepository walletRepository;
    private final WalletFreezeService walletFreezeService;

    public WalletValidationService(WalletRepository walletRepository,
                                   WalletFreezeService walletFreezeService) {
        this.walletRepository = walletRepository;
        this.walletFreezeService = walletFreezeService;
    }

    public void validateWalletState(WalletEntity wallet) {
        wallet.resetDailyIfNewDay();

        if (Boolean.TRUE.equals(wallet.getFrozen()) && wallet.getFrozenAt() != null) {
            long elapsed = Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toMinutes();

            if (elapsed >= FREEZE_DURATION_MINUTES) {
                walletFreezeService.unfreezeWallet(wallet);
                log.info("🧊 Wallet {} unfrozen after {} minutes", wallet.getId(), FREEZE_DURATION_MINUTES);
            } else {
                long secondsLeft = FREEZE_DURATION_MINUTES * 60 -
                        Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toSeconds();
                throw new WalletFrozenException("🚫 Wallet is frozen. Try again in " + secondsLeft + " seconds.", secondsLeft);
            }
        }
    }

    public void validateBalance(WalletEntity wallet, double amount) {
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    public void validateAndTrackDailyLimit(WalletEntity wallet, double amount) {
        double newTotal = wallet.getDailySpent() + amount;

        if (newTotal > DAILY_LIMIT) {
            double available = DAILY_LIMIT - wallet.getDailySpent();
            throw new IllegalStateException("🚫 Daily limit exceeded. Available ₹" + available);
        }

        wallet.setDailySpent(newTotal);
        walletRepository.saveAndFlush(wallet);

        if (wallet.getDailySpent() >= DAILY_LIMIT) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    walletFreezeService.freezeWallet(wallet);
                }
            });
        }
    }
}
