package org.walletservice.wallet_service.service.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ✅ Comprehensive unit tests for {@link TransactionService}.
 */
class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        transactionService = new TransactionService(transactionRepository);
    }

    // ───────────────────────────────
    @Test
    @DisplayName("✅ Should map transactions correctly when data exists")
    void testGetFilteredTransactions_Success() {
        Long walletId = 1L;
        TransactionType type = TransactionType.CREDIT;
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(0, 10);

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("TXN-123");
        entity.setWalletId(walletId);
        entity.setAmount(250.0);
        entity.setType(TransactionType.CREDIT);
        entity.setTransactionDate(LocalDateTime.now().minusDays(1));
        entity.setDescription("Top-up");

        when(transactionRepository.findFilteredTransactions(walletId, type, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<WalletTransactionResponseDTO> result =
                transactionService.getFilteredTransactions(walletId, type, start, end, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        WalletTransactionResponseDTO dto = result.getContent().get(0);

        assertEquals("TXN-123", dto.transactionId());
        assertEquals(250.0, dto.amount());
        assertEquals("CREDIT", dto.type());
        assertEquals("Top-up", dto.description());
        assertNotNull(dto.timestamp());

        verify(transactionRepository).findFilteredTransactions(walletId, type, start, end, pageable);
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🟡 Should handle empty transaction results gracefully")
    void testGetFilteredTransactions_EmptyResult() {
        Long walletId = 99L;
        TransactionType type = TransactionType.DEBIT;
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(0, 5);

        when(transactionRepository.findFilteredTransactions(walletId, type, start, end, pageable))
                .thenReturn(Page.empty(pageable));

        Page<WalletTransactionResponseDTO> result =
                transactionService.getFilteredTransactions(walletId, type, start, end, pageable);

        assertTrue(result.isEmpty());
        verify(transactionRepository).findFilteredTransactions(walletId, type, start, end, pageable);
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🟢 Should pass correct arguments to repository")
    void testGetFilteredTransactions_VerifyArguments() {
        Long walletId = 42L;
        TransactionType type = TransactionType.DEBIT;
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);
        PageRequest pageable = PageRequest.of(1, 20);

        when(transactionRepository.findFilteredTransactions(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        transactionService.getFilteredTransactions(walletId, type, start, end, pageable);

        ArgumentCaptor<Long> walletCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);

        verify(transactionRepository).findFilteredTransactions(
                walletCaptor.capture(),
                typeCaptor.capture(),
                startCaptor.capture(),
                endCaptor.capture(),
                pageableCaptor.capture()
        );

        assertEquals(walletId, walletCaptor.getValue());
        assertEquals(type, typeCaptor.getValue());
        assertEquals(start, startCaptor.getValue());
        assertEquals(end, endCaptor.getValue());
        assertEquals(pageable, pageableCaptor.getValue());
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🔴 Should handle repository exception safely")
    void testGetFilteredTransactions_RepositoryThrowsException() {
        Long walletId = 7L;
        TransactionType type = TransactionType.CREDIT;
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(0, 5);

        when(transactionRepository.findFilteredTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                transactionService.getFilteredTransactions(walletId, type, start, end, pageable));

        assertEquals("DB failure", ex.getMessage());
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🟠 Should set current timestamp if transactionDate is null")
    void testGetFilteredTransactions_NullTransactionDate() {
        Long walletId = 1L;
        TransactionType type = TransactionType.CREDIT;
        PageRequest pageable = PageRequest.of(0, 1);

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("TXN-NULL-DATE");
        entity.setWalletId(walletId);
        entity.setAmount(100.0);
        entity.setType(TransactionType.CREDIT);
        entity.setTransactionDate(null);
        entity.setDescription("Null date test");

        when(transactionRepository.findFilteredTransactions(eq(walletId), eq(type), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<WalletTransactionResponseDTO> result =
                transactionService.getFilteredTransactions(walletId, type, null, null, pageable);

        assertEquals(1, result.getContent().size());
        assertNotNull(result.getContent().get(0).timestamp()); // should be auto-set
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🧊 Should handle startDate after endDate gracefully (inverted range)")
    void testGetFilteredTransactions_InvertedDateRange() {
        Long walletId = 3L;
        TransactionType type = TransactionType.DEBIT;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(5);
        PageRequest pageable = PageRequest.of(0, 5);

        // even though dates are invalid logically, service should still pass to repo safely
        when(transactionRepository.findFilteredTransactions(walletId, type, start, end, pageable))
                .thenReturn(Page.empty(pageable));

        Page<WalletTransactionResponseDTO> result =
                transactionService.getFilteredTransactions(walletId, type, start, end, pageable);

        assertTrue(result.isEmpty());
        verify(transactionRepository).findFilteredTransactions(walletId, type, start, end, pageable);
    }

    // ───────────────────────────────
    @Test
    @DisplayName("🧩 Should support pagination correctly")
    void testGetFilteredTransactions_Pagination() {
        Long walletId = 5L;
        TransactionType type = TransactionType.CREDIT;
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(2, 2); // page index 2, size 2

        TransactionEntity e1 = new TransactionEntity();
        e1.setTransactionId("TXN-P1");
        e1.setType(TransactionType.CREDIT);
        e1.setTransactionDate(LocalDateTime.now().minusDays(1));
        e1.setAmount(10.0);

        TransactionEntity e2 = new TransactionEntity();
        e2.setTransactionId("TXN-P2");
        e2.setType(TransactionType.CREDIT);
        e2.setTransactionDate(LocalDateTime.now().minusHours(5));
        e2.setAmount(20.0);

        when(transactionRepository.findFilteredTransactions(walletId, type, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 10));

        Page<WalletTransactionResponseDTO> result =
                transactionService.getFilteredTransactions(walletId, type, start, end, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(10, result.getTotalElements());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getNumber());
    }
}
