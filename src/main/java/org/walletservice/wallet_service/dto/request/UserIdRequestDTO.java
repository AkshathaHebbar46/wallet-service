package org.walletservice.wallet_service.dto.request;

public class UserIdRequestDTO {

    private Long userId;

    public UserIdRequestDTO() {}

    public UserIdRequestDTO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
