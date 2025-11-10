package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletFreezeServiceTest {

    private WalletRepository walletRepository;
    private WalletFreezeService walletFreezeService;

    private WalletEntity wallet;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        walletFreezeService = new WalletFreezeService(walletRepository);

        wallet = new WalletEntity(10L, 100.0);
        wallet.setId(1L);
    }

    // ------------------------------------------------------------------
    // ✅ freezeWallet() Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should freeze wallet successfully")
    void testFreezeWallet_Success() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.saveAndFlush(any(WalletEntity.class))).thenReturn(wallet);

        walletFreezeService.freezeWallet(wallet);

        assertTrue(wallet.getFrozen());
        assertNotNull(wallet.getFrozenAt());
        verify(walletRepository).saveAndFlush(wallet);
    }

    @Test
    @DisplayName("Should retry once on OptimisticLockingFailureException and succeed")
    void testFreezeWallet_RetryOnOptimisticLock() {
        // Arrange
        WalletEntity freshWallet = new WalletEntity();
        freshWallet.setId(1L);

        when(walletRepository.findById(wallet.getId()))
                .thenReturn(Optional.of(wallet))
                .thenReturn(Optional.of(freshWallet));

        // First save fails, second succeeds
        doThrow(new OptimisticLockingFailureException("First attempt failed"))
                .doReturn(wallet)
                .when(walletRepository)
                .saveAndFlush(any(WalletEntity.class));

        // Act
        walletFreezeService.freezeWallet(wallet);

        // Assert - verify retry sequence
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).findById(wallet.getId());   // first attempt
        inOrder.verify(walletRepository).saveAndFlush(any(WalletEntity.class)); // failed save
        inOrder.verify(walletRepository).findById(wallet.getId());   // retry
        inOrder.verify(walletRepository).saveAndFlush(any(WalletEntity.class)); // retry success

        verify(walletRepository, times(2)).findById(wallet.getId());
        verify(walletRepository, times(2)).saveAndFlush(any(WalletEntity.class));

        // Ensure the saved wallet had frozen=true at least once
        verify(walletRepository, atLeastOnce())
                .saveAndFlush(argThat(w -> Boolean.TRUE.equals(w.getFrozen())));
    }

@Test
    @DisplayName("Should handle exception gracefully after retry fails")
    void testFreezeWallet_FailsEvenAfterRetry() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        // Both attempts throw OptimisticLockingFailureException
        doThrow(new OptimisticLockingFailureException("fail1"))
                .when(walletRepository).saveAndFlush(any(WalletEntity.class));

        // Should not throw anything out
        assertDoesNotThrow(() -> walletFreezeService.freezeWallet(wallet));

        verify(walletRepository, atLeastOnce()).saveAndFlush(any(WalletEntity.class));
    }

    @Test
    @DisplayName("Should handle unexpected exception gracefully")
    void testFreezeWallet_UnexpectedException() {
        when(walletRepository.findById(wallet.getId()))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> walletFreezeService.freezeWallet(wallet));
    }

    // ------------------------------------------------------------------
    // ✅ unfreezeWallet() Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should unfreeze wallet successfully")
    void testUnfreezeWallet_Success() {
        wallet.setFrozen(true);
        wallet.setFrozenAt(LocalDateTime.now());
        wallet.setDailySpent(50000.0);

        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.saveAndFlush(any(WalletEntity.class))).thenReturn(wallet);

        walletFreezeService.unfreezeWallet(wallet);

        assertFalse(wallet.getFrozen());
        assertNull(wallet.getFrozenAt());
        assertEquals(0.0, wallet.getDailySpent());
        verify(walletRepository).saveAndFlush(wallet);
    }

    @Test
    @DisplayName("Should handle OptimisticLockingFailureException gracefully on unfreeze")
    void testUnfreezeWallet_OptimisticLockIgnored() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        doThrow(new OptimisticLockingFailureException("fail"))
                .when(walletRepository).saveAndFlush(any(WalletEntity.class));

        assertDoesNotThrow(() -> walletFreezeService.unfreezeWallet(wallet));
        verify(walletRepository).saveAndFlush(wallet);
    }

    @Test
    @DisplayName("Should handle unexpected exception during unfreeze")
    void testUnfreezeWallet_UnexpectedError() {
        when(walletRepository.findById(wallet.getId()))
                .thenThrow(new RuntimeException("DB failure"));

        assertDoesNotThrow(() -> walletFreezeService.unfreezeWallet(wallet));
    }
}
