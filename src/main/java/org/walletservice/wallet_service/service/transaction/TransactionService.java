package org.walletservice.wallet_service.service.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.service.wallet.WalletValidationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletValidationService walletValidationService;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository,
                              WalletValidationService walletValidationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.walletValidationService = walletValidationService;
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponseDTO> getFilteredTransactions(
            Long walletId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        log.info("Fetching transactions for walletId={} with type={} from {} to {}",
                walletId, type, startDate, endDate);

        Page<TransactionEntity> transactions = transactionRepository.findFilteredTransactions(
                walletId, type, startDate, endDate, pageable
        );

        log.debug("Found {} transactions for walletId={}", transactions.getTotalElements(), walletId);

        // Fetch wallet once for balance & daily limit
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

        // Map entities to DTOs for API response
        return transactions.map(txn -> new WalletTransactionResponseDTO(
                txn.getTransactionId(),
                txn.getAmount(),
                txn.getType().name(),
                txn.getTransactionDate(),
                txn.getDescription(),
                wallet.getBalance(),
                availableDailyLimit
        ));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponseDTO> getAllUserTransactions(Long userId, Pageable pageable) {
        log.info("Fetching ALL transactions for userId={}", userId);

        // 1️⃣ Get all wallets of the user
        List<Long> walletIds = walletRepository.findByUserId(userId)
                .stream()
                .map(w -> w.getId())
                .toList();

        if (walletIds.isEmpty()) {
            log.info("User {} has no wallets", userId);
            return Page.empty(pageable);
        }

        // 2️⃣ Fetch all transactions for these wallets
        Page<TransactionEntity> transactions = transactionRepository.findByWalletIdIn(walletIds, pageable);

        if (transactions.isEmpty()) {
            log.info("No transactions found for userId={}", userId);
            return Page.empty(pageable);
        }

        log.debug("Found {} transactions for userId={}", transactions.getTotalElements(), userId);

        // 3️⃣ Map transactions to DTOs
        return transactions.map(txn -> {
            WalletEntity wallet = walletRepository.findById(txn.getWalletId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            double availableDailyLimit = walletValidationService.getRemainingDailyLimit(wallet);

            return new WalletTransactionResponseDTO(
                    txn.getTransactionId(),
                    txn.getAmount(),
                    txn.getType().name(),
                    txn.getTransactionDate(),
                    txn.getDescription(),
                    wallet.getBalance(),
                    availableDailyLimit
            );
        });
    }



}
