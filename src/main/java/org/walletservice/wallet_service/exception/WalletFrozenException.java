package org.walletservice.wallet_service.exception;

public class WalletFrozenException extends RuntimeException {
    private final Long secondsLeft;

    public WalletFrozenException(String message, long secondsLeft) {
        super(message);
        this.secondsLeft = secondsLeft;
    }

    public Long getSecondsLeft() {
        return secondsLeft;
    }
}
