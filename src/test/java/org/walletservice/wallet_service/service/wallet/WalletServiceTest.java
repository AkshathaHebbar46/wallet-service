package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private AuthValidator authValidator;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        authValidator = mock(AuthValidator.class); // mock the validator
        walletService = new WalletService(walletRepository, authValidator);
    }

    // ---------------- CREATE WALLET ----------------

    @Test
    @DisplayName("✅ Should create wallet when user is admin")
    void testCreateWallet_AdminSuccess() {
        WalletRequestDTO request = new WalletRequestDTO();
        request.setUserId(10L);
        request.setBalance(500.0);

        WalletEntity savedEntity = new WalletEntity(10L, 500.0);
        savedEntity.setId(1L);

        when(walletRepository.save(any(WalletEntity.class))).thenReturn(savedEntity);

        WalletResponseDTO response = walletService.createWallet(request, 99L, true);

        assertNotNull(response);
        assertEquals(1L, response.getWalletId());
        assertEquals(10L, response.getUserId());
        assertEquals(500.0, response.getCurrentBalance());

        verify(walletRepository, times(1)).save(any(WalletEntity.class));
    }

    @Test
    @DisplayName("❌ Should throw error if non-admin tries to create wallet for another user")
    void testCreateWallet_NonAdminUnauthorized() {
        WalletRequestDTO request = new WalletRequestDTO();
        request.setUserId(2L);
        request.setBalance(100.0);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                walletService.createWallet(request, 1L, false)
        );
        assertEquals("You can only create a wallet for yourself", ex.getMessage());
        verify(walletRepository, never()).save(any());
    }

    // ---------------- GET WALLET DETAILS ----------------

    @Test
    @DisplayName("✅ Should return wallet details if admin")
    void testGetWalletDetails_Admin() {
        WalletEntity wallet = new WalletEntity(5L, 200.0);
        wallet.setId(1L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletResponseDTO response = walletService.getWalletDetails(1L, 999L, true);

        assertEquals(1L, response.getWalletId());
        assertEquals(5L, response.getUserId());
        assertEquals(200.0, response.getCurrentBalance());
    }

    @Test
    @DisplayName("❌ Should throw error if non-admin tries to access someone else’s wallet")
    void testGetWalletDetails_NonAdminUnauthorized() {
        WalletEntity wallet = new WalletEntity(5L, 200.0);
        wallet.setId(1L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                walletService.getWalletDetails(1L, 3L, false)
        );

        assertEquals("You cannot access this wallet", ex.getMessage());
    }

    @Test
    @DisplayName("❌ Should throw error if wallet not found")
    void testGetWalletDetails_NotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                walletService.getWalletDetails(1L, 2L, true)
        );

        assertTrue(ex.getMessage().contains("Wallet not found"));
    }

    // ---------------- GET BALANCE ----------------

    @Test
    @DisplayName("✅ Should return balance for admin")
    void testGetBalance_Admin() {
        WalletEntity wallet = new WalletEntity(7L, 800.0);
        wallet.setId(1L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        Double balance = walletService.getBalance(1L, 3L, true);
        assertEquals(800.0, balance);
    }

    @Test
    @DisplayName("❌ Should deny access if non-admin tries to get someone else's balance")
    void testGetBalance_NonAdminUnauthorized() {
        WalletEntity wallet = new WalletEntity(7L, 800.0);
        wallet.setId(1L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                walletService.getBalance(1L, 1L, false)
        );
        assertEquals("You do not have access to this wallet", ex.getMessage());
    }

    // ---------------- UPDATE BALANCE ----------------

    @Test
    @DisplayName("✅ Should update balance for admin")
    void testUpdateBalance_Admin() {
        WalletEntity wallet = new WalletEntity(10L, 100.0);
        wallet.setId(1L);

        WalletEntity updated = new WalletEntity(10L, 500.0);
        updated.setId(1L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(WalletEntity.class))).thenReturn(updated);

        WalletResponseDTO response = walletService.updateBalance(1L, 500.0, 2L, true);

        assertEquals(500.0, response.getCurrentBalance());
        verify(walletRepository).save(wallet);
    }

    @Test
    @DisplayName("❌ Should throw if non-admin updates someone else’s wallet")
    void testUpdateBalance_NonAdminUnauthorized() {
        WalletEntity wallet = new WalletEntity(9L, 100.0);
        wallet.setId(1L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                walletService.updateBalance(1L, 200.0, 2L, false)
        );
        assertEquals("You cannot update this wallet", ex.getMessage());
    }

    // ---------------- GET ALL WALLETS ----------------

    @Test
    @DisplayName("✅ Should return all wallets")
    void testGetAllWallets() {
        WalletEntity w1 = new WalletEntity(1L, 100.0);
        w1.setId(1L);
        WalletEntity w2 = new WalletEntity(2L, 200.0);
        w2.setId(2L);

        when(walletRepository.findAll()).thenReturn(List.of(w1, w2));

        List<WalletResponseDTO> result = walletService.getAllWallets();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(200.0, result.get(1).getCurrentBalance());
    }

    @Test
    @DisplayName("✅ Should return empty list if no wallets")
    void testGetAllWallets_Empty() {
        when(walletRepository.findAll()).thenReturn(List.of());
        List<WalletResponseDTO> result = walletService.getAllWallets();
        assertTrue(result.isEmpty());
    }

    // ---------------- GET WALLETS BY USER ----------------

    @Test
    @DisplayName("✅ Should return wallets for specific user")
    void testGetWalletsByUser() {
        WalletEntity w1 = new WalletEntity(1L, 100.0);
        w1.setId(1L);
        WalletEntity w2 = new WalletEntity(1L, 150.0);
        w2.setId(2L);

        when(walletRepository.findByUserId(1L)).thenReturn(List.of(w1, w2));

        List<WalletResponseDTO> result = walletService.getWalletsByUser(1L);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(2L, result.get(1).getWalletId());
    }
}
