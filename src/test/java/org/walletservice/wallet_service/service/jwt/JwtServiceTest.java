package org.walletservice.wallet_service.service.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private Key signingKey;
    private String secret;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Generate a stable secret (must be >= 256 bits for HS256)
        secret = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef0123456789ABCDEF";
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());

        // Inject secret field using reflection (since @Value not loaded in unit tests)
        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, secret);
    }

    // ───────────────────────────────────────────────
    private String generateToken(Map<String, Object> claims, String subject, long validityMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + validityMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("✅ Should validate a well-formed token successfully")
    void testValidToken() {
        String token = generateToken(Map.of("userId", 100L, "role", "USER"), "test@example.com", 3600_000);

        assertTrue(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("❌ Should reject an invalid or tampered token")
    void testInvalidToken() {
        String token = "invalid.token.value";
        assertFalse(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("⚠️ Should reject token with wrong signing key")
    void testInvalidSignature() throws Exception {
        // Create token signed with different secret
        Key badKey = Keys.hmacShaKeyFor("different-secret-different-secret-diff1234".getBytes());
        String token = Jwts.builder()
                .setSubject("fake@example.com")
                .signWith(badKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("⌛ Should reject expired tokens")
    void testExpiredToken() {
        String token = generateToken(Map.of("userId", 200L), "expired@example.com", -1000); // already expired
        assertFalse(jwtService.isTokenValid(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("📧 Should correctly extract subject (email)")
    void testExtractEmail() {
        String token = generateToken(Map.of("userId", 123L, "role", "ADMIN"), "hello@wallet.com", 60000);
        String email = jwtService.extractEmail(token);
        assertEquals("hello@wallet.com", email);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧾 Should extract userId correctly as Long")
    void testExtractUserId_Long() {
        String token = generateToken(Map.of("userId", 999L), "id@test.com", 60000);
        Long userId = jwtService.extractUserId(token);
        assertEquals(999L, userId);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🔢 Should handle userId as Integer in claims")
    void testExtractUserId_Integer() {
        String token = generateToken(Map.of("userId", 42), "id2@test.com", 60000);
        Long userId = jwtService.extractUserId(token);
        assertEquals(42L, userId);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧍 Should extract role correctly")
    void testExtractRole() {
        String token = generateToken(Map.of("userId", 12L, "role", "ADMIN"), "role@test.com", 60000);
        String role = jwtService.extractRole(token);
        assertEquals("ADMIN", role);
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🌀 Should return null for missing userId or role claims")
    void testMissingClaims() {
        String token = generateToken(Map.of(), "missing@test.com", 60000);
        assertNull(jwtService.extractUserId(token));
        assertNull(jwtService.extractRole(token));
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("🧨 Should throw exception gracefully for malformed tokens")
    void testExtractFromMalformedToken() {
        String invalidToken = "not.a.jwt.token";
        assertFalse(jwtService.isTokenValid(invalidToken));
        assertThrows(Exception.class, () -> jwtService.extractEmail(invalidToken));
    }
}
