package org.walletservice.wallet_service.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.exception.WalletNotFoundException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.*;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminWalletControllerTest {

    @Mock private WalletService walletService;
    @Mock private AuthValidator authValidator;
    @Mock private WalletFreezeService walletFreezeService;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private AuthContext auth;

    @InjectMocks
    private AdminWalletController adminWalletController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ✅ 1. Admin fetches all wallets successfully
    @Test
    void getAllWalletsForUser_ShouldReturnWallets_WhenAdmin() {
        UserIdRequestDTO request = new UserIdRequestDTO(1L);
        List<WalletResponseDTO> wallets = List.of(new WalletResponseDTO(), new WalletResponseDTO());

        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletService.getWalletsByUser(1L)).thenReturn(wallets);

        ResponseEntity<List<WalletResponseDTO>> response =
                adminWalletController.getAllWalletsForUser(request, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(walletService).getWalletsByUser(1L);
    }

    // ✅ 2. Non-admin should get 403
    @Test
    void getAllWalletsForUser_ShouldReturnForbidden_WhenNotAdmin() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(false);

        ResponseEntity<List<WalletResponseDTO>> response =
                adminWalletController.getAllWalletsForUser(new UserIdRequestDTO(1L), httpRequest);

        assertEquals(403, response.getStatusCodeValue());
        verifyNoInteractions(walletService);
    }

    // ✅ 3. Admin but no wallets found
    @Test
    void getAllWalletsForUser_ShouldReturnNoContent_WhenNoWalletsFound() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletService.getWalletsByUser(1L)).thenReturn(List.of());

        ResponseEntity<List<WalletResponseDTO>> response =
                adminWalletController.getAllWalletsForUser(new UserIdRequestDTO(1L), httpRequest);

        assertEquals(204, response.getStatusCodeValue());
    }

    // ✅ 4. Admin freezes wallet successfully
    @Test
    void freezeWallet_ShouldFreeze_WhenAdmin() {
        WalletEntity wallet = new WalletEntity();
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletService.getWalletById(1L)).thenReturn(wallet);

        ResponseEntity<String> response = adminWalletController.freezeWallet(1L, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("frozen successfully"));
        verify(walletFreezeService).freezeWallet(wallet);
    }

    // ✅ 5. Non-admin should throw exception for freeze
    @Test
    void freezeWallet_ShouldThrowUnauthorized_WhenNotAdmin() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> adminWalletController.freezeWallet(1L, httpRequest));

        verify(walletFreezeService, never()).freezeWallet(any());
    }

    // ✅ 6. Admin unfreezes wallet successfully
    @Test
    void unfreezeWallet_ShouldUnfreeze_WhenAdmin() {
        WalletEntity wallet = new WalletEntity();
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletService.getWalletById(5L)).thenReturn(wallet);

        ResponseEntity<String> response = adminWalletController.unfreezeWallet(5L, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("unfrozen successfully"));
        verify(walletFreezeService).unfreezeWallet(wallet);
    }

    // ✅ 7. Non-admin should throw for unfreeze
    @Test
    void unfreezeWallet_ShouldThrowUnauthorized_WhenNotAdmin() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> adminWalletController.unfreezeWallet(5L, httpRequest));
    }

    // ✅ 8. Admin gets wallet transactions
    @Test
    void getWalletTransactions_ShouldReturnTransactions_WhenAdmin() {
        List<WalletTransactionResponseDTO> txs = List.of(
                new WalletTransactionResponseDTO("tx1", 100.0, "CREDIT", LocalDateTime.now(), "Initial deposit"),
                new WalletTransactionResponseDTO("tx2", -50.0, "DEBIT", LocalDateTime.now(), "Purchase at store")
        );
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletTransactionService.listTransactions(10L)).thenReturn(txs);

        ResponseEntity<List<WalletTransactionResponseDTO>> response =
                adminWalletController.getWalletTransactions(10L, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(walletTransactionService).listTransactions(10L);
    }

    // ✅ 9. Admin but no transactions found
    @Test
    void getWalletTransactions_ShouldThrow_WhenNoTransactionsFound() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(true);
        when(walletTransactionService.listTransactions(10L)).thenReturn(List.of());

        assertThrows(WalletNotFoundException.class,
                () -> adminWalletController.getWalletTransactions(10L, httpRequest));
    }

    // ✅ 10. Non-admin should throw unauthorized for transactions
    @Test
    void getWalletTransactions_ShouldThrowUnauthorized_WhenNotAdmin() {
        when(authValidator.getAuthContext(httpRequest)).thenReturn(auth);
        when(auth.isAdmin()).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> adminWalletController.getWalletTransactions(10L, httpRequest));
    }
}
