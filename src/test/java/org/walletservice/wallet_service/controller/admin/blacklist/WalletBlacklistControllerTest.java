package org.walletservice.wallet_service.controller.admin.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.controller.blacklist.WalletBlacklistController;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WalletBlacklistController}.
 */
class WalletBlacklistControllerTest {

    @Mock
    private WalletBlacklistService walletBlacklistService;

    @InjectMocks
    private WalletBlacklistController walletBlacklistController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -------------------------------------------------------------------------
    // 1️⃣ Blacklist all wallets for a user
    // -------------------------------------------------------------------------
    @Test
    void blacklistWalletsByUser_shouldCallServiceWithCorrectUserId() {
        Map<String, Long> request = Map.of("userId", 5L);

        walletBlacklistController.blacklistWalletsByUser(request);

        verify(walletBlacklistService).blacklistUserWallets(5L);
    }

    // -------------------------------------------------------------------------
    // 2️⃣ Unblock all wallets for a user
    // -------------------------------------------------------------------------
    @Test
    void unblockWalletsByUser_shouldCallServiceWithCorrectUserId() {
        Map<String, Long> request = Map.of("userId", 10L);

        walletBlacklistController.unblockWalletsByUser(request);

        verify(walletBlacklistService).unblockUserWallets(10L);
    }

    // -------------------------------------------------------------------------
    // 3️⃣ Blacklist specific wallet
    // -------------------------------------------------------------------------
    @Test
    void blacklistWallet_shouldCallServiceWithCorrectWalletId() {
        walletBlacklistController.blacklistWallet(123L);

        verify(walletBlacklistService).blacklistWallet(123L);
    }

    // -------------------------------------------------------------------------
    // 4️⃣ Unblock specific wallet
    // -------------------------------------------------------------------------
    @Test
    void unblockWallet_shouldCallServiceWithCorrectWalletId() {
        walletBlacklistController.unblockWallet(456L);

        verify(walletBlacklistService).unblockWallet(456L);
    }

    // -------------------------------------------------------------------------
    // 5️⃣ Missing userId in request → should call service with null
    // -------------------------------------------------------------------------
    @Test
    void blacklistWalletsByUser_missingUserId_shouldPassNullToService() {
        Map<String, Long> badRequest = new HashMap<>();

        walletBlacklistController.blacklistWalletsByUser(badRequest);

        verify(walletBlacklistService).blacklistUserWallets(null);
    }

    // -------------------------------------------------------------------------
    // 6️⃣ Missing userId in unblock request → should call service with null
    // -------------------------------------------------------------------------
    @Test
    void unblockWalletsByUser_missingUserId_shouldPassNullToService() {
        Map<String, Long> badRequest = new HashMap<>();

        walletBlacklistController.unblockWalletsByUser(badRequest);

        verify(walletBlacklistService).unblockUserWallets(null);
    }

    // -------------------------------------------------------------------------
    // 7️⃣ Service throws exception during blacklist → should propagate
    // -------------------------------------------------------------------------
    @Test
    void blacklistWallet_whenServiceThrows_shouldPropagateException() {
        doThrow(new IllegalArgumentException("Wallet not found"))
                .when(walletBlacklistService).blacklistWallet(999L);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                walletBlacklistController.blacklistWallet(999L)
        );

        assertEquals("Wallet not found", ex.getMessage());
        verify(walletBlacklistService).blacklistWallet(999L);
    }

    // -------------------------------------------------------------------------
    // 8️⃣ Service throws exception during unblock → should propagate
    // -------------------------------------------------------------------------
    @Test
    void unblockWallet_whenServiceThrows_shouldPropagateException() {
        doThrow(new IllegalStateException("Wallet already active"))
                .when(walletBlacklistService).unblockWallet(999L);

        Exception ex = assertThrows(IllegalStateException.class, () ->
                walletBlacklistController.unblockWallet(999L)
        );

        assertEquals("Wallet already active", ex.getMessage());
        verify(walletBlacklistService).unblockWallet(999L);
    }
}
