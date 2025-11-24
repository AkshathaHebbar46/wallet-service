package org.walletservice.wallet_service.entity.transaction;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_wallet_date", columnList = "wallet_id, transaction_date")
        }
)

public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "enum('CREDIT','DEBIT','TRANSFER')")
    private TransactionType type;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "transaction_id", nullable = false, unique = true, length = 255)
    private String transactionId;

    // --- Constructors ---
    public TransactionEntity() {}

    public TransactionEntity(Long walletId, TransactionType type, Double amount, String description) {
        this.walletId = walletId;
        this.type = type;
        setAmount(amount);
        this.description = description;
        this.transactionDate = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
