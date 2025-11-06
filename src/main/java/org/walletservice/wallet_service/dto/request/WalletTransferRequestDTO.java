package org.walletservice.wallet_service.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WalletTransferRequestDTO(
        @NotNull(message = "fromWalletId is required")
        Long fromWalletId,

        @NotNull(message = "toWalletId is required")
        Long toWalletId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Double amount
) {}
