package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.constant.ArticleStatusConstant;
import com.blog.dto.ArticleDTO;
import com.blog.exception.BusinessException;
import com.blog.vo.ArticleVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ArticleServiceTest {

    @Autowired
    private ArticleService articleService;

    @Test
    void testCreateAndGetArticle() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("测试文章");
        dto.setContent("这是测试内容");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);
        dto.setTagIds(Arrays.asList(1L, 2L));

        Long articleId = articleService.createArticle(dto, 1L);
        assertNotNull(articleId);

        ArticleVO article = articleService.getArticleById(articleId, 1L, null);
        assertEquals("测试文章", article.getTitle());
        assertEquals("这是测试内容", article.getContent());
    }

    @Test
    void testGetArticleList() {
        IPage<ArticleVO> page = articleService.getArticleList(1, 10, null, null, null, ArticleStatusConstant.PUBLISHED, null, null);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 0);
    }

    @Test
    void testSearchArticles() {
        // 创建测试文章
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("Spring Boot 全文搜索测试");
        dto.setContent("这是一篇关于 Spring Boot 全文搜索的测试文章");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);

        articleService.createArticle(dto, 1L);

        // 搜索文章
        List<ArticleVO> results = articleService.searchArticles("Spring Boot");
        assertNotNull(results);
        // 注意：全文搜索需要MySQL FULLTEXT索引支持
    }

    @Test
    void testLikeArticle() {
        // 创建文章
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("点赞测试文章");
        dto.setContent("测试点赞功能");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);
        dto.setTagIds(Arrays.asList(1L));

        Long articleId = articleService.createArticle(dto, 1L);

        // 点赞
        assertDoesNotThrow(() -> {
            articleService.likeArticle(articleId, 2L);
        });

        // 验证点赞数增加
        ArticleVO article = articleService.getArticleById(articleId, 2L, null);
        assertEquals(1, article.getLikeCount());
        assertTrue(article.getLiked());
    }

    @Test
    void testLikeArticleTwice() {
        // 创建文章
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("重复点赞测试");
        dto.setContent("测试重复点赞");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);

        Long articleId = articleService.createArticle(dto, 1L);

        // 第一次点赞
        articleService.likeArticle(articleId, 2L);

        // 第二次点赞应该失败
        assertThrows(BusinessException.class, () -> {
            articleService.likeArticle(articleId, 2L);
        });
    }

    @Test
    void testUnlikeArticle() {
        // 创建文章并点赞
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("取消点赞测试");
        dto.setContent("测试取消点赞");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);

        Long articleId = articleService.createArticle(dto, 1L);
        articleService.likeArticle(articleId, 2L);

        // 取消点赞
        assertDoesNotThrow(() -> {
            articleService.unlikeArticle(articleId, 2L);
        });

        // 验证点赞数减少
        ArticleVO article = articleService.getArticleById(articleId, 2L, null);
        assertEquals(0, article.getLikeCount());
        assertFalse(article.getLiked());
    }

    @Test
    void testUnlikeArticleNotLiked() {
        // 创建文章
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("未点赞取消测试");
        dto.setContent("测试未点赞就取消");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatusConstant.PUBLISHED);

        Long articleId = articleService.createArticle(dto, 1L);

        // 未点赞就取消应该失败
        assertThrows(BusinessException.class, () -> {
            articleService.unlikeArticle(articleId, 2L);
        });
    }
}
