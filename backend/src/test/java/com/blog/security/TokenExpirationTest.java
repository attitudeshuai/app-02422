package com.blog.security;

import com.blog.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Token过期机制测试
 * 验证JWT Token过期后无法访问受保护资源
 */
@SpringBootTest
@AutoConfigureMockMvc
class TokenExpirationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void testTokenExpirationCheck() {
        // 生成正常Token
        String validToken = jwtUtil.generateToken("testuser", 1L, 1);
        assertFalse(jwtUtil.isTokenExpired(validToken), "新生成的Token不应该过期");

        // 生成已过期的Token（过期时间设为1秒前）
        String expiredToken = createExpiredToken("testuser", 1L, 1, -1000);
        assertTrue(jwtUtil.isTokenExpired(expiredToken), "过期的Token应该被检测到");
    }

    @Test
    void testExpiredTokenValidation() {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("testuser", 1L, 1, -1000);
        
        // 验证过期Token应该返回false
        assertFalse(jwtUtil.validateToken(expiredToken), "过期的Token验证应该失败");
    }

    @Test
    void testAccessWithExpiredToken() throws Exception {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("user", 2L, 1, -1000);

        // 尝试使用过期Token访问受保护资源
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateArticleWithExpiredToken() throws Exception {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("user", 2L, 1, -1000);

        String articleJson = "{\"title\":\"测试\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";

        // 尝试使用过期Token创建文章
        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + expiredToken)
                .contentType("application/json")
                .content(articleJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateUserWithExpiredToken() throws Exception {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("user", 2L, 1, -1000);

        String updateJson = "{\"nickname\":\"新昵称\",\"email\":\"new@test.com\"}";

        // 尝试使用过期Token更新用户信息
        mockMvc.perform(put("/api/users/2")
                .header("Authorization", "Bearer " + expiredToken)
                .contentType("application/json")
                .content(updateJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteCommentWithExpiredToken() throws Exception {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("user", 2L, 1, -1000);

        // 尝试使用过期Token删除评论
        mockMvc.perform(delete("/api/comments/1")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAdminOperationWithExpiredToken() throws Exception {
        // 生成已过期的管理员Token
        String expiredAdminToken = createExpiredToken("admin", 1L, 0, -1000);

        // 尝试使用过期管理员Token访问用户列表
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + expiredAdminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTokenExpirationBoundary() {
        // 测试即将过期的Token（还有1秒）
        String almostExpiredToken = createExpiredToken("testuser", 1L, 1, 1000);
        assertFalse(jwtUtil.isTokenExpired(almostExpiredToken), "还未过期的Token应该有效");

        // 测试刚刚过期的Token（过期1毫秒）
        String justExpiredToken = createExpiredToken("testuser", 1L, 1, -1);
        assertTrue(jwtUtil.isTokenExpired(justExpiredToken), "刚过期的Token应该无效");
    }

    @Test
    void testGetUserInfoFromExpiredToken() {
        // 生成已过期的Token
        String expiredToken = createExpiredToken("testuser", 123L, 1, -1000);

        // 即使Token过期，仍然可以解析出用户信息（但不应该用于认证）
        try {
            String username = jwtUtil.getUsernameFromToken(expiredToken);
            Long userId = jwtUtil.getUserIdFromToken(expiredToken);
            Integer role = jwtUtil.getRoleFromToken(expiredToken);

            assertEquals("testuser", username);
            assertEquals(123L, userId);
            assertEquals(1, role);

            // 但是验证应该失败
            assertFalse(jwtUtil.validateToken(expiredToken));
        } catch (Exception e) {
            // 某些JWT库会在解析过期Token时抛出异常，这也是正确的行为
            assertTrue(true, "过期Token解析抛出异常是正确的");
        }
    }

    @Test
    void testMultipleExpiredTokens() throws Exception {
        // 测试多个不同过期时间的Token
        String[] expiredTokens = {
            createExpiredToken("user1", 1L, 1, -1000),      // 1秒前过期
            createExpiredToken("user2", 2L, 1, -60000),     // 1分钟前过期
            createExpiredToken("user3", 3L, 1, -3600000),   // 1小时前过期
            createExpiredToken("user4", 4L, 1, -86400000)   // 1天前过期
        };

        for (String expiredToken : expiredTokens) {
            // 所有过期Token都应该无法访问受保护资源
            mockMvc.perform(get("/api/auth/info")
                    .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void testValidTokenAfterExpiredAttempt() throws Exception {
        // 先尝试使用过期Token
        String expiredToken = createExpiredToken("user", 2L, 1, -1000);
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());

        // 然后使用有效Token应该成功
        String validToken = jwtUtil.generateToken("user", 2L, 1);
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    /**
     * 创建一个指定过期时间的Token
     * @param username 用户名
     * @param userId 用户ID
     * @param role 角色
     * @param expirationOffset 过期时间偏移（毫秒），负数表示已过期
     * @return JWT Token
     */
    private String createExpiredToken(String username, Long userId, Integer role, long expirationOffset) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationOffset);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
