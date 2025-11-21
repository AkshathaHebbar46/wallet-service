package org.walletservice.wallet_service.repository.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class WalletRepositoryTest {

    @Mock
    private WalletRepository walletRepository;

    private WalletEntity wallet1;
    private WalletEntity wallet2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        wallet1 = new WalletEntity();
        wallet1.setId(1L);
        wallet1.setUserId(10L);
        wallet1.setBalance(1000.0);
        wallet1.setCreatedAt(LocalDateTime.now().minusDays(3));

        wallet2 = new WalletEntity();
        wallet2.setId(2L);
        wallet2.setUserId(20L);
        wallet2.setBalance(200.0);
        wallet2.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should find wallets by userId")
    void testFindByUserId() {
        when(walletRepository.findByUserId(10L)).thenReturn(List.of(wallet1));

        List<WalletEntity> result = walletRepository.findByUserId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());
        verify(walletRepository).findByUserId(10L);
    }

    @Test
    @DisplayName("Should return true if wallet exists for userId")
    void testExistsByUserId_True() {
        when(walletRepository.existsByUserId(10L)).thenReturn(true);

        boolean exists = walletRepository.existsByUserId(10L);

        assertTrue(exists);
        verify(walletRepository).existsByUserId(10L);
    }

    @Test
    @DisplayName("Should return false if wallet does not exist for userId")
    void testExistsByUserId_False() {
        when(walletRepository.existsByUserId(999L)).thenReturn(false);

        boolean exists = walletRepository.existsByUserId(999L);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should find wallets created before given date")
    void testFindByCreatedAtBefore() {
        LocalDateTime date = LocalDateTime.now().minusDays(2);
        when(walletRepository.findByCreatedAtBefore(date))
                .thenReturn(List.of(wallet1));

        List<WalletEntity> result = walletRepository.findByCreatedAtBefore(date);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getCreatedAt().isBefore(date));
    }

    @Test
    @DisplayName("Should find wallets above average balance")
    void testFindWalletsAboveAverageBalance() {
        when(walletRepository.findWalletsAboveAverageBalance())
                .thenReturn(List.of(wallet1));

        List<WalletEntity> result = walletRepository.findWalletsAboveAverageBalance();

        assertEquals(1, result.size());
        assertEquals(1000.0, result.get(0).getBalance());
    }

    @Test
    @DisplayName("Should find wallet by ID for update")
    void testFindByIdForUpdate() {
        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(wallet1));

        Optional<WalletEntity> result = walletRepository.findByIdForUpdate(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Should return empty Optional if wallet not found for update")
    void testFindByIdForUpdate_NotFound() {
        when(walletRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        Optional<WalletEntity> result = walletRepository.findByIdForUpdate(99L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle empty results gracefully")
    void testEmptyResults() {
        when(walletRepository.findByUserId(111L)).thenReturn(Collections.emptyList());

        List<WalletEntity> result = walletRepository.findByUserId(111L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should not throw when checking existence for null userId")
    void testExistsByUserId_Null() {
        when(walletRepository.existsByUserId(null)).thenReturn(false);

        assertFalse(walletRepository.existsByUserId(null));
        verify(walletRepository).existsByUserId(null);
    }
}
