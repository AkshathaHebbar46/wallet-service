package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 100L;

    public WalletTransactionService(WalletRepository walletRepository,
                                    TransactionRepository transactionRepository,
                                    WalletValidationService walletValidationService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletValidationService = walletValidationService;
    }

    // Helper to get authenticated userId
    private Long getAuthenticatedUserId() {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    // Helper to check if current user is ADMIN
    private boolean isAdmin() {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;

                WalletEntity wallet = walletRepository.findById(walletId)
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

                Long userId = getAuthenticatedUserId();
                log.info("User {} is attempting a {} of {} on wallet {}", userId, request.type(), request.amount(), walletId);

                // Ownership check
                if (!wallet.getUserId().equals(userId) && !isAdmin()) {
                    log.warn("User {} attempted to operate on wallet {} which does not belong to them", userId, walletId);
                    throw new SecurityException("Forbidden: Wallet does not belong to you");
                }

                walletValidationService.validateWalletState(wallet);

                TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
                double amount = request.amount();
                if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

                if (type == TransactionType.DEBIT) {
                    walletValidationService.validateBalance(wallet, amount);
                    walletValidationService.validateAndTrackDailyLimit(wallet, amount);
                    wallet.setBalance(wallet.getBalance() - amount);
                    log.info("Debited {} from wallet {}", amount, walletId);
                } else {
                    wallet.setBalance(wallet.getBalance() + amount);
                    log.info("Credited {} to wallet {}", amount, walletId);
                }

                walletRepository.save(wallet);

                TransactionEntity txn = new TransactionEntity(walletId, type, amount, request.description());
                txn.setTransactionId(request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString());
                transactionRepository.save(txn);

                log.info("Transaction {} saved successfully", txn.getTransactionId());

                return new WalletTransactionResponseDTO(
                        txn.getTransactionId(),
                        txn.getAmount(),
                        type.name(),
                        txn.getTransactionDate(),
                        txn.getDescription()
                );

            } catch (ObjectOptimisticLockingFailureException | CannotAcquireLockException ex) {
                log.warn("Attempt {} failed due to lock. Retrying...", attempt);
                try { Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1))); } catch (InterruptedException ignored) { }
            }
        }

        throw new IllegalStateException("Please try again later. Wallet busy.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;

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

                // Ownership check for sender wallet
                if (!from.getUserId().equals(userId) && !isAdmin()) {
                    log.warn("User {} attempted to transfer from wallet {} which does not belong to them", userId, fromWalletId);
                    throw new SecurityException("Forbidden: Cannot transfer from wallet you do not own");
                }

                walletValidationService.validateWalletState(from);
                walletValidationService.validateWalletState(to);
                walletValidationService.validateBalance(from, amount);
                walletValidationService.validateAndTrackDailyLimit(from, amount);

                from.setBalance(from.getBalance() - amount);
                to.setBalance(to.getBalance() + amount);

                walletRepository.save(from);
                walletRepository.save(to);

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

            } catch (ObjectOptimisticLockingFailureException | CannotAcquireLockException e) {
                log.warn("Attempt {} failed due to lock. Retrying...", attempt);
                try { Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1))); } catch (InterruptedException ignored) { }
            }
        }

        throw new IllegalStateException("Please try again later. Wallet busy.");
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        Long userId = getAuthenticatedUserId();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getUserId().equals(userId) && !isAdmin()) {
            log.warn("User {} attempted to access transactions of wallet {} which does not belong to them", userId, walletId);
            throw new SecurityException("Forbidden: Cannot view transactions of this wallet");
        }

        log.info("User {} listing transactions for wallet {}", userId, walletId);

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
            log.warn("Non-admin attempted to access all transactions");
            throw new SecurityException("Forbidden: Only admin can view all transactions");
        }
        log.info("Admin fetching all transactions");
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
