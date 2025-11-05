package org.walletservice.wallet_service.dto.response;

import java.time.LocalDateTime;

public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Long secondsLeft; // optional — only for frozen wallet case

    public ErrorResponseDTO() {}

    public ErrorResponseDTO(LocalDateTime timestamp, int status, String error, String message, Long secondsLeft) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.secondsLeft = secondsLeft;
    }

    // ✅ Static builder for convenience
    public static ErrorResponseDTO of(int status, String error, String message) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, message,null);
    }

    public static ErrorResponseDTO of(int status, String error, String message, Long secondsLeft) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, message, secondsLeft);
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

    public Long getSecondsLeft() { return secondsLeft; }
    public void setSecondsLeft(Long secondsLeft) { this.secondsLeft = secondsLeft; }
}
