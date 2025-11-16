package org.walletservice.wallet_service.dto.response;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponseDTO", description = "Standard error response DTO")
public class ErrorResponseDTO {

    @Schema(description = "Timestamp of the error", example = "2025-11-14T10:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code of the error", example = "404")
    private int status;

    @Schema(description = "Error type or title", example = "Not Found")
    private String error;

    @Schema(description = "Detailed error message", example = "Wallet with ID 101 not found")
    private String message;

    public ErrorResponseDTO() {}

    public ErrorResponseDTO(LocalDateTime timestamp, int status, String error, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // Static builder for convenience
    public static ErrorResponseDTO of(int status, String error, String message) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, message);
    }

    public static ErrorResponseDTO of(int status, String error, String message, Long secondsLeft) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, message);
    }

    // Getters & Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

}
