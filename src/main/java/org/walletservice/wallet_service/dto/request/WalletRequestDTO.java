package org.walletservice.wallet_service.dto.request;


import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;

@Schema(name = "WalletRequestDTO", description = "DTO for creating or updating a wallet")
public class WalletRequestDTO {

    @Schema(description = "ID of the user owning the wallet", example = "123", required = true)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Initial balance of the wallet", example = "1000.0")
    @NotNull(message = "Balance is required")
    @ValidTransactionAmount
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

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "WalletRequestDTO{" +
                "userId=" + userId +
                ", balance=" + balance +
                '}';
    }
}
