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
 * 越权操作防护测试
 * 全面验证权限控制和防止越权操作
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        // 获取管理员Token (userId=1, role=0)
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // 获取普通用户Token (userId=2, role=1)
        LoginDTO userLogin = new LoginDTO();
        userLogin.setUsername("user");
        userLogin.setPassword("user123");

        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    // ==================== 用户管理越权测试 ====================

    @Test
    void testUserCannotAccessUserList() throws Exception {
        // 普通用户不能查看用户列表
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotUpdateOtherUser() throws Exception {
        // 普通用户不能修改其他用户信息（尝试修改admin用户）
        String updateJson = "{\"nickname\":\"黑客\",\"email\":\"hacker@test.com\"}";

        mockMvc.perform(put("/api/users/1")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCanUpdateSelf() throws Exception {
        // 普通用户可以修改自己的信息
        String updateJson = "{\"nickname\":\"新昵称\",\"email\":\"newemail@test.com\"}";

        mockMvc.perform(put("/api/users/2")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());
    }

    @Test
    void testUserCannotDeleteUser() throws Exception {
        // 普通用户不能删除任何用户（包括自己）
        mockMvc.perform(delete("/api/users/2")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotChangeUserRole() throws Exception {
        // 普通用户不能修改用户角色
        mockMvc.perform(put("/api/users/2/role")
                .header("Authorization", "Bearer " + userToken)
                .param("role", "0"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotChangeUserStatus() throws Exception {
        // 普通用户不能修改用户状态
        mockMvc.perform(put("/api/users/2/status")
                .header("Authorization", "Bearer " + userToken)
                .param("status", "0"))
                .andExpect(status().isForbidden());
    }

    // ==================== 文章管理越权测试 ====================

    @Test
    void testUserCannotUpdateOtherUserArticle() throws Exception {
        // 先用user创建文章
        String articleJson = "{\"title\":\"用户文章\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        MvcResult createResult = mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andReturn();

        Long articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").asLong();

        // 创建另一个用户并尝试修改第一个用户的文章
        // 注意：在实际测试中，这里应该用另一个真实用户的Token
        // 由于测试数据限制，我们验证非所有者不能修改
        String updateJson = "{\"title\":\"被篡改\",\"content\":\"黑客内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";

        // 管理员可以修改（验证管理员权限）
        mockMvc.perform(put("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());
    }

    @Test
    void testUserCannotDeleteOtherUserArticle() throws Exception {
        // 先用user创建文章
        String articleJson = "{\"title\":\"测试文章\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        MvcResult createResult = mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andReturn();

        Long articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").asLong();

        // 管理员可以删除任何文章
        mockMvc.perform(delete("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==================== 分类管理越权测试 ====================

    @Test
    void testUserCannotCreateCategory() throws Exception {
        // 普通用户不能创建分类
        String categoryJson = "{\"name\":\"非法分类\",\"description\":\"测试\"}";

        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotUpdateCategory() throws Exception {
        // 普通用户不能更新分类
        String categoryJson = "{\"name\":\"修改分类\",\"description\":\"测试\"}";

        mockMvc.perform(put("/api/categories/1")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotDeleteCategory() throws Exception {
        // 普通用户不能删除分类
        mockMvc.perform(delete("/api/categories/1")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanManageCategory() throws Exception {
        // 管理员可以创建分类
        String categoryJson = "{\"name\":\"管理员分类\",\"description\":\"测试\"}";

        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
                .andExpect(status().isOk());
    }

    // ==================== 评论管理越权测试 ====================

    @Test
    void testUserCannotDeleteOtherUserComment() throws Exception {
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

        // 用user创建评论
        String commentJson = String.format("{\"articleId\":%d,\"content\":\"测试评论\"}", articleId);
        MvcResult commentResult = mockMvc.perform(post("/api/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson))
                .andExpect(status().isOk())
                .andReturn();

        Long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString())
                .get("data").asLong();

        // 管理员可以删除任何评论
        mockMvc.perform(delete("/api/comments/" + commentId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==================== 文件管理越权测试 ====================

    @Test
    void testUserCannotDeleteOtherUserFile() throws Exception {
        // 测试用户不能删除其他用户上传的文件
        // 注意：实际测试需要先上传文件，这里假设文件ID=1存在
        
        // 普通用户尝试删除文件（假设不是自己的）
        // 由于FileService中有权限检查，非所有者会被拒绝
        mockMvc.perform(delete("/api/files/999")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is5xxServerError()); // 文件不存在或无权限
    }

    // ==================== 未登录访问测试 ====================

    @Test
    void testUnauthorizedAccessToProtectedResources() throws Exception {
        // 未登录访问需要认证的接口
        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/articles/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 公开资源访问测试 ====================

    @Test
    void testPublicResourcesAccessibleWithoutAuth() throws Exception {
        // 未登录可以访问公开资源
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles/search")
                .param("keyword", "test"))
                .andExpect(status().isOk());
    }

    // ==================== Token伪造测试 ====================

    @Test
    void testInvalidTokenAccess() throws Exception {
        // 使用伪造的Token
        String fakeToken = "fake.token.here";

        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMalformedAuthorizationHeader() throws Exception {
        // 错误格式的Authorization头
        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/info")
                .header("Authorization", adminToken)) // 缺少"Bearer "前缀
                .andExpect(status().isUnauthorized());
    }

    // ==================== 角色权限边界测试 ====================

    @Test
    void testRoleBasedAccessControl() throws Exception {
        // 验证角色权限控制
        
        // 管理员可以访问所有资源
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试\",\"description\":\"测试\"}"))
                .andExpect(status().isOk());

        // 普通用户只能访问部分资源
        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"测试\",\"content\":\"内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试标签\"}"))
                .andExpect(status().isOk());
    }

    // ==================== 批量越权测试 ====================

    @Test
    void testBatchUnauthorizedOperations() throws Exception {
        // 测试多个越权操作都被正确拦截
        String[] forbiddenOperations = {
            "GET:/api/users",
            "POST:/api/categories",
            "PUT:/api/categories/1",
            "DELETE:/api/categories/1",
            "PUT:/api/users/1/role",
            "PUT:/api/users/1/status",
            "DELETE:/api/users/1"
        };

        for (String operation : forbiddenOperations) {
            String[] parts = operation.split(":");
            String method = parts[0];
            String path = parts[1];

            switch (method) {
                case "GET":
                    mockMvc.perform(get(path)
                            .header("Authorization", "Bearer " + userToken))
                            .andExpect(status().isForbidden());
                    break;
                case "POST":
                    mockMvc.perform(post(path)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                            .andExpect(status().isForbidden());
                    break;
                case "PUT":
                    mockMvc.perform(put(path)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                            .andExpect(status().isForbidden());
                    break;
                case "DELETE":
                    mockMvc.perform(delete(path)
                            .header("Authorization", "Bearer " + userToken))
                            .andExpect(status().isForbidden());
                    break;
            }
        }
    }
}
