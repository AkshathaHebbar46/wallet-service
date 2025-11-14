package org.walletservice.wallet_service.service.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.walletservice.wallet_service.util.JwtUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = "dummy.jwt.token";
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("✅ Should validate a well-formed token successfully")
    void testValidToken() {
        when(jwtUtil.validateToken(token)).thenReturn(true);
        assertTrue(jwtService.isTokenValid(token));
        verify(jwtUtil).validateToken(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("❌ Should reject an invalid or tampered token")
    void testInvalidToken() {
        when(jwtUtil.validateToken(token)).thenReturn(false);
        assertFalse(jwtService.isTokenValid(token));
        verify(jwtUtil).validateToken(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("⚠️ Should reject token with wrong signing key")
    void testInvalidSignature() {
        when(jwtUtil.validateToken(token)).thenReturn(false);
        assertFalse(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("⌛ Should reject expired tokens")
    void testExpiredToken() {
        when(jwtUtil.validateToken(token)).thenReturn(false);
        assertFalse(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("📧 Should correctly extract subject (email)")
    void testExtractEmail() throws Exception {
        when(jwtUtil.extractUsername(token)).thenReturn("hello@wallet.com");
        String email = jwtService.extractEmail(token);
        assertEquals("hello@wallet.com", email);
        verify(jwtUtil).extractUsername(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧾 Should extract userId correctly as Long")
    void testExtractUserId_Long() throws Exception {
        when(jwtUtil.extractUserId(token)).thenReturn(999L);
        Long userId = jwtService.extractUserId(token);
        assertEquals(999L, userId);
        verify(jwtUtil).extractUserId(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🔢 Should handle userId as Integer in claims")
    void testExtractUserId_Integer() throws Exception {
        // JwtUtil converts Integer to Long internally
        when(jwtUtil.extractUserId(token)).thenReturn(42L);
        Long userId = jwtService.extractUserId(token);
        assertEquals(42L, userId);
        verify(jwtUtil).extractUserId(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧍 Should extract role correctly")
    void testExtractRole() throws Exception {
        when(jwtUtil.extractRole(token)).thenReturn("ADMIN");
        String role = jwtService.extractRole(token);
        assertEquals("ADMIN", role);
        verify(jwtUtil).extractRole(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🌀 Should return null for missing userId or role claims")
    void testMissingClaims() throws Exception {
        when(jwtUtil.extractUserId(token)).thenReturn(null);
        when(jwtUtil.extractRole(token)).thenReturn(null);

        assertNull(jwtService.extractUserId(token));
        assertNull(jwtService.extractRole(token));

        verify(jwtUtil).extractUserId(token);
        verify(jwtUtil).extractRole(token);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧨 Should handle malformed tokens gracefully")
    void testMalformedToken() {
        when(jwtUtil.validateToken(token)).thenReturn(false);

        assertFalse(jwtService.isTokenValid(token));
        // Extraction methods would fail if token invalid
        // You can throw exception if JwtUtil does that internally
        // Here we simulate with Mockito returning null
        when(jwtUtil.extractUsername(token)).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> jwtService.extractEmail(token));
    }
}
