package org.walletservice.wallet_service.dto.response;

/**
 * DTO representing wallet details returned from the Wallet microservice.
 */
public class WalletResponseDTO {

    private Long walletId;
    private Long userId;
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
