package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;

@Service
public class WalletBlacklistService {

    private final WalletRepository walletRepository;

    public WalletBlacklistService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public void blacklistUserWallets(Long userId) {
        walletRepository.findByUserId(userId).forEach(w -> w.setActive(false));
    }

    @Transactional
    public void unblockUserWallets(Long userId) {
        walletRepository.findByUserId(userId).forEach(w -> w.setActive(true));
    }

    @Transactional
    public void blacklistWallet(Long walletId) {
        walletRepository.findById(walletId).ifPresent(w -> w.setActive(false));
    }

    @Transactional
    public void unblockWallet(Long walletId) {
        walletRepository.findById(walletId).ifPresent(w -> w.setActive(true));
    }
}
