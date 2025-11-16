package org.walletservice.wallet_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;

@Schema(name = "WalletTransferRequestDTO", description = "DTO for transferring money between wallets")
public record WalletTransferRequestDTO(

        @Schema(description = "Wallet ID from which the money will be debited", example = "101", required = true)
        @NotNull(message = "fromWalletId is required")
        Long fromWalletId,

        @Schema(description = "Wallet ID to which the money will be credited", example = "102", required = true)
        @NotNull(message = "toWalletId is required")
        Long toWalletId,

        @Schema(description = "Amount to transfer", example = "250.0", required = true)
        @NotNull(message = "Amount is required")
        @ValidTransactionAmount
        Double amount
) {}
