package com.blog.integration;

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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文章功能集成测试
 * 测试完整的文章生命周期：创建、查询、更新、点赞、评论、删除
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private Long articleId;

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
    void testCompleteArticleLifecycle() throws Exception {
        // 1. 创建文章
        String articleJson = "{\"title\":\"集成测试文章\",\"content\":\"这是集成测试的内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1,2]}";
        
        MvcResult createResult = mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        articleId = objectMapper.readTree(createResponse).get("data").asLong();

        // 2. 查询文章详情
        mockMvc.perform(get("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("集成测试文章"))
                .andExpect(jsonPath("$.data.viewCount").value(1)); // 查看后浏览量+1

        // 3. 点赞文章
        mockMvc.perform(post("/api/articles/" + articleId + "/like")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 4. 验证点赞
        mockMvc.perform(get("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(true));

        // 5. 发表评论
        String commentJson = "{\"articleId\":" + articleId + ",\"content\":\"这是一条测试评论\"}";
        
        mockMvc.perform(post("/api/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson))
                .andExpect(status().isOk());

        // 6. 查询评论
        mockMvc.perform(get("/api/comments?articleId=" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("这是一条测试评论"));

        // 7. 更新文章
        String updateJson = "{\"title\":\"更新后的标题\",\"content\":\"更新后的内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        
        mockMvc.perform(put("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        // 8. 验证更新
        mockMvc.perform(get("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新后的标题"));

        // 9. 取消点赞
        mockMvc.perform(delete("/api/articles/" + articleId + "/like")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 10. 删除文章
        mockMvc.perform(delete("/api/articles/" + articleId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void testArticleSearch() throws Exception {
        // 创建文章
        String articleJson = "{\"title\":\"搜索测试文章\",\"content\":\"包含关键词的内容\",\"categoryId\":1,\"status\":1,\"tagIds\":[1]}";
        
        mockMvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk());

        // 搜索文章
        mockMvc.perform(get("/api/articles/search?keyword=搜索测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testArticlePagination() throws Exception {
        // 测试分页查询
        mockMvc.perform(get("/api/articles?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.size").value(10));
    }
}
