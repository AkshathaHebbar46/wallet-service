package org.walletservice.wallet_service.service.transaction;

import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponseDTO> getFilteredTransactions(
            Long walletId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<TransactionEntity> transactions = transactionRepository.findFilteredTransactions(
                walletId, type, startDate, endDate, pageable
        );

        return transactions.map(txn -> new WalletTransactionResponseDTO(
                txn.getTransactionId(),
                txn.getAmount(),
                txn.getType().name(),
                txn.getTransactionDate(),
                txn.getDescription()
        ));
    }
}
