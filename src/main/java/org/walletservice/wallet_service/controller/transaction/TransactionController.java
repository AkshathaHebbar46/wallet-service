package org.walletservice.wallet_service.controller.transaction;

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

    // 🔒 Process CREDIT or DEBIT
    @PostMapping("/{walletId}")
    public ResponseEntity<WalletTransactionResponseDTO> processTransaction(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransactionRequestDTO request,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, walletId)) {
            return ResponseEntity.status(403).build();
        }

        log.info("Processing {} transaction for walletId={} amount={}", request.type(), walletId, request.amount());
        WalletTransactionResponseDTO response = walletTransactionService.processTransaction(walletId, request);
        return ResponseEntity.status(201).body(response);
    }

    // 🔒 Transfer between wallets
    @PostMapping("/transfer")
    public ResponseEntity<WalletTransactionResponseDTO> transferMoney(
            @Valid @RequestBody WalletTransferRequestDTO request,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, request.fromWalletId())) {
            return ResponseEntity.status(403).build();
        }

        log.info("Initiating transfer: {} → {} | amount={}",
                request.fromWalletId(), request.toWalletId(), request.amount());
        WalletTransactionResponseDTO response = walletTransactionService.transferMoney(
                request.fromWalletId(), request.toWalletId(), request.amount());
        return ResponseEntity.status(201).body(response);
    }

    // 🔒 List all transactions of a wallet
    @GetMapping("/{walletId}/list")
    public ResponseEntity<List<WalletTransactionResponseDTO>> listTransactions(
            @PathVariable Long walletId,
            HttpServletRequest servletRequest) {

        AuthContext auth = authValidator.getAuthContext(servletRequest);
        if (!authValidator.isAuthorizedForWallet(auth, walletId)) {
            return ResponseEntity.status(403).build();
        }

        log.info("Fetching all transactions for walletId={}", walletId);
        List<WalletTransactionResponseDTO> transactions = walletTransactionService.listTransactions(walletId);
        return ResponseEntity.ok(transactions);
    }

    // 🔒 Paginated & filtered transaction history
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
            return ResponseEntity.status(403).build();
        }

        log.info("Fetching filtered transaction history for walletId={}", walletId);
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponseDTO> transactions = transactionService.getFilteredTransactions(
                walletId, type, startDate, endDate, pageable);

        return ResponseEntity.ok(transactions);
    }
}
