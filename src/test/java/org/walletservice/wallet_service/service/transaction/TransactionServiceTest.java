package org.walletservice.wallet_service.service.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

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

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- 1. save transaction ----------------
    @Test
    void testSaveTransaction() {
        TransactionEntity txn = new TransactionEntity();
        txn.setTransactionId("txn1");
        txn.setAmount(500.0);
        txn.setType(TransactionType.CREDIT);

        when(transactionRepository.save(txn)).thenReturn(txn);

        TransactionEntity saved = transactionService.save(txn);
        assertEquals("txn1", saved.getTransactionId());
        assertEquals(500.0, saved.getAmount());
    }

    // ---------------- 2. find by transactionId ----------------
    @Test
    void testFindByTransactionId() {
        TransactionEntity txn = new TransactionEntity();
        txn.setTransactionId("txn1");

        when(transactionRepository.findByTransactionId("txn1")).thenReturn(Optional.of(txn));

        Optional<TransactionEntity> result = transactionService.findByTransactionId("txn1");
        assertTrue(result.isPresent());
        assertEquals("txn1", result.get().getTransactionId());
    }

    // ---------------- 3. find by walletId ----------------
    @Test
    void testFindByWalletId() {
        TransactionEntity txn = new TransactionEntity();
        txn.setWalletId(1L);

        when(transactionRepository.findByWalletId(1L)).thenReturn(List.of(txn));

        List<TransactionEntity> result = transactionService.findByWalletId(1L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getWalletId());
    }

    // ---------------- 4. find by walletId in (pagination) ----------------
    @Test
    void testFindByWalletIdIn() {
        TransactionEntity txn = new TransactionEntity();
        txn.setWalletId(1L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionEntity> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findByWalletIdIn(List.of(1L), pageable)).thenReturn(page);

        Page<TransactionEntity> result = transactionService.findByWalletIdIn(List.of(1L), pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getWalletId());
    }

    // ---------------- 5. find filtered transactions ----------------
    @Test
    void testFindFilteredTransactions() {
        TransactionEntity txn = new TransactionEntity();
        txn.setWalletId(1L);
        txn.setType(TransactionType.CREDIT);
        txn.setTransactionDate(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionEntity> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findFilteredTransactions(1L, TransactionType.CREDIT, null, null, pageable))
                .thenReturn(page);

        Page<TransactionEntity> result = transactionService.findFilteredTransactions(
                1L, TransactionType.CREDIT, null, null, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.CREDIT, result.getContent().get(0).getType());
    }
}
