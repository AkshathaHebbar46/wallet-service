package org.walletservice.wallet_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used to create a new wallet in the Wallet microservice.
 * The userId will come from the User microservice via REST call or message queue.
 */
public class WalletRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Min(value = 0, message = "Initial balance cannot be negative")
    private Double balance;

    public WalletRequestDTO() {}

    public WalletRequestDTO(Long userId, Double balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setInitialBalance(Double initialBalance) {
        this.balance = initialBalance;
    }

    @Override
    public String toString() {
        return "WalletRequestDTO{" +
                "userId=" + userId +
                ", balance=" + balance +
                '}';
    }
}
