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
import org.walletservice.wallet_service.repository.transaction.TransactionRepository;

import java.time.LocalDateTime;

/**
 * Service to handle wallet transactions:
 * - fetch transaction history
 * - apply filters like type and date range
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Fetch filtered transactions for a wallet with pagination.
     *
     * @param walletId  Wallet ID to fetch transactions for
     * @param type      Transaction type filter (optional)
     * @param startDate Start date filter (optional)
     * @param endDate   End date filter (optional)
     * @param pageable  Pagination info
     * @return Page of WalletTransactionResponseDTO
     */
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

        // Fetch filtered transactions from repository
        Page<TransactionEntity> transactions = transactionRepository.findFilteredTransactions(
                walletId, type, startDate, endDate, pageable
        );

        log.debug("Found {} transactions for walletId={}", transactions.getTotalElements(), walletId);

        // Map entities to DTOs for API response
        return transactions.map(txn -> {
            log.trace("Mapping transaction {} to response DTO", txn.getTransactionId());
            return new WalletTransactionResponseDTO(
                    txn.getTransactionId(),
                    txn.getAmount(),
                    txn.getType().name(),
                    txn.getTransactionDate(),
                    txn.getDescription()
            );
        });
    }
}
