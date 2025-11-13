package org.walletservice.wallet_service.controller.wallet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final AuthValidator authValidator;

    public WalletController(WalletService walletService,
                            AuthValidator authValidator) {
        this.walletService = walletService;
        this.authValidator = authValidator;
    }

    // ---------------- CREATE WALLET ----------------
    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(
            @Valid @RequestBody WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        log.info("User {} requested to create a new wallet", auth.getUserId());

        WalletResponseDTO wallet = walletService.createWallet(request, auth.getUserId(), auth.isAdmin());

        log.info("Wallet creation request processed for user {}", auth.getUserId());
        return ResponseEntity.status(201).body(wallet);
    }

    // ---------------- GET WALLET DETAILS ----------------
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponseDTO> getWalletDetails(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        log.info("Fetching wallet details for walletId={} requested by userId={}", walletId, auth.getUserId());

        WalletResponseDTO wallet = walletService.getWalletDetails(walletId, auth.getUserId(), auth.isAdmin());

        log.info("Wallet details returned for walletId={}", walletId);
        return ResponseEntity.ok(wallet);
    }

    // ---------------- GET WALLET BALANCE ----------------
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        log.info("Fetching balance for walletId={} requested by userId={}", walletId, auth.getUserId());

        Double balance = walletService.getBalance(walletId, auth.getUserId(), auth.isAdmin());

        log.info("Balance fetched for walletId={}", walletId);
        return ResponseEntity.ok(balance);
    }

    // ---------------- UPDATE WALLET BALANCE ----------------
    @PutMapping("/{walletId}/balance")
    public ResponseEntity<WalletResponseDTO> updateBalance(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        log.info("Updating balance for walletId={} requested by userId={}", walletId, auth.getUserId());

        WalletResponseDTO updatedWallet = walletService.updateBalance(
                walletId,
                request.getBalance(),
                auth.getUserId(),
                auth.isAdmin()
        );

        log.info("Update balance request processed for walletId={}", walletId);
        return ResponseEntity.ok(updatedWallet);
    }

    // ---------------- GET ALL WALLETS FOR CURRENT USER ----------------
    @GetMapping("/all")
    public ResponseEntity<List<WalletResponseDTO>> getWalletsForCurrentUser(HttpServletRequest httpRequest) {
        AuthContext auth = authValidator.getAuthContext(httpRequest);
        log.info("Fetching wallets for userId={} (isAdmin={})", auth.getUserId(), auth.isAdmin());

        List<WalletResponseDTO> wallets;

        if (auth.isAdmin()) {
            // Admins can see all wallets
            wallets = walletService.getAllWallets();
            log.info("Admin fetched all wallets. Total wallets={}", wallets.size());
        } else {
            // Normal users see only their own wallets
            wallets = walletService.getWalletsByUser(auth.getUserId());
            log.info("User {} fetched {} wallets", auth.getUserId(), wallets.size());
        }

        return ResponseEntity.ok(wallets);
    }
}
