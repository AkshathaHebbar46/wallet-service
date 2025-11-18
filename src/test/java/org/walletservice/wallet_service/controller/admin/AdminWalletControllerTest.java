package org.walletservice.wallet_service.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletFreezeService;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;
import org.walletservice.wallet_service.service.wallet.WalletService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminWalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private WalletFreezeService walletFreezeService;

    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminWalletController adminWalletController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- 1. getAllWalletsForUser - success ----------------
    @Test
    void testGetAllWalletsForUserSuccess() {
        UserIdRequestDTO dto = new UserIdRequestDTO(1L);
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        WalletResponseDTO wallet = new WalletResponseDTO(1L, 1L, 1000.0); // Correct constructor
        when(walletService.getWalletsByUser(1L)).thenReturn(List.of(wallet));

        ResponseEntity<List<WalletResponseDTO>> response = adminWalletController.getAllWalletsForUser(dto, request);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(wallet.getWalletId(), response.getBody().get(0).getWalletId());
    }

    // ---------------- 2. getAllWalletsForUser - no content ----------------
    @Test
    void testGetAllWalletsForUserNoContent() {
        UserIdRequestDTO dto = new UserIdRequestDTO(1L);
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        when(walletService.getWalletsByUser(1L)).thenReturn(List.of());

        ResponseEntity<List<WalletResponseDTO>> response = adminWalletController.getAllWalletsForUser(dto, request);
        assertEquals(204, response.getStatusCodeValue());
    }

    // ---------------- 3. getAllWalletsForUser - unauthorized ----------------
    @Test
    void testGetAllWalletsForUserUnauthorized() {
        UserIdRequestDTO dto = new UserIdRequestDTO(1L);
        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        ResponseEntity<List<WalletResponseDTO>> response = adminWalletController.getAllWalletsForUser(dto, request);
        assertEquals(403, response.getStatusCodeValue());
    }

    // ---------------- 4. freezeWallet - success ----------------
    @Test
    void testFreezeWalletSuccess() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getWalletById(walletId)).thenReturn(wallet);

        ResponseEntity<String> response = adminWalletController.freezeWallet(walletId, request);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("frozen successfully"));
        verify(walletFreezeService).freezeWallet(wallet);
    }

    // ---------------- 5. freezeWallet - unauthorized ----------------
    @Test
    void testFreezeWalletUnauthorized() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        assertThrows(UnauthorizedAccessException.class,
                () -> adminWalletController.freezeWallet(walletId, request));
    }

    // ---------------- 6. unfreezeWallet - success ----------------
    @Test
    void testUnfreezeWalletSuccess() {
        Long walletId = 2L;
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getWalletById(walletId)).thenReturn(wallet);

        ResponseEntity<String> response = adminWalletController.unfreezeWallet(walletId, request);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("unfrozen successfully"));
        verify(walletFreezeService).unfreezeWallet(wallet);
    }

    // ---------------- 7. getWalletTransactions - success ----------------
    @Test
    void testGetWalletTransactionsSuccess() {
        Long walletId = 3L;
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        WalletTransactionResponseDTO txn = new WalletTransactionResponseDTO("txn1", 100.0, "CREDIT", null, "desc", 1000.0, 5000.0);
        when(walletTransactionService.listTransactions(walletId)).thenReturn(List.of(txn));

        ResponseEntity<List<WalletTransactionResponseDTO>> response = adminWalletController.getWalletTransactions(walletId, request);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    // ---------------- 8. getWalletTransactions - unauthorized ----------------
    @Test
    void testGetWalletTransactionsUnauthorized() {
        Long walletId = 3L;
        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);

        assertThrows(UnauthorizedAccessException.class,
                () -> adminWalletController.getWalletTransactions(walletId, request));
    }

    // ---------------- 10. freezeWallet - verify logging call ----------------
    @Test
    void testFreezeWalletLogging() {
        Long walletId = 10L;
        AuthContext auth = new AuthContext("TOKEN", 1L, true);
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(walletService.getWalletById(walletId)).thenReturn(wallet);

        ResponseEntity<String> response = adminWalletController.freezeWallet(walletId, request);
        assertEquals("Wallet 10 frozen successfully", response.getBody());
        verify(walletFreezeService).freezeWallet(wallet);
    }
}
