package org.walletservice.wallet_service.entity.wallet;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallets",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id")
        }
)
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance", nullable = false)
    private Double balance = 0.0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "daily_spent", nullable = false)
    private Double dailySpent = 0.0;

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate = LocalDateTime.now();

    @Column(name = "frozen", nullable = false)
    private Boolean frozen = false;

    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Wallet creation date
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Constructors ---
    public WalletEntity() {}

    public WalletEntity(Long userId, Double balance) {
        this.userId = userId;
        this.balance = (balance != null && balance >= 0) ? balance : 0.0;
        this.dailySpent = 0.0;
        this.frozen = false;
        this.lastTransactionDate = LocalDateTime.now();
    }

    // --- Helper Methods ---

    /** Reset daily spent and frozen status if it's a new day (here: every 2 minutes for testing). */
    public void resetDailyIfNewDay() {
        if (lastTransactionDate == null) {
            lastTransactionDate = LocalDateTime.now();
            return;
        }

        Duration duration = Duration.between(lastTransactionDate, LocalDateTime.now());
        if (duration.toMinutes() >= 2) {
            this.dailySpent = 0.0;
            this.lastTransactionDate = LocalDateTime.now();
        }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) {
        if (balance < 0) throw new IllegalArgumentException("Balance cannot be negative");
        this.balance = balance;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Double getDailySpent() { return dailySpent; }
    public void setDailySpent(Double dailySpent) { this.dailySpent = dailySpent; }

    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }

    public Boolean getFrozen() { return frozen; }
    public void setFrozen(Boolean frozen) { this.frozen = frozen; }

    public LocalDateTime getFrozenAt() { return frozenAt; }
    public void setFrozenAt(LocalDateTime frozenAt) { this.frozenAt = frozenAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
