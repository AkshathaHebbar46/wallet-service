package org.walletservice.wallet_service.repository.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionRepositoryTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionRepositoryTestWrapper testWrapper; // Fake service wrapper for testing

    private TransactionEntity txn1;
    private TransactionEntity txn2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        txn1 = new TransactionEntity();
        txn1.setWalletId(1L);
        txn1.setType(TransactionType.CREDIT);
        txn1.setAmount(1000.0);
        txn1.setTransactionId("TXN123");
        txn1.setDescription("Deposit");
        txn1.setTransactionDate(LocalDateTime.now().minusDays(1));

        txn2 = new TransactionEntity();
        txn2.setWalletId(1L);
        txn2.setType(TransactionType.DEBIT);
        txn2.setAmount(500.0);
        txn2.setTransactionId("TXN124");
        txn2.setDescription("Purchase");
        txn2.setTransactionDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find all transactions for a wallet")
    void testFindByWalletId() {
        when(transactionRepository.findByWalletId(1L)).thenReturn(List.of(txn1, txn2));

        List<TransactionEntity> results = transactionRepository.findByWalletId(1L);

        assertEquals(2, results.size());
        assertEquals(TransactionType.CREDIT, results.get(0).getType());
        verify(transactionRepository).findByWalletId(1L);
    }

    @Test
    @DisplayName("Should find transactions greater than given amount")
    void testFindTransactionsGreaterThan() {
        when(transactionRepository.findTransactionsGreaterThan(800.0))
                .thenReturn(List.of(txn1));

        List<TransactionEntity> result = transactionRepository.findTransactionsGreaterThan(800.0);

        assertEquals(1, result.size());
        assertEquals(1000.0, result.get(0).getAmount());
    }

    @Test
    @DisplayName("Should find transactions by type")
    void testFindByType() {
        when(transactionRepository.findByType(TransactionType.CREDIT))
                .thenReturn(List.of(txn1));

        List<TransactionEntity> result = transactionRepository.findByType(TransactionType.CREDIT);

        assertEquals(1, result.size());
        assertEquals(TransactionType.CREDIT, result.get(0).getType());
    }

    @Test
    @DisplayName("Should find transactions between amounts")
    void testFindByAmountBetween() {
        when(transactionRepository.findByAmountBetween(400.0, 1200.0))
                .thenReturn(List.of(txn1, txn2));

        List<TransactionEntity> result = transactionRepository.findByAmountBetween(400.0, 1200.0);

        assertEquals(2, result.size());
        verify(transactionRepository).findByAmountBetween(400.0, 1200.0);
    }

    @Test
    @DisplayName("Should find transactions after given date")
    void testFindByTransactionDateAfter() {
        LocalDateTime date = LocalDateTime.now().minusDays(2);
        when(transactionRepository.findByTransactionDateAfter(date))
                .thenReturn(List.of(txn1, txn2));

        List<TransactionEntity> result = transactionRepository.findByTransactionDateAfter(date);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getTransactionDate().isAfter(date)));
    }

    @Test
    @DisplayName("Should find by transactionId")
    void testFindByTransactionId() {
        when(transactionRepository.findByTransactionId("TXN123"))
                .thenReturn(Optional.of(txn1));

        Optional<TransactionEntity> result = transactionRepository.findByTransactionId("TXN123");

        assertTrue(result.isPresent());
        assertEquals("Deposit", result.get().getDescription());
    }

    @Test
    @DisplayName("Should find by walletId and type with paging")
    void testFindByWalletIdAndType() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<TransactionEntity> page = new PageImpl<>(List.of(txn1));

        when(transactionRepository.findByWalletIdAndType(1L, TransactionType.CREDIT, pageable))
                .thenReturn(page);

        Page<TransactionEntity> result = transactionRepository.findByWalletIdAndType(1L, TransactionType.CREDIT, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(TransactionType.CREDIT, result.getContent().get(0).getType());
    }

    @Test
    @DisplayName("Should find by walletId, type, and date range with paging")
    void testFindByWalletIdAndTypeAndDateRange() {
        Pageable pageable = PageRequest.of(0, 2);
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();

        Page<TransactionEntity> page = new PageImpl<>(List.of(txn2));
        when(transactionRepository.findByWalletIdAndTypeAndTransactionDateBetween(1L, TransactionType.DEBIT, start, end, pageable))
                .thenReturn(page);

        Page<TransactionEntity> result =
                transactionRepository.findByWalletIdAndTypeAndTransactionDateBetween(1L, TransactionType.DEBIT, start, end, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.DEBIT, result.getContent().get(0).getType());
    }

    @Test
    @DisplayName("Should handle empty optional when transactionId not found")
    void testFindByTransactionId_NotFound() {
        when(transactionRepository.findByTransactionId("INVALID"))
                .thenReturn(Optional.empty());

        Optional<TransactionEntity> result = transactionRepository.findByTransactionId("INVALID");

        assertFalse(result.isPresent());
    }
}

/**
 * Dummy wrapper — only needed to satisfy @InjectMocks requirement.
 */
class TransactionRepositoryTestWrapper {
    private final TransactionRepository transactionRepository;

    TransactionRepositoryTestWrapper(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
}
