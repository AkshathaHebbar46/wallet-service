package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.WalletFrozenException;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletValidationServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletFreezeService walletFreezeService;

    @InjectMocks
    private WalletValidationService walletValidationService;

    private WalletEntity wallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(10L);
        wallet.setBalance(1000.0);
        wallet.setDailySpent(0.0);
        wallet.setFrozen(false);
    }

    // -------------------------------------------------------------------
    // validateBalance()
    // -------------------------------------------------------------------
    @Test
    @DisplayName("Should pass if wallet has sufficient balance")
    void testValidateBalance_Sufficient() {
        assertDoesNotThrow(() -> walletValidationService.validateBalance(wallet, 500.0));
    }

    @Test
    @DisplayName("Should throw if insufficient balance")
    void testValidateBalance_Insufficient() {
        wallet.setBalance(100.0);
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> walletValidationService.validateBalance(wallet, 200.0));
        assertEquals("Insufficient balance.", ex.getMessage());
    }

    // -------------------------------------------------------------------
    // validateAndTrackDailyLimit()
    // -------------------------------------------------------------------
    @Test
    @DisplayName("Should track daily spending below limit")
    void testValidateAndTrackDailyLimit_Valid() {
        wallet.setDailySpent(10000.0);

        walletValidationService.validateAndTrackDailyLimit(wallet, 2000.0);

        verify(walletRepository).saveAndFlush(wallet);
        assertEquals(12000.0, wallet.getDailySpent());
    }

    @Test
    @DisplayName("Should throw if daily limit exceeded")
    void testValidateAndTrackDailyLimit_ExceedsLimit() {
        wallet.setDailySpent(49000.0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> walletValidationService.validateAndTrackDailyLimit(wallet, 2000.0));

        assertTrue(ex.getMessage().contains("Daily limit exceeded"));
        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should freeze wallet automatically when daily limit reached")
    void testValidateAndTrackDailyLimit_TriggersFreeze() {
        wallet.setDailySpent(49000.0);

        // Simulate a transaction context
            TransactionSynchronizationManager.initSynchronization();

        walletValidationService.validateAndTrackDailyLimit(wallet, 1000.0);

        verify(walletRepository).saveAndFlush(wallet);

        // ✅ Manually trigger afterCommit to simulate transaction commit
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(walletFreezeService).freezeWallet(wallet);

        // Clear transaction synchronization context
        TransactionSynchronizationManager.clearSynchronization();
    }


    // -------------------------------------------------------------------
    // validateWalletState()
    // -------------------------------------------------------------------
    @Test
    @DisplayName("Should allow normal wallet (not frozen)")
    void testValidateWalletState_NormalWallet() {
        wallet.setFrozen(false);
        assertDoesNotThrow(() -> walletValidationService.validateWalletState(wallet));
    }

    @Test
    @DisplayName("Should unfreeze wallet if freeze duration expired")
    void testValidateWalletState_UnfreezeAfterTimeout() {
        wallet.setFrozen(true);
        wallet.setFrozenAt(LocalDateTime.now().minusMinutes(5)); // older than 2 minutes

        walletValidationService.validateWalletState(wallet);

        verify(walletFreezeService).unfreezeWallet(wallet);
    }

    @Test
    @DisplayName("Should throw if wallet is still frozen within freeze duration")
    void testValidateWalletState_FrozenTooSoon() {
        wallet.setFrozen(true);
        wallet.setFrozenAt(LocalDateTime.now().minusSeconds(30)); // less than 2 mins ago

        WalletFrozenException ex = assertThrows(WalletFrozenException.class,
                () -> walletValidationService.validateWalletState(wallet));

        assertTrue(ex.getMessage().contains("Wallet is frozen"));
        assertTrue(ex.getSecondsLeft() > 0);
        verify(walletFreezeService, never()).unfreezeWallet(any());
    }

    @Test
    @DisplayName("Should handle null frozenAt safely (skip)")
    void testValidateWalletState_FrozenNullFrozenAt() {
        wallet.setFrozen(true);
        wallet.setFrozenAt(null);

        assertDoesNotThrow(() -> walletValidationService.validateWalletState(wallet));
        verify(walletFreezeService, never()).unfreezeWallet(any());
    }
}
