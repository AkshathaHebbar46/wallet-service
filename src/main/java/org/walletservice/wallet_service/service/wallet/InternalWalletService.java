package org.walletservice.wallet_service.service.wallet;

import org.springframework.stereotype.Service;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

@Service
public class InternalWalletService {

    private final WalletRepository walletRepository;

    public InternalWalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Validates a wallet for internal operations.
     * Throws exceptions if wallet is not found or inactive.
     *
     * @param walletId the ID of the wallet to validate
     * @throws IllegalArgumentException if wallet not found
     * @throws IllegalStateException if wallet is inactive
     */
    public void validateWallet(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (Boolean.FALSE.equals(wallet.getActive())) {
            throw new IllegalStateException("Wallet inactive");
        }
    }
}
