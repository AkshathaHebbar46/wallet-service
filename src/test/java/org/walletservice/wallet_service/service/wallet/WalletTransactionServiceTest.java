package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.mapper.WalletTransactionMapper;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.service.transaction.TransactionService;
import org.walletservice.wallet_service.validation.validator.WalletInternalValidationService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletTransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletValidationService walletValidationService;

    @Mock
    private WalletInternalValidationService walletInternalValidationService;

    @Mock
    private WalletTransactionMapper mapper;

    @Mock
    private WalletService walletService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletTransactionService walletTransactionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Mock authentication for userId = 1
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Test a standard CREDIT transaction processing.
     * Verifies that transaction is saved and wallet balance is updated.
     */
    @Test
    void testProcessTransactionCredit() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", 500.0, "CREDIT", "Deposit");
        TransactionEntity txn = new TransactionEntity(walletId, TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        when(transactionService.findByTransactionId("txn1")).thenReturn(Optional.empty());
        when(walletService.getWalletById(walletId)).thenReturn(wallet);
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(5000.0);
        when(mapper.toDTO(any(TransactionEntity.class), anyDouble(), anyDouble()))
                .thenReturn(mock(WalletTransactionResponseDTO.class));

        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);

        verify(transactionService).save(any(TransactionEntity.class));
        verify(walletRepository).save(wallet);
        assertNotNull(response);
    }

    /**
     * Test idempotent transaction: when transactionId already exists.
     * Verifies that the transaction is not duplicated.
     */
    @Test
    void testProcessTransactionIdempotent() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(walletId, TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", 500.0, "CREDIT", "Deposit");

        when(transactionService.findByTransactionId("txn1")).thenReturn(Optional.of(txn));
        when(walletService.getWalletById(walletId)).thenReturn(wallet);
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(5000.0);
        when(mapper.toDTO(txn, wallet.getBalance(), 5000.0)).thenReturn(mock(WalletTransactionResponseDTO.class));

        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);

        assertNotNull(response);
        verify(transactionService, never()).save(any(TransactionEntity.class)); // Already exists
    }

    /**
     * Test money transfer between wallets.
     * Verifies that both wallets are saved and transactions are recorded.
     */
    @Test
    void testTransferMoney() {
        WalletEntity from = new WalletEntity();
        from.setId(1L);
        from.setUserId(1L);
        from.setBalance(1000.0);

        WalletEntity to = new WalletEntity();
        to.setId(2L);
        to.setUserId(2L);
        to.setBalance(500.0);

        when(walletService.getWalletById(1L)).thenReturn(from);
        when(walletService.getWalletById(2L)).thenReturn(to);
        doNothing().when(walletInternalValidationService).validateReceiverWallet(2L);
        doNothing().when(walletValidationService).validateWalletState(from);
        doNothing().when(walletValidationService).validateBalance(from, 200.0);
        when(walletValidationService.getRemainingDailyLimit(from)).thenReturn(800.0);
        when(mapper.toDTO(any(TransactionEntity.class), anyDouble(), anyDouble()))
                .thenReturn(mock(WalletTransactionResponseDTO.class));

        WalletTransactionResponseDTO response = walletTransactionService.transferMoney(1L, 2L, 200.0);

        assertNotNull(response);
        verify(walletRepository, times(2)).save(any(WalletEntity.class));
        verify(transactionService, times(2)).save(any(TransactionEntity.class));
    }

    /**
     * Test listing transactions for a wallet.
     * Verifies that the returned list has expected size.
     */
    @Test
    void testListTransactions() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(2000.0);
        when(transactionService.findByWalletId(1L)).thenReturn(List.of(txn));
        when(mapper.toDTO(any(TransactionEntity.class), anyDouble(), anyDouble()))
                .thenReturn(mock(WalletTransactionResponseDTO.class));

        List<WalletTransactionResponseDTO> list = walletTransactionService.listTransactions(1L);
        assertEquals(1, list.size());
    }

    /**
     * Test filtered transactions with a Page response.
     */
    @Test
    void testGetFilteredTransactions() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        Pageable pageable = PageRequest.of(0, 10);

        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(transactionService.findFilteredTransactions(1L, TransactionType.CREDIT, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(txn)));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(2000.0);
        when(mapper.toDTO(any(TransactionEntity.class), anyDouble(), anyDouble()))
                .thenReturn(mock(WalletTransactionResponseDTO.class));

        Page<WalletTransactionResponseDTO> result = walletTransactionService.getFilteredTransactions(
                1L, TransactionType.CREDIT, null, null, pageable
        );

        assertEquals(1, result.getContent().size());
    }

    /**
     * Test getting all transactions for a user with multiple wallets.
     */
    @Test
    void testGetAllUserTransactions() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        Pageable pageable = PageRequest.of(0, 10);

        when(walletRepository.findByUserId(1L)).thenReturn(List.of(wallet));
        when(transactionService.findByWalletIdIn(List.of(1L), pageable))
                .thenReturn(new PageImpl<>(List.of(txn)));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(2000.0);
        when(mapper.toDTO(any(TransactionEntity.class), anyDouble(), anyDouble()))
                .thenReturn(mock(WalletTransactionResponseDTO.class));

        Page<WalletTransactionResponseDTO> result = walletTransactionService.getAllUserTransactions(1L, pageable);

        assertEquals(1, result.getContent().size());
    }

    // ------------------ ADDITIONAL TESTS ------------------

    /**
     * Test transfer fails if balance is insufficient.
     */
    @Test
    void testTransferMoney_insufficientBalance() {
        WalletEntity from = new WalletEntity();
        from.setId(1L);
        from.setUserId(1L);
        from.setBalance(100.0);

        WalletEntity to = new WalletEntity();
        to.setId(2L);
        to.setUserId(2L);
        to.setBalance(500.0);

        when(walletService.getWalletById(1L)).thenReturn(from);
        when(walletService.getWalletById(2L)).thenReturn(to);
        doThrow(new IllegalStateException("Insufficient balance"))
                .when(walletValidationService).validateBalance(from, 200.0);

        Exception ex = assertThrows(IllegalStateException.class,
                () -> walletTransactionService.transferMoney(1L, 2L, 200.0));
        assertEquals("Insufficient balance", ex.getMessage());
    }

    /**
     * Test transfer fails if receiver wallet is invalid.
     */
    @Test
    void testTransferMoney_invalidReceiver() {
        WalletEntity from = new WalletEntity();
        from.setId(1L);
        from.setUserId(1L);
        from.setBalance(1000.0);

        when(walletService.getWalletById(1L)).thenReturn(from);
        doThrow(new IllegalArgumentException("Receiver wallet invalid"))
                .when(walletInternalValidationService).validateReceiverWallet(999L);

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> walletTransactionService.transferMoney(1L, 999L, 100.0));
        assertEquals("Receiver wallet invalid", ex.getMessage());
    }

    /**
     * Test CREDIT transaction exceeding daily limit.
     */
    @Test
    void testProcessTransaction_exceedDailyLimit() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);
        wallet.setDailySpent(45000.0); // Already spent today

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn2", 6000.0, "CREDIT", "Deposit");

        when(transactionService.findByTransactionId("txn2")).thenReturn(Optional.empty());
        when(walletService.getWalletById(1L)).thenReturn(wallet);

        // Spy on walletValidationService to actually throw exception when limit exceeded
        doCallRealMethod().when(walletValidationService).updateDailySpentAndFreeze(wallet, 6000.0);

        Exception ex = assertThrows(IllegalStateException.class,
                () -> walletValidationService.updateDailySpentAndFreeze(wallet, request.amount()));

        assertTrue(ex.getMessage().contains("Daily limit exceeded"));
    }


    /**
     * Test listTransactions returns empty list when no transactions.
     */
    @Test
    void testListTransactions_empty() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);

        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(transactionService.findByWalletId(1L)).thenReturn(List.of());

        List<WalletTransactionResponseDTO> result = walletTransactionService.listTransactions(1L);
        assertTrue(result.isEmpty());
    }

    /**
     * Test getFilteredTransactions returns empty Page.
     */
    @Test
    void testGetFilteredTransactions_noResults() {
        Pageable pageable = PageRequest.of(0, 10);
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);

        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(transactionService.findFilteredTransactions(1L, TransactionType.DEBIT, null, null, pageable))
                .thenReturn(Page.empty());

        Page<WalletTransactionResponseDTO> result = walletTransactionService.getFilteredTransactions(
                1L, TransactionType.DEBIT, null, null, pageable
        );

        assertTrue(result.getContent().isEmpty());
    }

    /**
     * Test getAllUserTransactions returns empty Page when user has no wallets.
     */
    @Test
    void testGetAllUserTransactions_noWallets() {
        Pageable pageable = PageRequest.of(0, 10);
        when(walletRepository.findByUserId(1L)).thenReturn(List.of());

        Page<WalletTransactionResponseDTO> result = walletTransactionService.getAllUserTransactions(1L, pageable);

        assertTrue(result.getContent().isEmpty());
    }
}
