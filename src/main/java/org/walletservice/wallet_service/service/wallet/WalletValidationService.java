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

    private static final double DAILY_LIMIT = 50000.0; // daily transaction limit
    private static final long FREEZE_DURATION_MINUTES = 2; // freeze duration

    private final WalletRepository walletRepository;
    private final WalletFreezeService walletFreezeService;

    public WalletValidationService(WalletRepository walletRepository,
                                   WalletFreezeService walletFreezeService) {
        this.walletRepository = walletRepository;
        this.walletFreezeService = walletFreezeService;
    }

    /**
     * Validates wallet state (frozen or not) and unfreezes if freeze duration passed.
     */
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
                throw new WalletFrozenException(
                        "🚫 Wallet is frozen. Try again in " + secondsLeft + " seconds.",
                        secondsLeft
                );
            }
        }
    }

    /**
     * Validates if wallet has sufficient balance.
     */
    public void validateBalance(WalletEntity wallet, double amount) {
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    /**
     * Updates daily spent amount and freezes wallet if daily limit reached.
     */
    public void updateDailySpentAndFreeze(WalletEntity wallet, double amount) {
        wallet.resetDailyIfNewDay();
        double newTotal = wallet.getDailySpent() + amount;

        if (newTotal > DAILY_LIMIT) {
            double available = DAILY_LIMIT - wallet.getDailySpent();
            throw new IllegalStateException("🚫 Daily limit exceeded. Available ₹" + available);
        }

        wallet.setDailySpent(newTotal);
        walletRepository.saveAndFlush(wallet); // persist daily spent

        // If daily limit exactly reached, freeze wallet after commit
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
                log.warn("Transaction synchronization not active. Freezing wallet immediately.");
                walletFreezeService.freezeWallet(wallet);
            }
        }
    }

    /**
     * Validates if wallet exists and is active.
     */
    public void validateWalletActive(WalletEntity wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found");
        }
        if (Boolean.FALSE.equals(wallet.getActive())) {
            throw new IllegalStateException("Wallet is inactive");
        }
    }
}
