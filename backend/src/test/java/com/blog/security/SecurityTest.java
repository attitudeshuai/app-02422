package com.blog.security;

import com.blog.utils.JwtUtil;
import com.blog.utils.XssUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testJwtTokenGeneration() {
        String token = jwtUtil.generateToken("testuser", 1L, 1);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testJwtTokenValidation() {
        String token = jwtUtil.generateToken("testuser", 1L, 1);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testJwtTokenExpiration() {
        String token = jwtUtil.generateToken("testuser", 1L, 1);
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void testJwtGetUsername() {
        String token = jwtUtil.generateToken("testuser", 1L, 1);
        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    void testJwtGetUserId() {
        String token = jwtUtil.generateToken("testuser", 123L, 1);
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(123L, userId);
    }

    @Test
    void testJwtGetRole() {
        String token = jwtUtil.generateToken("testuser", 1L, 0);
        Integer role = jwtUtil.getRoleFromToken(token);
        assertEquals(0, role);
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testXssClean() {
        String maliciousInput = "<script>alert('XSS')</script>";
        String cleaned = XssUtil.clean(maliciousInput);
        assertNotNull(cleaned);
        assertFalse(cleaned.contains("<script>"));
        assertTrue(cleaned.contains("&lt;script&gt;"));
    }

    @Test
    void testXssCleanNull() {
        String result = XssUtil.clean(null);
        assertNull(result);
    }

    @Test
    void testXssCleanEmpty() {
        String result = XssUtil.clean("");
        assertEquals("", result);
    }

    @Test
    void testXssCleanNormalText() {
        String normalText = "这是正常文本";
        String result = XssUtil.clean(normalText);
        assertEquals(normalText, result);
    }

    @Test
    void testXssCleanHtmlEntities() {
        String input = "<img src='x' onerror='alert(1)'>";
        String cleaned = XssUtil.clean(input);
        assertFalse(cleaned.contains("<img"));
        assertTrue(cleaned.contains("&lt;img"));
    }
}
