package com.blog.controller;

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

@SpringBootTest
@AutoConfigureMockMvc
class PermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 获取管理员Token
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminResponse = adminResult.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminResponse)
                .get("data").get("token").asText();

        // 获取普通用户Token
        LoginDTO userLogin = new LoginDTO();
        userLogin.setUsername("user");
        userLogin.setPassword("user123");

        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String userResponse = userResult.getResponse().getContentAsString();
        userToken = objectMapper.readTree(userResponse)
                .get("data").get("token").asText();
    }

    @Test
    void testAccessWithoutToken() throws Exception {
        // 未登录访问需要认证的接口
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAdminAccessUserList() throws Exception {
        // 管理员可以访问用户列表
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testUserAccessUserList() throws Exception {
        // 普通用户不能访问用户列表
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCreateCategory() throws Exception {
        // 管理员可以创建分类
        String categoryJson = "{\"name\":\"测试分类\",\"description\":\"测试\"}";
        
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isOk());
    }

    @Test
    void testUserCreateCategory() throws Exception {
        // 普通用户不能创建分类
        String categoryJson = "{\"name\":\"测试分类2\",\"description\":\"测试\"}";
        
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCreateArticle() throws Exception {
        // 普通用户可以创建文章
        String articleJson = "{\"title\":\"测试文章\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        
        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk());
    }

    @Test
    void testUserCreateTag() throws Exception {
        // 普通用户可以创建标签
        String tagJson = "{\"name\":\"测试标签\"}";
        
        mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tagJson))
                .andExpect(status().isOk());
    }

    @Test
    void testGetCurrentUserInfo() throws Exception {
        // 登录用户可以获取自己的信息
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user"));
    }

    @Test
    void testInvalidToken() throws Exception {
        // 无效Token
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }
}
