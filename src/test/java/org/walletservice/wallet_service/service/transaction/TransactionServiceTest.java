package org.walletservice.wallet_service.service.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.service.wallet.WalletValidationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletValidationService walletValidationService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- 1. getFilteredTransactions ----------------
    @Test
    void testGetFilteredTransactions() {
        Long walletId = 1L;
        TransactionEntity txn = new TransactionEntity();
        txn.setTransactionId("txn1");
        txn.setAmount(500.0);
        txn.setType(TransactionType.CREDIT);
        txn.setTransactionDate(LocalDateTime.now());
        txn.setDescription("Test txn");
        txn.setWalletId(walletId);

        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);
        wallet.setBalance(1000.0);

        Page<TransactionEntity> txnPage = new PageImpl<>(List.of(txn));

        when(transactionRepository.findFilteredTransactions(
                eq(walletId), eq(TransactionType.CREDIT), any(), any(), any(Pageable.class)))
                .thenReturn(txnPage);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(5000.0);

        Page<WalletTransactionResponseDTO> result = transactionService.getFilteredTransactions(
                walletId, TransactionType.CREDIT, LocalDateTime.now().minusDays(1), LocalDateTime.now(), PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        WalletTransactionResponseDTO dto = result.getContent().get(0);
        assertEquals("txn1", dto.transactionId());
        assertEquals(1000.0, dto.balance());
        assertEquals(5000.0, dto.availableDailyLimit());
    }

    // ---------------- 2. getAllUserTransactions ----------------
    @Test
    void testGetAllUserTransactions() {
        Long userId = 1L;

        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setBalance(2000.0);

        TransactionEntity txn = new TransactionEntity();
        txn.setTransactionId("txn2");
        txn.setAmount(300.0);
        txn.setType(TransactionType.DEBIT);
        txn.setTransactionDate(LocalDateTime.now());
        txn.setDescription("Debit txn");
        txn.setWalletId(wallet.getId());

        Page<TransactionEntity> txnPage = new PageImpl<>(List.of(txn));

        when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet));
        when(transactionRepository.findByWalletIdIn(List.of(wallet.getId()), PageRequest.of(0, 10)))
                .thenReturn(txnPage);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(4000.0);

        Page<WalletTransactionResponseDTO> result = transactionService.getAllUserTransactions(
                userId, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        WalletTransactionResponseDTO dto = result.getContent().get(0);
        assertEquals("txn2", dto.transactionId());
        assertEquals(2000.0, dto.balance());
        assertEquals(4000.0, dto.availableDailyLimit());
    }

    // ---------------- 3. getAllUserTransactions - no wallets ----------------
    @Test
    void testGetAllUserTransactionsNoWallets() {
        Long userId = 1L;
        when(walletRepository.findByUserId(userId)).thenReturn(List.of());

        Page<WalletTransactionResponseDTO> result = transactionService.getAllUserTransactions(userId, PageRequest.of(0, 10));
        assertEquals(0, result.getTotalElements());
    }

    // ---------------- 4. getFilteredTransactions - wallet not found ----------------
    @Test
    void testGetFilteredTransactionsWalletNotFound() {
        Long walletId = 99L;

        when(transactionRepository.findFilteredTransactions(eq(walletId), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.getFilteredTransactions(walletId, TransactionType.CREDIT, null, null, PageRequest.of(0, 10))
        );
    }

    // ---------------- 5. getAllUserTransactions - transaction empty ----------------
    @Test
    void testGetAllUserTransactionsEmptyTransactions() {
        Long userId = 1L;
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setBalance(1000.0);

        when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet));
        when(transactionRepository.findByWalletIdIn(List.of(wallet.getId()), PageRequest.of(0, 10)))
                .thenReturn(Page.empty());

        Page<WalletTransactionResponseDTO> result = transactionService.getAllUserTransactions(userId, PageRequest.of(0, 10));
        assertEquals(0, result.getTotalElements());
    }
}
