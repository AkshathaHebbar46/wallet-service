package org.walletservice.wallet_service.controller.wallet;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private WalletController walletController;

    private AuthContext userAuth;
    private AuthContext adminAuth;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userAuth = new AuthContext("token-user", 1L, false);
        adminAuth = new AuthContext("token-admin", 99L, true);
    }

    // ---------------------------------------------------
    // 1️⃣ CREATE WALLET
    // ---------------------------------------------------
    @Test
    void createWallet_shouldReturn201AndWalletResponse() {
        WalletRequestDTO req = new WalletRequestDTO();
        WalletResponseDTO resp = new WalletResponseDTO(1L, 1L, 100.0);

        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.createWallet(req, 1L, false)).thenReturn(resp);

        ResponseEntity<WalletResponseDTO> result = walletController.createWallet(req, httpRequest);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());
        verify(walletService).createWallet(req, 1L, false);
    }

    // ---------------------------------------------------
    // 2️⃣ CREATE WALLET - Admin creating for someone else
    // ---------------------------------------------------
    @Test
    void createWallet_asAdmin_shouldCallServiceWithAdminFlag() {
        WalletRequestDTO req = new WalletRequestDTO();
        WalletResponseDTO resp = new WalletResponseDTO(10L, 50L, 200.0);

        when(authValidator.getAuthContext(httpRequest)).thenReturn(adminAuth);
        when(walletService.createWallet(req, 99L, true)).thenReturn(resp);

        ResponseEntity<WalletResponseDTO> result = walletController.createWallet(req, httpRequest);

        assertEquals(201, result.getStatusCodeValue());
        verify(walletService).createWallet(req, 99L, true);
    }

    // ---------------------------------------------------
    // 3️⃣ GET WALLET DETAILS
    // ---------------------------------------------------
    @Test
    void getWalletDetails_shouldReturnWallet() {
        WalletResponseDTO wallet = new WalletResponseDTO(1L, 1L, 500.0);
        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.getWalletDetails(1L, 1L, false)).thenReturn(wallet);

        ResponseEntity<WalletResponseDTO> result = walletController.getWalletDetails(1L, httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(wallet, result.getBody());
        verify(walletService).getWalletDetails(1L, 1L, false);
    }

    // ---------------------------------------------------
    // 4️⃣ GET BALANCE
    // ---------------------------------------------------
    @Test
    void getBalance_shouldReturnBalance() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.getBalance(1L, 1L, false)).thenReturn(150.0);

        ResponseEntity<Double> result = walletController.getBalance(1L, httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(150.0, result.getBody());
        verify(walletService).getBalance(1L, 1L, false);
    }

    // ---------------------------------------------------
    // 5️⃣ UPDATE BALANCE
    // ---------------------------------------------------
    @Test
    void updateBalance_shouldReturnUpdatedWallet() {
        WalletRequestDTO req = new WalletRequestDTO();
        req.setBalance(200.0);
        WalletResponseDTO updated = new WalletResponseDTO(1L, 1L, 200.0);

        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.updateBalance(1L, 200.0, 1L, false)).thenReturn(updated);

        ResponseEntity<WalletResponseDTO> result = walletController.updateBalance(1L, req, httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(updated, result.getBody());
        verify(walletService).updateBalance(1L, 200.0, 1L, false);
    }

    // ---------------------------------------------------
    // 6️⃣ GET ALL WALLETS (ADMIN)
    // ---------------------------------------------------
    @Test
    void getWalletsForCurrentUser_shouldReturnAllWalletsForAdmin() {
        List<WalletResponseDTO> allWallets = List.of(
                new WalletResponseDTO(1L, 1L, 100.0),
                new WalletResponseDTO(2L, 2L, 250.0)
        );

        when(authValidator.getAuthContext(httpRequest)).thenReturn(adminAuth);
        when(walletService.getAllWallets()).thenReturn(allWallets);

        ResponseEntity<List<WalletResponseDTO>> result = walletController.getWalletsForCurrentUser(httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(2, result.getBody().size());
        verify(walletService).getAllWallets();
    }

    // ---------------------------------------------------
    // 7️⃣ GET ALL WALLETS (USER)
    // ---------------------------------------------------
    @Test
    void getWalletsForCurrentUser_shouldReturnUserWallets() {
        List<WalletResponseDTO> wallets = List.of(
                new WalletResponseDTO(1L, 1L, 100.0)
        );

        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.getWalletsByUser(1L)).thenReturn(wallets);

        ResponseEntity<List<WalletResponseDTO>> result = walletController.getWalletsForCurrentUser(httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(wallets, result.getBody());
        verify(walletService).getWalletsByUser(1L);
    }

    // ---------------------------------------------------
    // 8️⃣ GET ALL WALLETS (ADMIN) - No wallets
    // ---------------------------------------------------
    @Test
    void getWalletsForCurrentUser_adminWithNoWallets_shouldReturnEmptyList() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(adminAuth);
        when(walletService.getAllWallets()).thenReturn(Collections.emptyList());

        ResponseEntity<List<WalletResponseDTO>> result = walletController.getWalletsForCurrentUser(httpRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody().isEmpty());
        verify(walletService).getAllWallets();
    }

    // ---------------------------------------------------
    // 9️⃣ GET WALLET DETAILS - Non-existent wallet
    // ---------------------------------------------------
    @Test
    void getWalletDetails_whenWalletNotFound_shouldThrowException() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.getWalletDetails(99L, 1L, false))
                .thenThrow(new IllegalArgumentException("Wallet not found"));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> walletController.getWalletDetails(99L, httpRequest));

        assertEquals("Wallet not found", ex.getMessage());
        verify(walletService).getWalletDetails(99L, 1L, false);
    }

    // ---------------------------------------------------
    // 🔟 UPDATE BALANCE - Invalid amount
    // ---------------------------------------------------
    @Test
    void updateBalance_withNegativeBalance_shouldThrowException() {
        WalletRequestDTO req = new WalletRequestDTO();
        req.setBalance(-50.0);

        when(authValidator.getAuthContext(httpRequest)).thenReturn(userAuth);
        when(walletService.updateBalance(1L, -50.0, 1L, false))
                .thenThrow(new IllegalArgumentException("Invalid balance amount"));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> walletController.updateBalance(1L, req, httpRequest));

        assertEquals("Invalid balance amount", ex.getMessage());
        verify(walletService).updateBalance(1L, -50.0, 1L, false);
    }
}
