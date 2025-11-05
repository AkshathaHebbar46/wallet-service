package org.walletservice.wallet_service.dto.response;

import java.time.LocalDateTime;

/**
 * DTO representing the response after processing a wallet transaction.
 */
public record WalletTransactionResponseDTO(
        String transactionId,
        Double amount,
        String type,
        LocalDateTime timestamp,
        String description
) {
    // Automatically set timestamp if not provided
    public WalletTransactionResponseDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
