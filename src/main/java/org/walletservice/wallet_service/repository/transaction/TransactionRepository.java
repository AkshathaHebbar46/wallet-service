package org.walletservice.wallet_service.repository.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import org.walletservice.wallet_service.entity.transaction.TransactionType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Find transactions by wallet
    List<TransactionEntity> findByWalletId(Long walletId);

    // Custom JPQL query example: find all transactions above a certain amount
    @Query("SELECT t FROM TransactionEntity t WHERE t.amount > :amount")
    List<TransactionEntity> findTransactionsGreaterThan(Double amount);

    // Find transactions by type (CREDIT or DEBIT)
    List<TransactionEntity> findByType(TransactionType type);

    // Find transactions with amount between two values
    List<TransactionEntity> findByAmountBetween(Double min, Double max);

    // Find transactions after a certain date
    List<TransactionEntity> findByTransactionDateAfter(java.time.LocalDateTime date);

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    Page<TransactionEntity> findByWalletIdAndType(Long walletId, TransactionType type, Pageable pageable);

    Page<TransactionEntity> findByWalletIdIn(List<Long> walletIds, Pageable pageable);

    Page<TransactionEntity> findByWalletIdAndTypeAndTransactionDateBetween(
            Long walletId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.walletId = :walletId AND t.type = 'DEBIT' AND t.transactionDate BETWEEN :start AND :end")
    double sumDebitsByWalletAndDate(Long walletId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE (:walletId IS NULL OR t.walletId = :walletId)
          AND (:type IS NULL OR t.type = :type)
          AND (:start IS NULL OR t.transactionDate >= :start)
          AND (:end IS NULL OR t.transactionDate <= :end)
    """)
    Page<TransactionEntity> findFilteredTransactions(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

}
