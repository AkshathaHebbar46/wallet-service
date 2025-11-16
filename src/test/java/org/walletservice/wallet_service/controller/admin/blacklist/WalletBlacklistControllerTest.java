package org.walletservice.wallet_service.controller.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

import java.util.Map;

import static org.mockito.Mockito.*;

class WalletBlacklistControllerTest {

    @Mock
    private WalletBlacklistService walletBlacklistService;

    @InjectMocks
    private WalletBlacklistController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ------------------- 1. Blacklist all wallets by user -------------------
    @Test
    void testBlacklistWalletsByUser() {
        Map<String, Long> request = Map.of("userId", 1L);
        controller.blacklistWalletsByUser(request);

        verify(walletBlacklistService, times(1)).blacklistUserWallets(1L);
    }

    // ------------------- 2. Unblock all wallets by user -------------------
    @Test
    void testUnblockWalletsByUser() {
        Map<String, Long> request = Map.of("userId", 2L);
        controller.unblockWalletsByUser(request);

        verify(walletBlacklistService, times(1)).unblockUserWallets(2L);
    }

    // ------------------- 3. Blacklist single wallet -------------------
    @Test
    void testBlacklistWallet() {
        Long walletId = 5L;
        controller.blacklistWallet(walletId);

        verify(walletBlacklistService, times(1)).blacklistWallet(walletId);
    }

    // ------------------- 4. Unblock single wallet -------------------
    @Test
    void testUnblockWallet() {
        Long walletId = 6L;
        controller.unblockWallet(walletId);

        verify(walletBlacklistService, times(1)).unblockWallet(walletId);
    }

    // ------------------- 5. Blacklist with missing userId -------------------
    @Test
    void testBlacklistWalletsByUserMissingId() {
        Map<String, Long> request = Map.of(); // empty map
        controller.blacklistWalletsByUser(request);

        // Verify method is called with null
        verify(walletBlacklistService, times(1)).blacklistUserWallets(null);
    }

    // ------------------- 6. Unblock with missing userId -------------------
    @Test
    void testUnblockWalletsByUserMissingId() {
        Map<String, Long> request = Map.of(); // empty map
        controller.unblockWalletsByUser(request);

        verify(walletBlacklistService, times(1)).unblockUserWallets(null);
    }

    // ------------------- 7. Blacklist multiple calls -------------------
    @Test
    void testBlacklistWalletMultipleCalls() {
        controller.blacklistWallet(10L);
        controller.blacklistWallet(11L);

        verify(walletBlacklistService, times(1)).blacklistWallet(10L);
        verify(walletBlacklistService, times(1)).blacklistWallet(11L);
    }

    // ------------------- 8. Unblock multiple calls -------------------
    @Test
    void testUnblockWalletMultipleCalls() {
        controller.unblockWallet(20L);
        controller.unblockWallet(21L);

        verify(walletBlacklistService, times(1)).unblockWallet(20L);
        verify(walletBlacklistService, times(1)).unblockWallet(21L);
    }

    // ------------------- 9. Blacklist and unblock user -------------------
    @Test
    void testBlacklistAndUnblockUser() {
        Map<String, Long> request = Map.of("userId", 99L);
        controller.blacklistWalletsByUser(request);
        controller.unblockWalletsByUser(request);

        verify(walletBlacklistService, times(1)).blacklistUserWallets(99L);
        verify(walletBlacklistService, times(1)).unblockUserWallets(99L);
    }

    // ------------------- 10. Blacklist and unblock wallet -------------------
    @Test
    void testBlacklistAndUnblockWallet() {
        controller.blacklistWallet(50L);
        controller.unblockWallet(50L);

        verify(walletBlacklistService, times(1)).blacklistWallet(50L);
        verify(walletBlacklistService, times(1)).unblockWallet(50L);
    }
}
