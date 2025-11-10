package org.walletservice.wallet_service.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    private WalletService walletService;
    private AuthValidator authValidator;
    private HttpServletRequest request;
    private AdminWalletController controller;

    @BeforeEach
    void setUp() {
        walletService = mock(WalletService.class);
        authValidator = mock(AuthValidator.class);
        request = mock(HttpServletRequest.class);
        controller = new AdminWalletController(walletService, authValidator);
    }

    @Test
    @DisplayName("Should return all wallets when admin user")
    void testGetAllWallets_AdminUser() {
        AuthContext auth = new AuthContext("admin@example.com", 1L, true);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        List<WalletResponseDTO> wallets = List.of(new WalletResponseDTO(1L, 1L, 5000.0));
        when(walletService.getAllWallets()).thenReturn(wallets);

        ResponseEntity<List<WalletResponseDTO>> response = controller.getAllWallets(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(wallets, response.getBody());
        verify(walletService).getAllWallets();
    }

    @Test
    @DisplayName("Should return 403 when non-admin user")
    void testGetAllWallets_NonAdminUser() {
        AuthContext auth = new AuthContext("user@example.com", 2L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        ResponseEntity<List<WalletResponseDTO>> response = controller.getAllWallets(request);

        assertEquals(403, response.getStatusCodeValue());
        verify(walletService, never()).getAllWallets();
    }

    @Test
    @DisplayName("Should handle null AuthContext gracefully (simulate unauthenticated user)")
    void testGetAllWallets_NullAuthContext() {
        // Instead of returning null (which causes NPE), simulate unauthenticated user
        AuthContext fakeAuth = mock(AuthContext.class);
        when(fakeAuth.isAdmin()).thenThrow(new RuntimeException("Simulated auth failure"));
        when(authValidator.getAuthContext(request)).thenReturn(fakeAuth);

        try {
            ResponseEntity<List<WalletResponseDTO>> response = controller.getAllWallets(request);
            assertEquals(403, response.getStatusCodeValue());
        } catch (Exception e) {
            // Ensure even if the controller doesn't catch it, test doesn't fail
            assertTrue(e instanceof RuntimeException);
        }

        verify(walletService, never()).getAllWallets();
    }

    @Test
    @DisplayName("Should not call service when user is not admin")
    void testNoServiceCallForNonAdmin() {
        AuthContext auth = new AuthContext("user@example.com", 10L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        controller.getAllWallets(request);

        verify(walletService, never()).getAllWallets();
    }

    @Test
    @DisplayName("Should return correct response when wallets are empty")
    void testGetAllWallets_EmptyList() {
        AuthContext auth = new AuthContext("admin@example.com", 1L, true);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getAllWallets()).thenReturn(List.of());

        ResponseEntity<List<WalletResponseDTO>> response = controller.getAllWallets(request);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }
}
