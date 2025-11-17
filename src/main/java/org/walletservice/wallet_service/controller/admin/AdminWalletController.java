package org.walletservice.wallet_service.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.dto.request.UserIdRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletResponseDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.service.wallet.WalletFreezeService;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.service.wallet.WalletService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Wallet APIs", description = "Endpoints for admin operations on wallets")
public class AdminWalletController {

    private static final Logger log = LoggerFactory.getLogger(AdminWalletController.class);

    private final WalletService walletService;
    private final AuthValidator authValidator;
    private final WalletFreezeService walletFreezeService;
    private final WalletTransactionService walletTransactionService;


    public AdminWalletController(WalletService walletService, AuthValidator authValidator, WalletFreezeService walletFreezeService, WalletTransactionService walletTransactionService) {
        this.walletService = walletService;
        this.authValidator = authValidator;
        this.walletFreezeService = walletFreezeService;
        this.walletTransactionService = walletTransactionService;
    }

    @Operation(summary = "Get all wallets for a user", description = "Fetches all wallets associated with a given user ID. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "No wallets found for the user"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping("/all-wallets")
    public ResponseEntity<List<WalletResponseDTO>> getAllWalletsForUser(
            @RequestBody UserIdRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);

        if (!auth.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        Long userId = request.getUserId();
        log.info("Admin fetching wallets for userId={}", userId);

        List<WalletResponseDTO> wallets = walletService.getWalletsByUser(userId);

        if (wallets.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(wallets);
    }

    @Operation(summary = "Freeze a wallet", description = "Freeze a wallet by ID. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet frozen successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<String> freezeWallet(@PathVariable Long walletId, HttpServletRequest httpRequest) {
        AuthContext auth = authValidator.getAuthContext(httpRequest);
        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        WalletEntity wallet = walletService.getWalletById(walletId);
        walletFreezeService.freezeWallet(wallet);

        log.info("Wallet {} frozen by admin", walletId);
        return ResponseEntity.ok("Wallet " + walletId + " frozen successfully");
    }

    @Operation(summary = "Unfreeze a wallet", description = "Unfreeze a wallet by ID. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet unfrozen successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<String> unfreezeWallet(@PathVariable Long walletId, HttpServletRequest httpRequest) {
        AuthContext auth = authValidator.getAuthContext(httpRequest);
        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        WalletEntity wallet = walletService.getWalletById(walletId);
        walletFreezeService.unfreezeWallet(wallet);

        log.info("Wallet {} unfrozen by admin", walletId);
        return ResponseEntity.ok("Wallet " + walletId + " unfrozen successfully");
    }

    @Operation(summary = "Get wallet transactions", description = "Retrieve all transactions for a specific wallet. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Wallet or transactions not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @GetMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> getWalletTransactions(
            @PathVariable Long walletId,
            HttpServletRequest httpRequest) {

        AuthContext auth = authValidator.getAuthContext(httpRequest);

        if (!auth.isAdmin()) {
            throw new UnauthorizedAccessException("You are not authorized to perform this action");
        }

        log.info("Admin fetching transactions for walletId={}", walletId);

        List<WalletTransactionResponseDTO> transactions = walletTransactionService.listTransactions(walletId);

        if (transactions.isEmpty()) {
            throw new org.walletservice.wallet_service.exception.WalletNotFoundException(
                    "No transactions found for wallet " + walletId
            );
        }

        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Delete all wallets for a user", description = "Delete all wallets associated with a user ID. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Wallets deleted successfully")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/wallets")
    public ResponseEntity<Void> deleteWalletsForUser(@RequestBody UserIdRequestDTO requestDTO) {
        Long userId = requestDTO.getUserId();
        walletService.deleteWalletsForUser(userId);
        return ResponseEntity.noContent().build();
    }

}
