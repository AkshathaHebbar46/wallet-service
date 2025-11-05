package org.walletservice.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;

/**
 * DTO for incoming transaction requests to the Wallet microservice.
 * Used for debit, credit, or transfer operations.
 */
public record WalletTransactionRequestDTO(

        String transactionId,

        @ValidTransactionAmount
        Double amount,

        @NotBlank(message = "Transaction type is required")
        String type,

        @NotBlank(message = "Transaction description is required")
        String description
) {}
