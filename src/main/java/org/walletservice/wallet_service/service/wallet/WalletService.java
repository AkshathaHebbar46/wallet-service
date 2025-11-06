package org.walletservice.wallet_service.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    // Create wallet with ownership/admin check
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WalletResponseDTO createWallet(WalletRequestDTO request, Long requesterUserId, boolean isAdmin) {
        if (!isAdmin && !request.getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("You can only create a wallet for yourself");
        }

        WalletEntity wallet = new WalletEntity(request.getUserId(), request.getBalance());
        WalletEntity saved = walletRepository.save(wallet);

        log.info("✅ Wallet created for userId={} with balance ₹{}", request.getUserId(), wallet.getBalance());
        return new WalletResponseDTO(saved.getId(), saved.getUserId(), saved.getBalance());
    }

    // Get wallet details with ownership/admin check
    @Transactional(readOnly = true)
    public WalletResponseDTO getWalletDetails(Long walletId, Long requesterUserId, boolean isAdmin) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!isAdmin && !wallet.getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("You do not have access to this wallet");
        }

        return new WalletResponseDTO(wallet.getId(), wallet.getUserId(), wallet.getBalance());
    }

    // Get balance with ownership/admin check
    @Transactional(readOnly = true)
    public Double getBalance(Long walletId, Long requesterUserId, boolean isAdmin) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!isAdmin && !wallet.getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("You do not have access to this wallet");
        }

        return wallet.getBalance();
    }

    // Update balance with ownership/admin check
    @Transactional
    public WalletResponseDTO updateBalance(Long walletId, Double newBalance, Long requesterUserId, boolean isAdmin) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!isAdmin && !wallet.getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("You cannot update this wallet");
        }

        wallet.setBalance(newBalance);
        WalletEntity saved = walletRepository.save(wallet);

        log.info("💰 Wallet {} balance updated to ₹{}", walletId, newBalance);
        return new WalletResponseDTO(saved.getId(), saved.getUserId(), saved.getBalance());
    }

    // Get all wallets (admin only)
    @Transactional(readOnly = true)
    public List<WalletResponseDTO> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(w -> new WalletResponseDTO(w.getId(), w.getUserId(), w.getBalance()))
                .collect(Collectors.toList());
    }
}
