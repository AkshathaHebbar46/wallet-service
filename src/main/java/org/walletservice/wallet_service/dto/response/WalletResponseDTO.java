package org.walletservice.wallet_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing wallet details returned from the Wallet microservice.
 */
@Schema(name = "WalletResponseDTO", description = "Details of a wallet")
public class WalletResponseDTO {

    @Schema(description = "Unique identifier of the wallet", example = "101")
    private Long walletId;

    @Schema(description = "ID of the user who owns this wallet", example = "1001")
    private Long userId;

    @Schema(description = "Current balance in the wallet", example = "2500.75")
    private Double currentBalance;

    public WalletResponseDTO() {}

    public WalletResponseDTO(Long walletId, Long userId, Double currentBalance) {
        this.walletId = walletId;
        this.userId = userId;
        this.currentBalance = currentBalance;
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

    public Double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(Double currentBalance) {
        this.currentBalance = currentBalance;
    }

    @Override
    public String toString() {
        return "WalletResponseDTO{" +
                "walletId=" + walletId +
                ", userId=" + userId +
                ", currentBalance=" + currentBalance +
                '}';
    }
}
