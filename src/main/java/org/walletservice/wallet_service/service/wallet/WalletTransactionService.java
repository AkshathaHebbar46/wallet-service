package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.mapper.WalletTransactionMapper;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.service.transaction.TransactionService;
import org.walletservice.wallet_service.validation.validator.WalletInternalValidationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletTransactionService {

    private static final Logger log = LoggerFactory.getLogger(WalletTransactionService.class);
    private static final int MAX_RETRY = 3;

    private final WalletRepository walletRepository;
    private final WalletValidationService walletValidationService;
    private final WalletInternalValidationService walletInternalValidationService;
    private final TransactionService transactionService;
    private final WalletTransactionMapper mapper;
    private final WalletService walletService;

    public WalletTransactionService(WalletRepository walletRepository,
                                    TransactionService transactionService,
                                    WalletValidationService walletValidationService,
                                    WalletInternalValidationService walletInternalValidationService,
                                    WalletTransactionMapper mapper,
                                    WalletService walletService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.walletValidationService = walletValidationService;
        this.walletInternalValidationService = walletInternalValidationService;
        this.mapper = mapper;
        this.walletService = walletService;
    }

    private Long getAuthenticatedUserId() {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private boolean isAdmin() {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {

        Optional<TransactionEntity> existing = transactionService.findByTransactionId(request.transactionId());
        if (existing.isPresent()) {
            TransactionEntity txn = existing.get();
            log.info("Idempotent request detected for transactionId={}", request.transactionId());

            WalletEntity wallet = walletService.getWalletById(walletId);

            double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

            return mapper.toDTO(
                    txn,
                    wallet.getBalance(),
                    availableDailyLimit
            );

        }

        int attempts = 0;
        while (attempts < MAX_RETRY) {
            try {
                WalletEntity wallet = validateTransaction(walletId, request);
                return processTransactionTransactional(wallet, request);
            } catch (Exception ex) {
                attempts++;
                if (attempts >= MAX_RETRY) throw ex;
            }
        }
        throw new IllegalStateException("Unexpected error processing transaction");
    }

    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        int attempts = 0;

        while (attempts < MAX_RETRY) {
            try {
                WalletEntity[] wallets = validateTransfer(fromWalletId, toWalletId, amount);
                return transferMoneyTransactional(wallets[0], wallets[1], amount);
            } catch (Exception ex) {
                attempts++;
                if (attempts >= MAX_RETRY) throw ex;
            }
        }
        throw new IllegalStateException("Unexpected error during transfer");
    }

    private WalletEntity validateTransaction(Long walletId, WalletTransactionRequestDTO request) {
        WalletEntity wallet = walletService.getWalletById(walletId);

        Long userId = getAuthenticatedUserId();

        if (!wallet.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Wallet does not belong to you");
        }

        walletValidationService.validateWalletState(wallet);

        double amount = request.amount();
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

        if (TransactionType.valueOf(request.type().toUpperCase()) == TransactionType.DEBIT) {
            walletValidationService.validateBalance(wallet, amount);
        }

        return wallet;
    }

    private WalletEntity[] validateTransfer(Long fromWalletId, Long toWalletId, Double amount) {
        if (Objects.equals(fromWalletId, toWalletId))
            throw new IllegalArgumentException("Cannot transfer to same wallet.");
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("Amount must be positive.");

        WalletEntity from = walletService.getWalletById(fromWalletId);

        Long userId = getAuthenticatedUserId();

        if (!from.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Cannot transfer from wallet you do not own");
        }

        walletValidationService.validateWalletState(from);
        walletValidationService.validateBalance(from, amount);

        WalletEntity to = walletService.getWalletById(toWalletId);

        walletInternalValidationService.validateReceiverWallet(toWalletId);

        return new WalletEntity[]{from, to};
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected WalletTransactionResponseDTO processTransactionTransactional(WalletEntity wallet, WalletTransactionRequestDTO request) {

        double amount = request.amount();
        TransactionType type = TransactionType.valueOf(request.type().toUpperCase());

        if (type == TransactionType.DEBIT) {
            walletValidationService.updateDailySpentAndFreeze(wallet, amount);
            wallet.setBalance(wallet.getBalance() - amount);
        } else {
            wallet.setBalance(wallet.getBalance() + amount);
        }

        walletRepository.save(wallet);

        TransactionEntity txn = new TransactionEntity(wallet.getId(), type, amount, request.description());
        txn.setTransactionId(request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString());

        transactionService.save(txn);

        double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

        return mapper.toDTO(
                txn,
                wallet.getBalance(),
                availableDailyLimit
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected WalletTransactionResponseDTO transferMoneyTransactional(WalletEntity from, WalletEntity to, Double amount) {

        walletValidationService.updateDailySpentAndFreeze(from, amount);

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        walletRepository.save(from);
        walletRepository.save(to);

        String txnId = UUID.randomUUID().toString();

        TransactionEntity debit = new TransactionEntity(from.getId(), TransactionType.DEBIT, amount,
                "Transfer to wallet " + to.getId());
        debit.setTransactionId(txnId + "-D");
        transactionService.save(debit);

        TransactionEntity credit = new TransactionEntity(to.getId(), TransactionType.CREDIT, amount,
                "Transfer from wallet " + from.getId());
        credit.setTransactionId(txnId + "-C");
        transactionService.save(credit);

        double availableDailyLimit = walletValidationService.getRemainingDailyLimit(from);

        return mapper.toDTO(
                debit,
                from.getBalance(),
                availableDailyLimit
        );
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {

        Long userId = getAuthenticatedUserId();

        WalletEntity wallet = walletService.getWalletById(walletId);

        if (!wallet.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Cannot view transactions of this wallet");
        }

        double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

        return transactionService.findByWalletId(walletId)
                .stream()
                .map(tx -> mapper.toDTO(
                        tx,
                        wallet.getBalance(),
                        availableDailyLimit
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> getAllTransactions() {
        if (!isAdmin()) {
            throw new SecurityException("Forbidden: Only admin can view all transactions");
        }

        return transactionService.findByWalletIdIn(
                walletRepository.findAll().stream().map(WalletEntity::getId).toList(),
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).stream().map(tx -> {

            WalletEntity wallet = walletService.getWalletById(tx.getWalletId());

            double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

            return mapper.toDTO(
                    tx,
                    wallet.getBalance(),
                    availableDailyLimit
            );

        }).collect(Collectors.toList());
    }

    // Get filtered transactions for a wallet
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponseDTO> getFilteredTransactions(Long walletId,
                                                                      TransactionType type,
                                                                      LocalDateTime startDate,
                                                                      LocalDateTime endDate,
                                                                      Pageable pageable) {
        WalletEntity wallet = walletService.getWalletById(walletId);

        Page<TransactionEntity> transactions = transactionService.findFilteredTransactions(walletId, type, startDate, endDate, pageable);

        double balance = wallet.getBalance();
        double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

        return transactions.map(txn -> mapper.toDTO(txn, balance, availableDailyLimit));
    }

    // Get all transactions for a user
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponseDTO> getAllUserTransactions(Long userId, Pageable pageable) {
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        List<Long> walletIds = wallets.stream().map(WalletEntity::getId).toList();

        if (walletIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<TransactionEntity> transactions = transactionService.findByWalletIdIn(walletIds, pageable);

        return transactions.map(txn -> {
            WalletEntity wallet = wallets.stream()
                    .filter(w -> w.getId().equals(txn.getWalletId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            double balance = wallet.getBalance();
            double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

            return mapper.toDTO(txn, balance, availableDailyLimit);
        });
    }


}
