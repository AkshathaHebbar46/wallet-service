package org.walletservice.wallet_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.walletservice.wallet_service.dto.response.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle unauthorized wallet access
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Wallet frozen
    @ExceptionHandler(WalletFrozenException.class)
    public ResponseEntity<ErrorResponseDTO> handleWalletFrozen(WalletFrozenException ex) {
        log.warn("Wallet frozen: {} seconds left", ex.getSecondsLeft());
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.LOCKED.value(),
                "Wallet Frozen",
                ex.getMessage(),
                ex.getSecondsLeft()
        );
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    // Illegal or invalid state
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal operation attempted: {}", ex.getMessage());
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.CONFLICT.value(),
                "Operation Not Allowed",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Bad request (invalid args)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Validation error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation failed: {}", message);
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Fallback for unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        log.error("Unexpected exception: ", ex);
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Wallet not found
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleWalletNotFound(WalletNotFoundException ex) {
        log.warn("Wallet not found: {}", ex.getMessage());
        ErrorResponseDTO error = ErrorResponseDTO.of(
                HttpStatus.NOT_FOUND.value(),
                "Wallet Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

}
