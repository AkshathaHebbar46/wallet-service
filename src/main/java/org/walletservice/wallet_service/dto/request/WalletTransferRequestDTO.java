package org.walletservice.wallet_service.dto.request;

/**
 * DTO for transferring funds between two wallets.
 */
public record WalletTransferRequestDTO(
        Long fromWalletId,
        Long toWalletId,
        Double amount
) {}
