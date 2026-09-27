package com.ubb.practicas.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "superSecretKeyForTestingJwtTokensWhichMustBeAtLeast256BitsLong12345!";
    private static final String OTHER_SECRET = "anotherSuperSecretKeyForTestingJwtTokensWhichMustBeAtLeast256Bits123!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L); // 1 hour
    }

    @Test
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken("student@ubb.cl", "Estudiante");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void testExtractEmailAndRole_Success() {
        String token = jwtUtil.generateToken("student@ubb.cl", "Estudiante");

        assertEquals("student@ubb.cl", jwtUtil.extractEmail(token));
        assertEquals("Estudiante", jwtUtil.extractRole(token));
    }

    @Test
    void testExtractClaims_ExpirationInFuture() {
        String token = jwtUtil.generateToken("student@ubb.cl", "Estudiante");

        Date expiration = jwtUtil.extractClaims(token).getExpiration();
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testTokenExpiration_Rejection() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -5000L);
        String expiredToken = jwtUtil.generateToken("user@ubb.cl", "Estudiante");

        assertFalse(jwtUtil.isTokenValid(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractClaims(expiredToken));
    }

    @Test
    void testTamperedSignature_Rejection() {
        String token = jwtUtil.generateToken("student@ubb.cl", "Estudiante");
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtUtil.isTokenValid(tampered));
        assertThrows(JwtException.class, () -> jwtUtil.extractClaims(tampered));
    }

    @Test
    void testTokenSignedWithDifferentSecret_Rejection() {
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", OTHER_SECRET);
        ReflectionTestUtils.setField(otherJwtUtil, "expirationMs", 3600000L);

        String tokenFromOther = otherJwtUtil.generateToken("student@ubb.cl", "Estudiante");

        assertFalse(jwtUtil.isTokenValid(tokenFromOther));
        assertThrows(JwtException.class, () -> jwtUtil.extractClaims(tokenFromOther));
    }

    @Test
    void testMalformedToken_Rejection() {
        assertFalse(jwtUtil.isTokenValid("invalid.token"));
        assertFalse(jwtUtil.isTokenValid("not-a-token"));
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    void testIsTokenValid_NullToken_ReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid(null));
    }
}
