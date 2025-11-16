package org.walletservice.wallet_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserIdRequestDTO", description = "DTO to pass a user ID for wallet operations")
public class UserIdRequestDTO {

    @Schema(description = "ID of the user", example = "123")
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
