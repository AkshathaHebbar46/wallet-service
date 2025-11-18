package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.WalletNotFoundException;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- 1. createWallet - self ----------------
    @Test
    void testCreateWalletSelf() {
        WalletRequestDTO request = new WalletRequestDTO(1L, 1000.0);
        WalletEntity saved = new WalletEntity(1L, 1000.0);
        saved.setId(10L);

        when(walletRepository.save(any())).thenReturn(saved);

        WalletResponseDTO response = walletService.createWallet(request, 1L, false);

        assertEquals(10L, response.getWalletId());
        assertEquals(1L, response.getUserId());
        assertEquals(1000.0, response.getBalance());
    }

    // ---------------- 2. createWallet - admin creating for others ----------------
    @Test
    void testCreateWalletAdmin() {
        WalletRequestDTO request = new WalletRequestDTO(2L, 2000.0);
        WalletEntity saved = new WalletEntity(2L, 2000.0);
        saved.setId(20L);

        when(walletRepository.save(any())).thenReturn(saved);

        WalletResponseDTO response = walletService.createWallet(request, 1L, true);

        assertEquals(20L, response.getWalletId());
        assertEquals(2L, response.getUserId());
    }

    // ---------------- 3. createWallet - non-admin cannot create for others ----------------
    @Test
    void testCreateWalletUnauthorized() {
        WalletRequestDTO request = new WalletRequestDTO(2L, 500.0);
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(request, 1L, false));
    }

    // ---------------- 4. getWalletDetails ----------------
    @Test
    void testGetWalletDetails() {
        WalletEntity wallet = new WalletEntity(1L, 1000.0);
        wallet.setId(10L);

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        WalletResponseDTO response = walletService.getWalletDetails(10L, 1L, false);

        assertEquals(10L, response.getWalletId());
        assertEquals(1000.0, response.getBalance());
    }

    // ---------------- 5. getWalletDetails - forbidden ----------------
    @Test
    void testGetWalletDetailsForbidden() {
        WalletEntity wallet = new WalletEntity(2L, 2000.0);
        wallet.setId(20L);

        when(walletRepository.findById(20L)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class, () -> walletService.getWalletDetails(20L, 1L, false));
    }

    // ---------------- 6. getBalance ----------------
    @Test
    void testGetBalance() {
        WalletEntity wallet = new WalletEntity(1L, 1500.0);
        wallet.setId(5L);

        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));

        Double balance = walletService.getBalance(5L, 1L, false);

        assertEquals(1500.0, balance);
    }

    // ---------------- 7. updateBalance ----------------
    @Test
    void testUpdateBalance() {
        WalletEntity wallet = new WalletEntity(1L, 1000.0);
        wallet.setId(10L);

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenReturn(wallet);

        WalletResponseDTO response = walletService.updateBalance(10L, 2000.0, 1L, false);

        assertEquals(2000.0, response.getBalance());
    }

    // ---------------- 8. getAllWallets ----------------
    @Test
    void testGetAllWallets() {
        WalletEntity w1 = new WalletEntity(1L, 1000.0);
        w1.setId(1L);
        WalletEntity w2 = new WalletEntity(2L, 2000.0);
        w2.setId(2L);

        when(walletRepository.findAll()).thenReturn(List.of(w1, w2));

        List<WalletResponseDTO> wallets = walletService.getAllWallets();

        assertEquals(2, wallets.size());
    }

    // ---------------- 9. getWalletsByUser ----------------
    @Test
    void testGetWalletsByUser() {
        WalletEntity w1 = new WalletEntity(1L, 1000.0);
        w1.setId(10L);

        when(walletRepository.findByUserId(1L)).thenReturn(List.of(w1));

        List<WalletResponseDTO> wallets = walletService.getWalletsByUser(1L);

        assertEquals(1, wallets.size());
        assertEquals(10L, wallets.get(0).getWalletId());
    }

    // ---------------- 10. deleteWalletsForUser ----------------
    @Test
    void testDeleteWalletsForUser() {
        WalletEntity w1 = new WalletEntity(1L, 1000.0);
        w1.setId(10L);
        WalletEntity w2 = new WalletEntity(1L, 500.0);
        w2.setId(11L);

        when(walletRepository.findByUserId(1L)).thenReturn(List.of(w1, w2));

        walletService.deleteWalletsForUser(1L);

        verify(walletRepository, times(1)).deleteAll(List.of(w1, w2));
    }

    // ---------------- 11. deleteWalletsForUser - no wallets ----------------
    @Test
    void testDeleteWalletsForUserNoWallets() {
        when(walletRepository.findByUserId(1L)).thenReturn(List.of());
        walletService.deleteWalletsForUser(1L);
        verify(walletRepository, never()).deleteAll(any());
    }

    // ---------------- 12. getWalletById - not found ----------------
    @Test
    void testGetWalletByIdNotFound() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletById(99L));
    }
}
