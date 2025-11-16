package org.walletservice.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletTransactionRequestDTO", description = "DTO for processing a wallet transaction")
public record WalletTransactionRequestDTO(

        @Schema(description = "Unique transaction identifier", example = "txn_12345")
        String transactionId,

        @Schema(description = "Amount for the transaction", example = "500.0", required = true)
        @ValidTransactionAmount
        Double amount,

        @Schema(description = "Transaction type (CREDIT or DEBIT)", example = "CREDIT", required = true)
        @NotBlank(message = "Transaction type is required")
        String type,

        @Schema(description = "Description of the transaction", example = "Payment for order #123", required = true)
        @NotBlank(message = "Transaction description is required")
        String description
) {}
