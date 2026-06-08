package com.blog.security;

import com.blog.dto.LoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SQL注入防护测试
 * 验证MyBatis-Plus参数化查询是否有效防止SQL注入
 */
@SpringBootTest
@AutoConfigureMockMvc
class SqlInjectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("user");
        loginDTO.setPassword("user123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(response)
                .get("data").get("token").asText();
    }

    @Test
    void testSqlInjectionInLogin() throws Exception {
        // 尝试SQL注入绕过登录
        String[] sqlInjectionPayloads = {
            "admin' OR '1'='1",
            "admin' OR '1'='1' --",
            "admin' OR '1'='1' /*",
            "' OR 1=1 --",
            "admin'--",
            "' OR 'x'='x",
            "admin' AND 1=0 UNION ALL SELECT 'admin', '81dc9bdb52d04dc20036dbd8313ed055'",
            "1' UNION SELECT NULL, NULL, NULL--"
        };

        for (String payload : sqlInjectionPayloads) {
            LoginDTO maliciousLogin = new LoginDTO();
            maliciousLogin.setUsername(payload);
            maliciousLogin.setPassword("anything");

            // SQL注入应该失败（返回错误或401）
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(maliciousLogin)))
                    .andExpect(status().is5xxServerError()); // 登录失败
        }
    }

    @Test
    void testSqlInjectionInArticleSearch() throws Exception {
        // 尝试在搜索中注入SQL
        String[] sqlInjectionPayloads = {
            "' OR '1'='1",
            "'; DROP TABLE article; --",
            "' UNION SELECT * FROM user --",
            "1' AND 1=0 UNION ALL SELECT NULL, username, password FROM user--",
            "' OR 1=1 LIMIT 1 --"
        };

        for (String payload : sqlInjectionPayloads) {
            // SQL注入应该被安全处理（不会导致错误或数据泄露）
            mockMvc.perform(get("/api/articles/search")
                    .param("keyword", payload))
                    .andExpect(status().isOk()); // 应该正常返回（空结果或安全结果）
        }
    }

    @Test
    void testSqlInjectionInUserList() throws Exception {
        // 获取管理员Token
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // 尝试在用户列表搜索中注入SQL
        String[] sqlInjectionPayloads = {
            "admin' OR '1'='1",
            "' UNION SELECT * FROM article --",
            "'; UPDATE user SET role=0 WHERE username='user'; --"
        };

        for (String payload : sqlInjectionPayloads) {
            mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("keyword", payload))
                    .andExpect(status().isOk()); // 应该安全处理
        }
    }

    @Test
    void testSqlInjectionInArticleTitle() throws Exception {
        // 尝试在文章标题中注入SQL
        String sqlPayload = "'; DROP TABLE article; --";
        String articleJson = String.format(
            "{\"title\":\"%s\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}",
            sqlPayload
        );

        // 应该成功创建（SQL被当作普通字符串处理）
        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk());

        // 验证数据库表仍然存在（通过查询文章列表）
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionInCategoryName() throws Exception {
        // 获取管理员Token
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // 尝试在分类名中注入SQL
        String sqlPayload = "'; DELETE FROM category WHERE '1'='1";
        String categoryJson = String.format(
            "{\"name\":\"%s\",\"description\":\"测试\"}",
            sqlPayload
        );

        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isOk());

        // 验证分类表仍然存在
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionInComment() throws Exception {
        // 先创建文章
        String articleJson = "{\"title\":\"测试\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        MvcResult articleResult = mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andReturn();

        Long articleId = objectMapper.readTree(articleResult.getResponse().getContentAsString())
                .get("data").asLong();

        // 尝试在评论中注入SQL
        String sqlPayload = "'; UPDATE article SET like_count=99999 WHERE id=" + articleId + "; --";
        String commentJson = String.format(
            "{\"articleId\":%d,\"content\":\"%s\"}",
            articleId, sqlPayload
        );

        mockMvc.perform(post("/api/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson))
                .andExpect(status().isOk());

        // 验证文章点赞数未被修改
        MvcResult getResult = mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(status().isOk())
                .andReturn();

        String response = getResult.getResponse().getContentAsString();
        int likeCount = objectMapper.readTree(response)
                .get("data").get("likeCount").asInt();

        // 点赞数应该是0，不是99999
        assert likeCount != 99999 : "SQL注入成功修改了数据！";
    }

    @Test
    void testBlindSqlInjection() throws Exception {
        // 测试盲注
        String[] blindSqlPayloads = {
            "admin' AND SLEEP(5) --",
            "' OR IF(1=1, SLEEP(5), 0) --",
            "' AND (SELECT * FROM (SELECT(SLEEP(5)))a) --"
        };

        for (String payload : blindSqlPayloads) {
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/articles/search")
                    .param("keyword", payload))
                    .andExpect(status().isOk());
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 如果SQL注入成功，会延迟5秒
            assert duration < 3000 : "可能存在盲注漏洞，查询时间过长: " + duration + "ms";
        }
    }
}
