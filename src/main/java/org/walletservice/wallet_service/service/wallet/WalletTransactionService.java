package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletTransactionService {

    private static final Logger log = LoggerFactory.getLogger(WalletTransactionService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletValidationService walletValidationService;

    public WalletTransactionService(WalletRepository walletRepository,
                                    TransactionRepository transactionRepository,
                                    WalletValidationService walletValidationService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletValidationService = walletValidationService;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        Long userId = getAuthenticatedUserId();
        log.info("User {} is attempting a {} of {} on wallet {}", userId, request.type(), request.amount(), walletId);

        if (!wallet.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Wallet does not belong to you");
        }

        walletValidationService.validateWalletState(wallet);

        double amount = request.amount();
        TransactionType type = TransactionType.valueOf(request.type().toUpperCase());

        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

        if (type == TransactionType.DEBIT) {
            walletValidationService.validateBalance(wallet, amount);
            walletValidationService.updateDailySpentAndFreeze(wallet, amount);
            wallet.setBalance(wallet.getBalance() - amount);
        } else {
            wallet.setBalance(wallet.getBalance() + amount);
            walletRepository.save(wallet); // only save for CREDIT
        }

        TransactionEntity txn = new TransactionEntity(walletId, type, amount, request.description());
        txn.setTransactionId(request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString());
        transactionRepository.save(txn);

        log.info("Transaction {} completed", txn.getTransactionId());

        return new WalletTransactionResponseDTO(
                txn.getTransactionId(),
                txn.getAmount(),
                type.name(),
                txn.getTransactionDate(),
                txn.getDescription()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        if (Objects.equals(fromWalletId, toWalletId))
            throw new IllegalArgumentException("Cannot transfer to same wallet.");
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("Amount must be positive.");

        WalletEntity from = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        WalletEntity to = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        Long userId = getAuthenticatedUserId();
        log.info("User {} attempting to transfer {} from wallet {} to wallet {}", userId, amount, fromWalletId, toWalletId);

        if (!from.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Cannot transfer from wallet you do not own");
        }

        walletValidationService.validateWalletState(from);
        walletValidationService.validateWalletActive(to); // only check presence + active
        walletValidationService.validateBalance(from, amount);
        walletValidationService.updateDailySpentAndFreeze(from, amount);

        // Update balances
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        // Save both wallets once
        walletRepository.save(from);
        walletRepository.save(to);

        // Save transactions
        String txnId = UUID.randomUUID().toString();
        TransactionEntity debit = new TransactionEntity(fromWalletId, TransactionType.DEBIT, amount,
                "Transfer to wallet " + toWalletId);
        debit.setTransactionId(txnId + "-D");
        transactionRepository.save(debit);

        TransactionEntity credit = new TransactionEntity(toWalletId, TransactionType.CREDIT, amount,
                "Transfer from wallet " + fromWalletId);
        credit.setTransactionId(txnId + "-C");
        transactionRepository.save(credit);

        log.info("Transfer {} completed successfully", txnId);

        return new WalletTransactionResponseDTO(
                debit.getTransactionId(),
                debit.getAmount(),
                debit.getType().name(),
                debit.getTransactionDate(),
                debit.getDescription());
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        Long userId = getAuthenticatedUserId();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getUserId().equals(userId) && !isAdmin()) {
            throw new SecurityException("Forbidden: Cannot view transactions of this wallet");
        }

        return transactionRepository.findByWalletId(walletId).stream()
                .map(tx -> new WalletTransactionResponseDTO(
                        tx.getTransactionId(),
                        tx.getAmount(),
                        tx.getType().name(),
                        tx.getTransactionDate(),
                        tx.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> getAllTransactions() {
        if (!isAdmin()) {
            throw new SecurityException("Forbidden: Only admin can view all transactions");
        }

        return transactionRepository.findAll().stream()
                .map(tx -> new WalletTransactionResponseDTO(
                        tx.getTransactionId(),
                        tx.getAmount(),
                        tx.getType().name(),
                        tx.getTransactionDate(),
                        tx.getDescription()))
                .collect(Collectors.toList());
    }
}
