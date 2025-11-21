package org.walletservice.wallet_service.controller.transaction;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.request.WalletTransferRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessTransaction() {
        Long walletId = 1L;
        WalletTransactionRequestDTO dto = new WalletTransactionRequestDTO("txn1", 500.0, "CREDIT", "Test payment");

        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);

        WalletTransactionResponseDTO response = new WalletTransactionResponseDTO(
                "txn1", 500.0, "CREDIT", null, "Test payment", 1000.0, 5000.0
        );
        when(walletTransactionService.processTransaction(walletId, dto)).thenReturn(response);

        var result = transactionController.processTransaction(walletId, dto, request);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals("txn1", result.getBody().transactionId());
    }

    @Test
    void testTransferMoney() {
        WalletTransferRequestDTO dto = new WalletTransferRequestDTO(1L, 2L, 200.0);

        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, 1L)).thenReturn(true);

        WalletTransactionResponseDTO response = new WalletTransactionResponseDTO(
                "txn2", 200.0, "DEBIT", null, "Transfer", 800.0, 5000.0
        );
        when(walletTransactionService.transferMoney(1L, 2L, 200.0)).thenReturn(response);

        var result = transactionController.transferMoney(dto, request);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals("txn2", result.getBody().transactionId());
    }

    @Test
    void testListTransactions() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("TOKEN", 1L, false);

        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);

        List<WalletTransactionResponseDTO> transactions = List.of(
                new WalletTransactionResponseDTO("txn1", 100.0, "CREDIT", null, "desc", 1000.0, 5000.0)
        );
        when(walletTransactionService.listTransactions(walletId)).thenReturn(transactions);

        var result = transactionController.listTransactions(walletId, request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void testGetTransactionHistory() {
        Long walletId = 1L;
        AuthContext auth = new AuthContext("TOKEN", 1L, false);
        when(authValidator.getAuthContext(request)).thenReturn(auth);
        when(authValidator.isAuthorizedForWallet(auth, walletId)).thenReturn(true);

        Page<WalletTransactionResponseDTO> page = new PageImpl<>(List.of(
                new WalletTransactionResponseDTO("txn1", 100.0, "CREDIT", null, "desc", 1000.0, 5000.0)
        ));

        when(walletTransactionService.getFilteredTransactions(eq(walletId), eq(null), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        var result = transactionController.getTransactionHistory(walletId, null, null, null, 0, 10, request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(1, result.getBody().getContent().size());
    }

    @Test
    void testGetAllUserTransactions() {
        when(authValidator.extractToken(request)).thenReturn("TOKEN");
        when(authValidator.extractUserId("TOKEN")).thenReturn(1L);

        Page<WalletTransactionResponseDTO> page = new PageImpl<>(List.of(
                new WalletTransactionResponseDTO("txn1", 50.0, "CREDIT", null, "desc", 500.0, 1000.0)
        ));

        when(walletTransactionService.getAllUserTransactions(1L, PageRequest.of(0, 10))).thenReturn(page);

        var result = transactionController.getAllUserTransactions(PageRequest.of(0,10), request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(1, result.getBody().getContent().size());
    }
}
