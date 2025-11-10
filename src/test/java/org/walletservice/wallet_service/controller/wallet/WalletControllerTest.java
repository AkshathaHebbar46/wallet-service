package org.walletservice.wallet_service.controller.wallet;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WalletController walletController;

    private AuthContext auth;

    @BeforeEach
    void setUp() {
        auth = new AuthContext("user@example.com", 1L, false);
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should create wallet successfully")
    void testCreateWallet_Success() {
        WalletRequestDTO walletRequest = new WalletRequestDTO(1L, 500.0);
        WalletResponseDTO responseDTO = new WalletResponseDTO(1L, 1L, 500.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.createWallet(walletRequest, auth.getUserId(), auth.isAdmin())).thenReturn(responseDTO);

        ResponseEntity<WalletResponseDTO> response = walletController.createWallet(walletRequest, request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(responseDTO, response.getBody());
        verify(walletService).createWallet(walletRequest, auth.getUserId(), auth.isAdmin());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should get wallet details successfully")
    void testGetWalletDetails_Success() {
        Long walletId = 10L;
        WalletResponseDTO responseDTO = new WalletResponseDTO(walletId, 1L, 200.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getWalletDetails(walletId, auth.getUserId(), auth.isAdmin())).thenReturn(responseDTO);

        ResponseEntity<WalletResponseDTO> response = walletController.getWalletDetails(walletId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(responseDTO, response.getBody());
        verify(walletService).getWalletDetails(walletId, auth.getUserId(), auth.isAdmin());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should return wallet balance successfully")
    void testGetBalance_Success() {
        Long walletId = 5L;
        Double expectedBalance = 300.0;

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getBalance(walletId, auth.getUserId(), auth.isAdmin())).thenReturn(expectedBalance);

        ResponseEntity<Double> response = walletController.getBalance(walletId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedBalance, response.getBody());
        verify(walletService).getBalance(walletId, auth.getUserId(), auth.isAdmin());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should update balance successfully")
    void testUpdateBalance_Success() {
        Long walletId = 1L;
        WalletRequestDTO walletRequest = new WalletRequestDTO(1L, 800.0);
        WalletResponseDTO updatedWallet = new WalletResponseDTO(walletId, 1L, 800.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.updateBalance(walletId, walletRequest.getBalance(), auth.getUserId(), auth.isAdmin()))
                .thenReturn(updatedWallet);

        ResponseEntity<WalletResponseDTO> response =
                walletController.updateBalance(walletId, walletRequest, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(updatedWallet.getCurrentBalance(), response.getBody().getCurrentBalance());
        verify(walletService).updateBalance(walletId, walletRequest.getBalance(), auth.getUserId(), auth.isAdmin());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should get wallets by user successfully when authorized")
    void testGetWalletsByUser_Success() {
        Long userId = 1L;
        WalletEntity wallet1 = new WalletEntity();
        wallet1.setId(1L);
        wallet1.setUserId(userId);
        wallet1.setBalance(100.0);

        WalletEntity wallet2 = new WalletEntity();
        wallet2.setId(2L);
        wallet2.setUserId(userId);
        wallet2.setBalance(200.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorized(auth, userId)).thenReturn(true);
        when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet1, wallet2));

        ResponseEntity<List<WalletResponseDTO>> response =
                walletController.getWalletsByUser(userId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(walletRepository).findByUserId(userId);
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should return 403 for unauthorized access when fetching wallets by user")
    void testGetWalletsByUser_Unauthorized() {
        Long userId = 2L;

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorized(auth, userId)).thenReturn(false);

        ResponseEntity<List<WalletResponseDTO>> response =
                walletController.getWalletsByUser(userId, request);

        assertEquals(403, response.getStatusCodeValue());
        assertNull(response.getBody());
        verify(walletRepository, never()).findByUserId(any());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should handle zero balance gracefully")
    void testCreateWallet_ZeroBalance() {
        WalletRequestDTO walletRequest = new WalletRequestDTO(1L, 0.0);
        WalletResponseDTO responseDTO = new WalletResponseDTO(1L, 1L, 0.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.createWallet(walletRequest, auth.getUserId(), auth.isAdmin()))
                .thenReturn(responseDTO);

        ResponseEntity<WalletResponseDTO> response = walletController.createWallet(walletRequest, request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(0.0, response.getBody().getCurrentBalance());
    }

    // --------------------------------------------------------------------
    @Test
    @DisplayName("Should handle negative balance gracefully")
    void testUpdateBalance_NegativeBalance() {
        Long walletId = 1L;
        WalletRequestDTO walletRequest = new WalletRequestDTO(1L, -50.0);
        WalletResponseDTO updatedWallet = new WalletResponseDTO(walletId, 1L, -50.0);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.updateBalance(walletId, -50.0, auth.getUserId(), auth.isAdmin()))
                .thenReturn(updatedWallet);

        ResponseEntity<WalletResponseDTO> response =
                walletController.updateBalance(walletId, walletRequest, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(-50.0, response.getBody().getCurrentBalance());
    }
}
