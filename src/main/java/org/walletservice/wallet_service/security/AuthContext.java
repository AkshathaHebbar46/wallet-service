package org.walletservice.wallet_service.security;

public class AuthContext {
    private final String token;
    private final Long userId;
    private final boolean isAdmin;

    public AuthContext(String token, Long userId, boolean isAdmin) {
        this.token = token;
        this.userId = userId;
        this.isAdmin = isAdmin;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
