package org.walletservice.wallet_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletResponseDTO", description = "Details of a wallet")
public class WalletResponseDTO {

    @Schema(description = "Unique identifier of the wallet", example = "101")
    private Long walletId;

    @Schema(description = "ID of the user who owns this wallet", example = "1001")
    private Long userId;

    @Schema(description = "Balance in the wallet", example = "2500.75")
    private Double balance;

    public WalletResponseDTO() {}

    public WalletResponseDTO(Long walletId, Long userId, Double balance) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance; // FIXED
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
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
        return "WalletResponseDTO{" +
                "walletId=" + walletId +
                ", userId=" + userId +
                ", balance=" + balance +
                '}';
    }
}
