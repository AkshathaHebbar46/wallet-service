package org.walletservice.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;

public record WalletTransactionRequestDTO(

        String transactionId,

        @ValidTransactionAmount
        Double amount,

        @NotBlank(message = "Transaction type is required")
        String type,

        @NotBlank(message = "Transaction description is required")
        String description
) {}
