package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.RoleConstant;
import com.blog.dto.CommentDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务类
 * 
 * 提供评论管理的核心业务功能，包括：
 * - 评论的发表和删除
 * - 评论列表查询
 * - 评论回复功能（支持二级评论）
 * - 文章评论数统计
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 获取文章的评论列表
     * 
     * 返回指定文章的所有评论，包括评论者信息
     * 支持二级评论结构（评论和回复）
     * 
     * @param articleId 文章ID
     * @return 评论列表，包含评论者昵称和头像
     */
    public List<CommentVO> getCommentsByArticleId(Long articleId) {
        return commentMapper.selectCommentVOsByArticleId(articleId);
    }

    /**
     * 创建评论
     * 
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 如果是回复评论，验证父评论是否存在
     * 3. 创建评论记录
     * 4. 增加文章的评论计数
     * 
     * 使用事务保证评论创建和计数更新的一致性
     * 
     * @param dto 评论数据，包含文章ID、评论内容、父评论ID（可选）
     * @param userId 评论者ID（当前登录用户）
     * @return 新创建的评论ID
     * @throws BusinessException 文章不存在或父评论不存在时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentDTO dto, Long userId) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 如果是回复评论，验证父评论是否存在
        if (dto.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment == null) {
                throw new BusinessException("父评论不存在");
            }
        }

        // 创建评论记录
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());

        commentMapper.insert(comment);

        // 增加文章的评论计数
        article.setCommentCount(article.getCommentCount() + 1);
        articleMapper.updateById(article);

        return comment.getId();
    }

    /**
     * 删除评论
     * 
     * 权限控制：
     * - 评论作者可以删除自己的评论
     * - 管理员可以删除任何评论
     * 
     * 业务逻辑：
     * 1. 验证评论是否存在
     * 2. 验证当前用户是否有权限删除
     * 3. 删除评论记录
     * 4. 减少文章的评论计数（不能小于0）
     * 
     * 使用事务保证评论删除和计数更新的一致性
     * 
     * @param id 评论ID
     * @param currentUserId 当前登录用户ID
     * @param currentUserRole 当前用户角色
     * @throws BusinessException 评论不存在或无权限时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id, Long currentUserId, Integer currentUserRole) {
        // 验证评论是否存在
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 权限验证：只有评论作者或管理员可以删除
        if (!comment.getUserId().equals(currentUserId) && !currentUserRole.equals(RoleConstant.ADMIN)) {
            throw new BusinessException("无权删除此评论");
        }

        // 删除评论记录
        commentMapper.deleteById(id);

        // 减少文章的评论计数（使用Math.max确保不会小于0）
        Article article = articleMapper.selectById(comment.getArticleId());
        if (article != null) {
            article.setCommentCount(Math.max(0, article.getCommentCount() - 1));
            articleMapper.updateById(article);
        }
    }
}
