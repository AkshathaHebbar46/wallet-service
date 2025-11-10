package org.walletservice.wallet_service.controller.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.request.WalletTransferRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.transaction.TransactionService;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    private WalletTransactionService walletTransactionService;
    private TransactionService transactionService;
    private AuthValidator authValidator;
    private HttpServletRequest request;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        walletTransactionService = mock(WalletTransactionService.class);
        transactionService = mock(TransactionService.class);
        authValidator = mock(AuthValidator.class);
        request = mock(HttpServletRequest.class);
        controller = new TransactionController(walletTransactionService, transactionService, authValidator);
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should process a valid transaction for authorized user")
    void testProcessTransaction_Success() {
        Long walletId = 1L;
        WalletTransactionRequestDTO dto =
                new WalletTransactionRequestDTO("TX123", 100.0, "DEBIT", "Test payment");
        WalletTransactionResponseDTO responseDTO =
                new WalletTransactionResponseDTO("TX123", 100.0, "DEBIT", LocalDateTime.now(), "Processed");

        AuthContext auth = new AuthContext("user@example.com", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);
        when(walletTransactionService.processTransaction(walletId, dto)).thenReturn(responseDTO);

        ResponseEntity<WalletTransactionResponseDTO> response = controller.processTransaction(walletId, dto, request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(responseDTO, response.getBody());
        verify(walletTransactionService).processTransaction(walletId, dto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException for unauthorized user in processTransaction")
    void testProcessTransaction_Unauthorized() {
        Long walletId = 1L;
        WalletTransactionRequestDTO dto =
                new WalletTransactionRequestDTO("TX999", 100.0, "CREDIT", "Test");
        AuthContext auth = new AuthContext("user@example.com", 2L, false);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () ->
                controller.processTransaction(walletId, dto, request));
        verify(walletTransactionService, never()).processTransaction(any(), any());
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should transfer money successfully for authorized user")
    void testTransferMoney_Success() {
        WalletTransferRequestDTO dto = new WalletTransferRequestDTO(1L, 2L, 200.0);
        WalletTransactionResponseDTO responseDTO =
                new WalletTransactionResponseDTO("TX999", 200.0, "TRANSFER", LocalDateTime.now(), "Transferred");

        AuthContext auth = new AuthContext("user@example.com", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, dto.fromWalletId())).thenReturn(true);
        when(walletTransactionService.transferMoney(dto.fromWalletId(), dto.toWalletId(), dto.amount()))
                .thenReturn(responseDTO);

        ResponseEntity<WalletTransactionResponseDTO> response = controller.transferMoney(dto, request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(responseDTO, response.getBody());
        verify(walletTransactionService).transferMoney(dto.fromWalletId(), dto.toWalletId(), dto.amount());
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException for unauthorized transfer")
    void testTransferMoney_Unauthorized() {
        WalletTransferRequestDTO dto = new WalletTransferRequestDTO(1L, 2L, 200.0);
        AuthContext auth = new AuthContext("user@example.com", 99L, false);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, dto.fromWalletId())).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () ->
                controller.transferMoney(dto, request));
        verify(walletTransactionService, never()).transferMoney(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should list all transactions for authorized wallet")
    void testListTransactions_Success() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("user@example.com", 1L, false);
        List<WalletTransactionResponseDTO> transactions = List.of(
                new WalletTransactionResponseDTO("TX1", 50.0, "DEBIT", LocalDateTime.now(), "Groceries"),
                new WalletTransactionResponseDTO("TX2", 100.0, "CREDIT", LocalDateTime.now(), "Refund")
        );

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);
        when(walletTransactionService.listTransactions(walletId)).thenReturn(transactions);

        ResponseEntity<List<WalletTransactionResponseDTO>> response = controller.listTransactions(walletId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(walletTransactionService).listTransactions(walletId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException for listTransactions when unauthorized")
    void testListTransactions_Unauthorized() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("user@example.com", 2L, false);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () ->
                controller.listTransactions(walletId, request));
        verify(walletTransactionService, never()).listTransactions(any());
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch filtered transaction history successfully")
    void testGetTransactionHistory_Success() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("user@example.com", 1L, false);
        Pageable pageable = PageRequest.of(0, 10);
        Page<WalletTransactionResponseDTO> mockPage = new PageImpl<>(
                List.of(new WalletTransactionResponseDTO("TX123", 300.0, "CREDIT", LocalDateTime.now(), "Bonus")));

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);
        when(transactionService.getFilteredTransactions(walletId, null, null, null, pageable))
                .thenReturn(mockPage);

        ResponseEntity<Page<WalletTransactionResponseDTO>> response =
                controller.getTransactionHistory(walletId, null, null, null, 0, 10, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getTotalElements());
        verify(transactionService).getFilteredTransactions(walletId, null, null, null, pageable);
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException for getTransactionHistory when unauthorized")
    void testGetTransactionHistory_Unauthorized() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("user@example.com", 3L, false);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () ->
                controller.getTransactionHistory(walletId, null, null, null, 0, 10, request));
        verify(transactionService, never()).getFilteredTransactions(any(), any(), any(), any(), any());
    }
}
