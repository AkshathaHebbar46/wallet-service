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
import org.walletservice.wallet_service.service.jwt.JwtService;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;
    private final JwtService jwtService;
    private final WalletRepository walletRepository;

    public WalletController(WalletService walletService, JwtService jwtService, WalletRepository walletRepository) {
        this.walletService = walletService;
        this.jwtService = jwtService;
        this.walletRepository = walletRepository;
    }

    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(
            @RequestBody @Valid WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        Long requesterUserId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        WalletResponseDTO wallet = walletService.createWallet(
                request,
                requesterUserId,
                isAdmin
        );

        return ResponseEntity.status(201).body(wallet);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponseDTO> getWalletDetails(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        Long requesterUserId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        WalletResponseDTO wallet = walletService.getWalletDetails(walletId, requesterUserId, isAdmin);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        Long requesterUserId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        Double balance = walletService.getBalance(walletId, requesterUserId, isAdmin);
        return ResponseEntity.ok(balance);
    }

    @PutMapping("/{walletId}/balance")
    public ResponseEntity<WalletResponseDTO> updateBalance(
            @PathVariable Long walletId,
            @RequestBody @Valid WalletRequestDTO request,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        Long requesterUserId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        WalletResponseDTO updatedWallet = walletService.updateBalance(
                walletId,
                request.getBalance(),
                requesterUserId,
                isAdmin
        );

        return ResponseEntity.ok(updatedWallet);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WalletResponseDTO>> getWalletsByUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        Long requesterUserId = jwtService.extractUserId(token);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        if (!isAdmin && !requesterUserId.equals(userId)) {
            return ResponseEntity.status(403).build(); // forbidden
        }

        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        List<WalletResponseDTO> dtoList = wallets.stream()
                .map(w -> new WalletResponseDTO(w.getId(), w.getUserId(), w.getBalance()))
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<WalletResponseDTO>> getAllWallets(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        boolean isAdmin = "ADMIN".equals(jwtService.extractRole(token));

        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }

        List<WalletResponseDTO> wallets = walletService.getAllWallets();
        return ResponseEntity.ok(wallets);
    }
}
