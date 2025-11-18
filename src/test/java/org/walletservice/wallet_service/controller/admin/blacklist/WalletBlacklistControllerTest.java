package org.walletservice.wallet_service.controller.admin.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.controller.blacklist.WalletBlacklistController;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.service.wallet.WalletBlacklistService;

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

    @Test
    void testBlacklistWalletsByUser() {
        UserIdRequestDTO dto = new UserIdRequestDTO(1L);
        controller.blacklistWalletsByUser(dto);
        verify(walletBlacklistService, times(1)).blacklistUserWallets(1L);
    }

    @Test
    void testUnblockWalletsByUser() {
        UserIdRequestDTO dto = new UserIdRequestDTO(2L);
        controller.unblockWalletsByUser(dto);
        verify(walletBlacklistService, times(1)).unblockUserWallets(2L);
    }

    @Test
    void testBlacklistWallet() {
        controller.blacklistWallet(5L);
        verify(walletBlacklistService, times(1)).blacklistWallet(5L);
    }

    @Test
    void testUnblockWallet() {
        controller.unblockWallet(6L);
        verify(walletBlacklistService, times(1)).unblockWallet(6L);
    }

    @Test
    void testBlacklistWalletsByUser_Null() {
        UserIdRequestDTO dto = new UserIdRequestDTO(null);
        controller.blacklistWalletsByUser(dto);
        verify(walletBlacklistService, times(1)).blacklistUserWallets(null);
    }

    @Test
    void testUnblockWalletsByUser_Null() {
        UserIdRequestDTO dto = new UserIdRequestDTO(null);
        controller.unblockWalletsByUser(dto);
        verify(walletBlacklistService, times(1)).unblockUserWallets(null);
    }

    @Test
    void testBlacklistAndUnblockMultipleCalls() {
        controller.blacklistWallet(10L);
        controller.blacklistWallet(11L);
        controller.unblockWallet(20L);
        controller.unblockWallet(21L);

        verify(walletBlacklistService).blacklistWallet(10L);
        verify(walletBlacklistService).blacklistWallet(11L);
        verify(walletBlacklistService).unblockWallet(20L);
        verify(walletBlacklistService).unblockWallet(21L);
    }

    @Test
    void testBlacklistAndUnblockUser() {
        UserIdRequestDTO dto = new UserIdRequestDTO(99L);
        controller.blacklistWalletsByUser(dto);
        controller.unblockWalletsByUser(dto);

        verify(walletBlacklistService).blacklistUserWallets(99L);
        verify(walletBlacklistService).unblockUserWallets(99L);
    }

    @Test
    void testBlacklistAndUnblockSingleWallet() {
        controller.blacklistWallet(50L);
        controller.unblockWallet(50L);

        verify(walletBlacklistService).blacklistWallet(50L);
        verify(walletBlacklistService).unblockWallet(50L);
    }
}
