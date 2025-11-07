package org.walletservice.wallet_service.controller.wallet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.dto.request.WalletRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.repository.wallet.WalletRepository;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final AuthValidator authValidator;

    public WalletController(WalletService walletService,
                            WalletRepository walletRepository,
                            AuthValidator authValidator) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.authValidator = authValidator;
    }

    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(
            @Valid @RequestBody WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        WalletResponseDTO wallet = walletService.createWallet(request, auth.getUserId(), auth.isAdmin());
        return ResponseEntity.status(201).body(wallet);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponseDTO> getWalletDetails(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        WalletResponseDTO wallet = walletService.getWalletDetails(walletId, auth.getUserId(), auth.isAdmin());
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        Double balance = walletService.getBalance(walletId, auth.getUserId(), auth.isAdmin());
        return ResponseEntity.ok(balance);
    }

    @PutMapping("/{walletId}/balance")
    public ResponseEntity<WalletResponseDTO> updateBalance(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        WalletResponseDTO updatedWallet = walletService.updateBalance(
                walletId,
                request.getBalance(),
                auth.getUserId(),
                auth.isAdmin()
        );
        return ResponseEntity.ok(updatedWallet);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WalletResponseDTO>> getWalletsByUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);
        if (!authValidator.isAuthorized(auth, userId)) {
            return ResponseEntity.status(403).build();
        }

        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        List<WalletResponseDTO> dtoList = wallets.stream()
                .map(w -> new WalletResponseDTO(w.getId(), w.getUserId(), w.getBalance()))
                .toList();

        return ResponseEntity.ok(dtoList);
    }
}
