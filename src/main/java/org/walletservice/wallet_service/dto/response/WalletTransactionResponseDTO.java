package org.walletservice.wallet_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO representing the response after processing a wallet transaction.
 */
@Schema(name = "WalletTransactionResponseDTO", description = "Details of a wallet transaction")
public record WalletTransactionResponseDTO(
        @Schema(description = "Unique identifier of the transaction", example = "txn_12345")
        String transactionId,

        @Schema(description = "Amount involved in the transaction", example = "150.75")
        Double amount,

        @Schema(description = "Type of transaction (CREDIT or DEBIT)", example = "CREDIT")
        String type,

        @Schema(description = "Timestamp when the transaction occurred")
        LocalDateTime timestamp,

        @Schema(description = "Description of the transaction", example = "Payment for order #123")
        String description,

        @Schema(description = "Balance of the wallet after the transaction", example = "1200.50")
        double balance,

        @Schema(description = "Remaining daily limit for transactions", example = "5000.0")
        double availableDailyLimit
) {
    // Automatically set timestamp if not provided
    public WalletTransactionResponseDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
