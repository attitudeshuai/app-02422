package com.blog.service;

import com.blog.constant.ArticleStatusConstant;
import com.blog.constant.RoleConstant;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.exception.BusinessException;
import com.blog.vo.CommentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArticleService articleService;

    private Long testArticleId;

    @BeforeEach
    void setUp() {
        // 创建测试文章
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("测试文章");
        articleDTO.setContent("测试内容");
        articleDTO.setCategoryId(1L);
        articleDTO.setStatus(ArticleStatusConstant.PUBLISHED);
        articleDTO.setTagIds(Arrays.asList(1L, 2L));
        testArticleId = articleService.createArticle(articleDTO, 1L);
    }

    @Test
    void testCreateComment() {
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(testArticleId);
        dto.setContent("这是一条测试评论");

        Long commentId = commentService.createComment(dto, 1L);
        assertNotNull(commentId);
        assertTrue(commentId > 0);
    }

    @Test
    void testCreateCommentArticleNotFound() {
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(99999L);
        dto.setContent("测试评论");

        assertThrows(BusinessException.class, () -> {
            commentService.createComment(dto, 1L);
        });
    }

    @Test
    void testCreateReplyComment() {
        // 先创建父评论
        CommentDTO parentDto = new CommentDTO();
        parentDto.setArticleId(testArticleId);
        parentDto.setContent("父评论");
        Long parentId = commentService.createComment(parentDto, 1L);

        // 创建回复评论
        CommentDTO replyDto = new CommentDTO();
        replyDto.setArticleId(testArticleId);
        replyDto.setParentId(parentId);
        replyDto.setContent("回复评论");

        Long replyId = commentService.createComment(replyDto, 2L);
        assertNotNull(replyId);
        assertTrue(replyId > 0);
    }

    @Test
    void testGetCommentsByArticleId() {
        // 创建几条评论
        CommentDTO dto1 = new CommentDTO();
        dto1.setArticleId(testArticleId);
        dto1.setContent("评论1");
        commentService.createComment(dto1, 1L);

        CommentDTO dto2 = new CommentDTO();
        dto2.setArticleId(testArticleId);
        dto2.setContent("评论2");
        commentService.createComment(dto2, 2L);

        List<CommentVO> comments = commentService.getCommentsByArticleId(testArticleId);
        assertNotNull(comments);
        assertTrue(comments.size() >= 2);
    }

    @Test
    void testDeleteComment() {
        // 创建评论
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(testArticleId);
        dto.setContent("待删除评论");
        Long commentId = commentService.createComment(dto, 1L);

        // 删除评论（用户删除自己的评论）
        assertDoesNotThrow(() -> {
            commentService.deleteComment(commentId, 1L, RoleConstant.USER);
        });
    }

    @Test
    void testDeleteCommentUnauthorized() {
        // 创建评论
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(testArticleId);
        dto.setContent("其他用户的评论");
        Long commentId = commentService.createComment(dto, 1L);

        // 其他用户尝试删除
        assertThrows(BusinessException.class, () -> {
            commentService.deleteComment(commentId, 2L, RoleConstant.USER);
        });
    }

    @Test
    void testDeleteCommentByAdmin() {
        // 创建评论
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(testArticleId);
        dto.setContent("用户评论");
        Long commentId = commentService.createComment(dto, 2L);

        // 管理员删除
        assertDoesNotThrow(() -> {
            commentService.deleteComment(commentId, 1L, RoleConstant.ADMIN);
        });
    }
}
