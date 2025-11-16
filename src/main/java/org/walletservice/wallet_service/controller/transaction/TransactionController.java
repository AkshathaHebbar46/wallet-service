package org.walletservice.wallet_service.controller.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.walletservice.wallet_service.exception.UnauthorizedAccessException;
import org.walletservice.wallet_service.security.AuthContext;
import org.walletservice.wallet_service.validation.validator.AuthValidator;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.request.WalletTransferRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionType;
import org.walletservice.wallet_service.service.transaction.TransactionService;
import org.walletservice.wallet_service.service.wallet.WalletTransactionService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Wallet Transaction APIs", description = "Endpoints for wallet transactions and transfers")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final WalletTransactionService walletTransactionService;
    private final TransactionService transactionService;
    private final AuthValidator authValidator;

    public TransactionController(WalletTransactionService walletTransactionService,
                                 TransactionService transactionService,
                                 AuthValidator authValidator) {
        this.walletTransactionService = walletTransactionService;
        this.transactionService = transactionService;
        this.authValidator = authValidator;
    }

    @Operation(summary = "Process a CREDIT or DEBIT transaction", description = "Processes a single transaction (CREDIT or DEBIT) for a specific wallet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction processed successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping("/{walletId}")
    public ResponseEntity<WalletTransactionResponseDTO> processTransaction(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransactionRequestDTO request,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, walletId)) {
            throw new UnauthorizedAccessException("You are not allowed to access this wallet.");
        }

        log.info("Processing {} transaction for walletId={} amount={}", request.type(), walletId, request.amount());
        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Transfer money between wallets", description = "Transfers money from one wallet to another.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping("/transfer")
    public ResponseEntity<WalletTransactionResponseDTO> transferMoney(
            @Valid @RequestBody WalletTransferRequestDTO request,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, request.fromWalletId())) {
            throw new UnauthorizedAccessException("You are not allowed to access this wallet.");
        }

        log.info("Initiating transfer: {} → {} | amount={}",
                request.fromWalletId(), request.toWalletId(), request.amount());
        WalletTransactionResponseDTO response = walletTransactionService.transferMoney(
                request.fromWalletId(), request.toWalletId(), request.amount());
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "List all transactions of a wallet", description = "Retrieves all transactions for a given wallet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @GetMapping("/{walletId}/list")
    public ResponseEntity<List<WalletTransactionResponseDTO>> listTransactions(
            @PathVariable Long walletId,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, walletId)) {
            throw new UnauthorizedAccessException("You are not allowed to access this wallet.");
        }

        log.info("Fetching all transactions for walletId={}", walletId);
        List<WalletTransactionResponseDTO> transactions = walletTransactionService.listTransactions(walletId);
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get paginated and filtered transaction history", description = "Fetch transaction history for a wallet with optional filters and pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<WalletTransactionResponseDTO>> getTransactionHistory(
            @RequestParam Long walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, walletId)) {
            throw new UnauthorizedAccessException("You are not allowed to access this wallet.");
        }

        log.info("Fetching filtered transaction history for walletId={}", walletId);
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponseDTO> transactions = transactionService.getFilteredTransactions(
                walletId, type, startDate, endDate, pageable);

        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get all transactions for the current user", description = "Fetches all wallet transactions for the logged-in user with pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User transactions retrieved successfully")
    })
    @GetMapping("/all")
    public ResponseEntity<Page<WalletTransactionResponseDTO>> getAllUserTransactions(
            Pageable pageable,
            HttpServletRequest request
    ) {
        String token = authValidator.extractToken(request);
        Long userId = authValidator.extractUserId(token);
        return ResponseEntity.ok(transactionService.getAllUserTransactions(userId, pageable));
    }
}
