package org.walletservice.wallet_service.controller.wallet;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(@RequestBody @Valid WalletRequestDTO request) {
        log.info("Creating wallet for userId={}", request.getUserId());
        WalletResponseDTO wallet = walletService.createWallet(request);
        return ResponseEntity.status(201).body(wallet);
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable Long walletId) {
        log.info("Fetching balance for walletId={}", walletId);
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponseDTO> getWalletDetails(@PathVariable Long walletId) {
        log.info("Fetching wallet details for walletId={}", walletId);
        return ResponseEntity.ok(walletService.getWalletDetails(walletId));
    }

    @GetMapping
    public ResponseEntity<List<WalletResponseDTO>> getAllWallets() {
        log.info("Fetching all wallets");
        List<WalletResponseDTO> wallets = walletService.getAllWallets();
        return ResponseEntity.ok(wallets);
    }
}
