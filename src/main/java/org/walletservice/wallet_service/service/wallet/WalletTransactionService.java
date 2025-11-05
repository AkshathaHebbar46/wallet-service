package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;

                WalletEntity wallet = walletRepository.findById(walletId)
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

                walletValidationService.validateWalletState(wallet);

                TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
                double amount = request.amount();
                if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

                if (type == TransactionType.DEBIT) {
                    walletValidationService.validateBalance(wallet, amount);
                    walletValidationService.validateAndTrackDailyLimit(wallet, amount);
                    wallet.setBalance(wallet.getBalance() - amount);
                } else {
                    wallet.setBalance(wallet.getBalance() + amount);
                }

                walletRepository.save(wallet);

                TransactionEntity txn = new TransactionEntity(walletId, type, amount, request.description());
                txn.setTransactionId(request.transactionId() != null
                        ? request.transactionId()
                        : UUID.randomUUID().toString());
                transactionRepository.save(txn);

                return new WalletTransactionResponseDTO(
                        txn.getTransactionId(),
                        txn.getAmount(),
                        type.name(),
                        txn.getTransactionDate(),
                        txn.getDescription()
                );

            } catch (ObjectOptimisticLockingFailureException | CannotAcquireLockException ex) {
                try {
                    Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
                } catch (InterruptedException ignored) { }
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

                return new WalletTransactionResponseDTO(
                        debit.getTransactionId(),
                        debit.getAmount(),
                        debit.getType().name(),
                        debit.getTransactionDate(),
                        debit.getDescription());

            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("⚠️ Optimistic lock failure during transfer attempt {}. Retrying...", attempt);
                try {
                    Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
                } catch (InterruptedException ignored) { }
            }
        }

        throw new IllegalStateException("Please try again later. Wallet busy.");
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
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
