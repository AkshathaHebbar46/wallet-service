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
    private final WalletValidationService walletValidationService;

    public WalletService(WalletRepository walletRepository,
                         WalletValidationService walletValidationService) {
        this.walletRepository = walletRepository;
        this.walletValidationService = walletValidationService;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WalletResponseDTO createWallet(WalletRequestDTO request) {
        WalletEntity wallet = new WalletEntity(request.getUserId(), request.getBalance());
        WalletEntity saved = walletRepository.save(wallet);

        log.info("✅ Wallet created for userId={} with balance ₹{}", request.getUserId(), wallet.getBalance());
        return new WalletResponseDTO(saved.getId(), saved.getUserId(), saved.getBalance());
    }

    @Transactional(readOnly = true)
    public WalletResponseDTO getWalletDetails(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        walletValidationService.validateWalletState(wallet);
        return new WalletResponseDTO(wallet.getId(), wallet.getUserId(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public List<WalletResponseDTO> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(w -> new WalletResponseDTO(w.getId(), w.getUserId(), w.getBalance()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getBalance(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"))
                .getBalance();
    }
}
