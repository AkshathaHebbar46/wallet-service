package org.walletservice.wallet_service.service.wallet;

import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletTransactionServiceTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private WalletValidationService walletValidationService;
    private WalletTransactionService service;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        walletValidationService = mock(WalletValidationService.class);
        service = new WalletTransactionService(walletRepository, transactionRepository, walletValidationService);
        mockAuthenticatedUser(10L, false); // default user
    }

    private void mockAuthenticatedUser(Long userId, boolean isAdmin) {
        var authorities = isAdmin ? Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("Should fail if wallet does not belong to user")
    void testProcessTransaction_UnauthorizedUser() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity(99L, 200.0); // different user
        wallet.setId(walletId);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        WalletTransactionRequestDTO request =
                new WalletTransactionRequestDTO("credit", 20.0, "Invalid user", null);

        assertThrows(SecurityException.class,
                () -> service.processTransaction(walletId, request));
    }


    // -------------------------------------------------------------
    // transferMoney()
    // -------------------------------------------------------------

    @Test
    @DisplayName("Should transfer money successfully")
    void testTransferMoney_Success() {
        Long fromId = 1L;
        Long toId = 2L;
        WalletEntity from = new WalletEntity(10L, 200.0);
        from.setId(fromId);
        WalletEntity to = new WalletEntity(20L, 100.0);
        to.setId(toId);

        when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WalletTransactionResponseDTO response =
                service.transferMoney(fromId, toId, 50.0);

        assertNotNull(response);
        assertEquals("DEBIT", response.type());
        assertEquals(150.0, from.getBalance());
        assertEquals(150.0, to.getBalance());
        verify(walletRepository, times(2)).save(any(WalletEntity.class));
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should throw if same wallet IDs provided")
    void testTransferMoney_SameWallet() {
        assertThrows(IllegalArgumentException.class,
                () -> service.transferMoney(1L, 1L, 100.0));
    }

    @Test
    @DisplayName("Should throw if unauthorized user tries transfer")
    void testTransferMoney_Unauthorized() {
        Long fromId = 1L;
        Long toId = 2L;
        WalletEntity from = new WalletEntity(99L, 100.0); // belongs to someone else
        from.setId(fromId);
        WalletEntity to = new WalletEntity(10L, 50.0);
        to.setId(toId);

        when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

        assertThrows(SecurityException.class,
                () -> service.transferMoney(fromId, toId, 50.0));
    }

    // -------------------------------------------------------------
    // listTransactions()
    // -------------------------------------------------------------

    @Test
    @DisplayName("Should list transactions for wallet owner")
    void testListTransactions_Success() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity(10L, 200.0);
        wallet.setId(walletId);
        TransactionEntity tx = new TransactionEntity(walletId, TransactionType.CREDIT, 50.0, "Deposit");
        tx.setTransactionId("tx1");

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletId(walletId)).thenReturn(List.of(tx));

        List<WalletTransactionResponseDTO> list = service.listTransactions(walletId);

        assertEquals(1, list.size());
        assertEquals("tx1", list.get(0).transactionId());
    }

    @Test
    @DisplayName("Should block user from listing transactions of another wallet")
    void testListTransactions_Unauthorized() {
        Long walletId = 1L;
        WalletEntity wallet = new WalletEntity(99L, 100.0);
        wallet.setId(walletId);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(SecurityException.class,
                () -> service.listTransactions(walletId));
    }

    // -------------------------------------------------------------
    // getAllTransactions()
    // -------------------------------------------------------------

    @Test
    @DisplayName("Should allow admin to fetch all transactions")
    void testGetAllTransactions_Admin() {
        mockAuthenticatedUser(1L, true); // admin

        TransactionEntity tx = new TransactionEntity(1L, TransactionType.CREDIT, 100.0, "Admin view");
        tx.setTransactionId("TX-1");
        when(transactionRepository.findAll()).thenReturn(List.of(tx));

        List<WalletTransactionResponseDTO> result = service.getAllTransactions();
        assertEquals(1, result.size());
        assertEquals("TX-1", result.get(0).transactionId());
    }

    @Test
    @DisplayName("Should block non-admin from fetching all transactions")
    void testGetAllTransactions_NonAdminForbidden() {
        assertThrows(SecurityException.class,
                () -> service.getAllTransactions());
    }
}
