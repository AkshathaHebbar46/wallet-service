package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletTransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletValidationService walletValidationService;

    @Mock
    private WalletInternalValidationService walletInternalValidationService;

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

    // ---------------- 1. processTransaction - new CREDIT ----------------
    @Test
    void testProcessTransactionCredit() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity();
        wallet.setId(walletId);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", 500.0, "CREDIT", "Deposit");

        when(transactionRepository.findByTransactionId("txn1")).thenReturn(Optional.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(5000.0);

        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);

        assertEquals(1500.0, response.balance());
        assertEquals("CREDIT", response.type());
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    // ---------------- 2. processTransaction - idempotent ----------------
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

        when(transactionRepository.findByTransactionId("txn1")).thenReturn(Optional.of(txn));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(5000.0);

        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);
        assertEquals("txn1", response.transactionId());
        assertEquals(1000.0, response.balance()); // balance not updated because idempotent
    }

    // ---------------- 3. transferMoney ----------------
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

        when(walletRepository.findById(1L)).thenReturn(Optional.of(from));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(to));
        doNothing().when(walletInternalValidationService).validateReceiverWallet(2L);
        doNothing().when(walletValidationService).validateWalletState(from);
        doNothing().when(walletValidationService).validateBalance(from, 200.0);
        when(walletValidationService.getRemainingDailyLimit(from)).thenReturn(800.0);

        WalletTransactionResponseDTO response = walletTransactionService.transferMoney(1L, 2L, 200.0);

        assertEquals(800.0, response.balance()); // from wallet balance after transfer
        verify(walletRepository, times(2)).save(any(WalletEntity.class));
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class));
    }

    // ---------------- 4. listTransactions ----------------
    @Test
    void testListTransactions() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(2000.0);
        when(transactionRepository.findByWalletId(1L)).thenReturn(List.of(txn));

        List<WalletTransactionResponseDTO> list = walletTransactionService.listTransactions(1L);

        assertEquals(1, list.size());
        assertEquals("txn1", list.get(0).transactionId());
    }

    // ---------------- 5. getAllTransactions - admin ----------------
    @Test
    void testGetAllTransactionsAdmin() {
        // Mock admin authentication
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(() -> "ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), TransactionType.CREDIT, 500.0, "Deposit");
        txn.setTransactionId("txn1");

        when(transactionRepository.findAll()).thenReturn(List.of(txn));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletValidationService.getRemainingDailyLimit(wallet)).thenReturn(2000.0);

        List<WalletTransactionResponseDTO> list = walletTransactionService.getAllTransactions();
        assertEquals(1, list.size());
        assertEquals("txn1", list.get(0).transactionId());
    }

    // ---------------- 6. getAllTransactions - non-admin forbidden ----------------
    @Test
    void testGetAllTransactionsNonAdminForbidden() {
        assertThrows(SecurityException.class, () -> walletTransactionService.getAllTransactions());
    }

    // ---------------- 7. processTransaction - negative amount ----------------
    @Test
    void testProcessTransactionNegativeAmount() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(1000.0);

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", -100.0, "CREDIT", "Deposit");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class, () -> walletTransactionService.processTransaction(1L, request));
    }

    // ---------------- 8. transferMoney - same wallet ----------------
    @Test
    void testTransferMoneySameWallet() {
        assertThrows(IllegalArgumentException.class, () -> walletTransactionService.transferMoney(1L, 1L, 100.0));
    }

    // ---------------- 9. transferMoney - negative amount ----------------
    @Test
    void testTransferMoneyNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> walletTransactionService.transferMoney(1L, 2L, -50.0));
    }

    // ---------------- 10. processTransaction - DEBIT insufficient balance ----------------
    @Test
    void testProcessTransactionDebitInsufficientBalance() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setBalance(100.0);

        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", 200.0, "DEBIT", "Withdraw");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        doThrow(new IllegalArgumentException("Insufficient balance"))
                .when(walletValidationService).validateBalance(wallet, 200.0);

        assertThrows(IllegalArgumentException.class, () -> walletTransactionService.processTransaction(1L, request));
    }
}
