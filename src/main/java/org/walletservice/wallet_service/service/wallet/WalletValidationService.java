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

/**
 * Service to validate wallet state, balance, daily limits, and handle freezing/unfreezing logic.
 */
@Service
public class WalletValidationService {

    private static final Logger log = LoggerFactory.getLogger(WalletValidationService.class);

    private static final double DAILY_LIMIT = 50000.0; // Daily transaction limit
    private static final long FREEZE_DURATION_MINUTES = 2; // Freeze duration after wallet reaches limit

    private final WalletRepository walletRepository;
    private final WalletFreezeService walletFreezeService;

    public WalletValidationService(WalletRepository walletRepository,
                                   WalletFreezeService walletFreezeService) {
        this.walletRepository = walletRepository;
        this.walletFreezeService = walletFreezeService;
    }

    /**
     * Validates the wallet state (frozen or active) and automatically unfreezes
     * it if the freeze duration has elapsed.
     */
    public void validateWalletState(WalletEntity wallet) {
        // Reset daily spent if it's a new day
        wallet.resetDailyIfNewDay();

        if (Boolean.TRUE.equals(wallet.getFrozen()) && wallet.getFrozenAt() != null) {
            long elapsed = Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toMinutes();
            if (elapsed >= FREEZE_DURATION_MINUTES) {
                walletFreezeService.unfreezeWallet(wallet);
                log.info("🧊 Wallet {} automatically unfrozen after {} minutes", wallet.getId(), FREEZE_DURATION_MINUTES);
            } else {
                long secondsLeft = FREEZE_DURATION_MINUTES * 60 -
                        Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toSeconds();
                log.warn("🚫 Wallet {} is currently frozen. {} seconds remaining", wallet.getId(), secondsLeft);
                throw new WalletFrozenException(
                        "Wallet is frozen. Try again in " + secondsLeft + " seconds.",
                        secondsLeft
                );
            }
        }
    }

    /**
     * Validates that the wallet has sufficient balance for a debit transaction.
     */
    public void validateBalance(WalletEntity wallet, double amount) {
        if (wallet.getBalance() < amount) {
            log.warn("❌ Wallet {} insufficient balance: required {}, available {}", wallet.getId(), amount, wallet.getBalance());
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    /**
     * Updates the daily spent amount and freezes the wallet if daily limit is reached.
     */
    public void updateDailySpentAndFreeze(WalletEntity wallet, double amount) {
        wallet.resetDailyIfNewDay();
        double newTotal = wallet.getDailySpent() + amount;

        if (newTotal > DAILY_LIMIT) {
            double available = DAILY_LIMIT - wallet.getDailySpent();
            log.warn("🚫 Wallet {} daily limit exceeded. Available amount ₹{}", wallet.getId(), available);
            throw new IllegalStateException("Daily limit exceeded. Available ₹" + available);
        }

        wallet.setDailySpent(newTotal);
        walletRepository.saveAndFlush(wallet); // persist daily spent immediately
        log.info("💵 Wallet {} daily spent updated: ₹{} / ₹{}", wallet.getId(), newTotal, DAILY_LIMIT);

        // Freeze wallet if daily limit is reached
        if (wallet.getDailySpent() >= DAILY_LIMIT) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        walletFreezeService.freezeWallet(wallet);
                        log.info("🧊 Wallet {} frozen after reaching daily limit", wallet.getId());
                    }
                });
            } else {
                log.warn("Transaction synchronization not active. Freezing wallet {} immediately.", wallet.getId());
                walletFreezeService.freezeWallet(wallet);
            }
        }
    }

    public double getRemainingDailyLimit(WalletEntity wallet) {
        wallet.resetDailyIfNewDay(); // ensure dailySpent is up-to-date
        return DAILY_LIMIT - wallet.getDailySpent();
    }

}
