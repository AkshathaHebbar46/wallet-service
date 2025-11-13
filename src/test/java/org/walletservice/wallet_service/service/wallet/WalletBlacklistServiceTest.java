package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletBlacklistServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletBlacklistService walletBlacklistService;

    private WalletEntity activeWallet;
    private WalletEntity inactiveWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        activeWallet = new WalletEntity();
        activeWallet.setId(1L);
        activeWallet.setUserId(100L);
        activeWallet.setActive(true);

        inactiveWallet = new WalletEntity();
        inactiveWallet.setId(2L);
        inactiveWallet.setUserId(100L);
        inactiveWallet.setActive(false);
    }

    // ---------------------------------------------------
    // 1️⃣ Blacklist all wallets for a user
    // ---------------------------------------------------
    @Test
    @DisplayName("Should blacklist all wallets for given user")
    void testBlacklistUserWallets() {
        when(walletRepository.findByUserId(100L))
                .thenReturn(List.of(activeWallet, inactiveWallet));

        walletBlacklistService.blacklistUserWallets(100L);

        assertFalse(activeWallet.getActive());
        assertFalse(inactiveWallet.getActive());
        verify(walletRepository).findByUserId(100L);
    }

    // ---------------------------------------------------
    // 2️⃣ Unblock all wallets for a user
    // ---------------------------------------------------
    @Test
    @DisplayName("Should unblock all wallets for given user")
    void testUnblockUserWallets() {
        activeWallet.setActive(false);
        inactiveWallet.setActive(false);

        when(walletRepository.findByUserId(100L))
                .thenReturn(List.of(activeWallet, inactiveWallet));

        walletBlacklistService.unblockUserWallets(100L);

        assertTrue(activeWallet.getActive());
        assertTrue(inactiveWallet.getActive());
        verify(walletRepository).findByUserId(100L);
    }

    // ---------------------------------------------------
    // 3️⃣ Blacklist single wallet by ID
    // ---------------------------------------------------
    @Test
    @DisplayName("Should blacklist wallet by ID if found")
    void testBlacklistWallet() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(activeWallet));

        walletBlacklistService.blacklistWallet(1L);

        assertFalse(activeWallet.getActive());
        verify(walletRepository).findById(1L);
    }

    // ---------------------------------------------------
    // 4️⃣ Blacklist wallet by ID - not found
    // ---------------------------------------------------
    @Test
    @DisplayName("Should do nothing when blacklisting non-existent wallet")
    void testBlacklistWallet_NotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        walletBlacklistService.blacklistWallet(999L);

        // No exception, no interaction beyond findById
        verify(walletRepository).findById(999L);
        verifyNoMoreInteractions(walletRepository);
    }

    // ---------------------------------------------------
    // 5️⃣ Unblock single wallet by ID
    // ---------------------------------------------------
    @Test
    @DisplayName("Should unblock wallet by ID if found")
    void testUnblockWallet() {
        inactiveWallet.setActive(false);
        when(walletRepository.findById(2L)).thenReturn(Optional.of(inactiveWallet));

        walletBlacklistService.unblockWallet(2L);

        assertTrue(inactiveWallet.getActive());
        verify(walletRepository).findById(2L);
    }

    // ---------------------------------------------------
    // 6️⃣ Unblock wallet - not found
    // ---------------------------------------------------
    @Test
    @DisplayName("Should do nothing when unblocking non-existent wallet")
    void testUnblockWallet_NotFound() {
        when(walletRepository.findById(404L)).thenReturn(Optional.empty());

        walletBlacklistService.unblockWallet(404L);

        verify(walletRepository).findById(404L);
        verifyNoMoreInteractions(walletRepository);
    }

    // ---------------------------------------------------
    // 7️⃣ Handle null userId safely
    // ---------------------------------------------------
    @Test
    @DisplayName("Should handle null userId gracefully")
    void testBlacklistUserWallets_NullUserId() {
        when(walletRepository.findByUserId(null)).thenReturn(Collections.emptyList());

        walletBlacklistService.blacklistUserWallets(null);

        verify(walletRepository).findByUserId(null);
    }
}
