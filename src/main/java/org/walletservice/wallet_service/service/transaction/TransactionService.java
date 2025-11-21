package org.walletservice.wallet_service.service.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Save transaction
    public TransactionEntity save(TransactionEntity entity) {
        return transactionRepository.save(entity);
    }

    // Find by transaction ID
    public Optional<TransactionEntity> findByTransactionId(String txnId) {
        return transactionRepository.findByTransactionId(txnId);
    }

    // Find all transactions for a wallet
    public List<TransactionEntity> findByWalletId(Long walletId) {
        return transactionRepository.findByWalletId(walletId);
    }

    // Find transactions for multiple wallets with pagination
    public Page<TransactionEntity> findByWalletIdIn(List<Long> walletIds, Pageable pageable) {
        return transactionRepository.findByWalletIdIn(walletIds, pageable);
    }

    // Filtered transactions
    public Page<TransactionEntity> findFilteredTransactions(
            Long walletId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return transactionRepository.findFilteredTransactions(walletId, type, startDate, endDate, pageable);
    }
}
