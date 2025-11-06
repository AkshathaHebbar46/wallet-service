package org.walletservice.wallet_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.walletservice.wallet_service.dto.response.ErrorResponseDTO;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // Wallet frozen
    @ExceptionHandler(WalletFrozenException.class)
    public ResponseEntity<ErrorResponseDTO> handleWalletFrozen(WalletFrozenException ex) {
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.LOCKED.value(),
                "Wallet Frozen",
                ex.getMessage(),
                ex.getSecondsLeft()
        );
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    // Daily limit or illegal operation
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.CONFLICT.value(),
                "Operation Not Allowed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Invalid arguments (Bad request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Validation errors (e.g., invalid transaction amount)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Generic fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
