package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.constant.ArticleStatusConstant;
import com.blog.entity.Article;
import com.blog.entity.ArticleFavorite;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleFavoriteMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.TagMapper;
import com.blog.vo.ArticleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章收藏服务类
 *
 * 提供文章收藏相关业务功能：
 * - 收藏 / 取消收藏文章
 * - 查询当前用户对某篇文章的收藏状态
 * - 分页查询当前用户收藏的文章列表
 *
 * 业务规则：
 * - 只能收藏已发布的文章
 * - 不能收藏自己发布的文章（避免刷量）
 * - 同一用户对同一文章只能收藏一次（重复收藏抛出业务异常）
 *
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class ArticleFavoriteService {

    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private TagMapper tagMapper;

    /**
     * 收藏文章
     *
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 仅允许收藏已发布的文章
     * 3. 不允许收藏自己发布的文章（避免刷量）
     * 4. 检查是否已经收藏过（防止重复收藏）
     * 5. 创建收藏记录并增加文章收藏计数
     *
     * @param articleId 文章ID
     * @param userId    收藏用户ID
     * @throws BusinessException 文章不存在、文章未发布、收藏自己文章或已收藏时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId, Long userId) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 仅允许收藏已发布的文章
        if (!ArticleStatusConstant.PUBLISHED.equals(article.getStatus())) {
            throw new BusinessException("只能收藏已发布的文章");
        }

        // 不能收藏自己的文章（避免刷量）
        if (article.getUserId().equals(userId)) {
            throw new BusinessException("不能收藏自己的文章");
        }

        // 检查是否已经收藏（防止重复收藏）
        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        if (articleFavoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经收藏过了");
        }

        // 创建收藏记录
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setArticleId(articleId);
        favorite.setUserId(userId);
        articleFavoriteMapper.insert(favorite);

        // 增加文章收藏计数
        Integer current = article.getFavoriteCount() == null ? 0 : article.getFavoriteCount();
        article.setFavoriteCount(current + 1);
        articleMapper.updateById(article);
    }

    /**
     * 取消收藏文章
     *
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 检查是否已收藏（只能取消已收藏的文章）
     * 3. 删除收藏记录并减少文章收藏计数
     *
     * @param articleId 文章ID
     * @param userId    取消收藏的用户ID
     * @throws BusinessException 文章不存在或未收藏时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId, Long userId) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 检查是否已收藏
        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        if (articleFavoriteMapper.selectCount(wrapper) == 0) {
            throw new BusinessException("还未收藏");
        }

        // 删除收藏记录
        articleFavoriteMapper.delete(wrapper);

        // 减少文章收藏计数（保护下界）
        Integer current = article.getFavoriteCount() == null ? 0 : article.getFavoriteCount();
        article.setFavoriteCount(Math.max(0, current - 1));
        articleMapper.updateById(article);
    }

    /**
     * 判断指定用户是否收藏了指定文章
     *
     * @param articleId 文章ID
     * @param userId    用户ID（可为null，为null时直接返回false）
     * @return true表示已收藏
     */
    public boolean isFavorited(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        return articleFavoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 分页查询当前用户的收藏文章列表
     *
     * 仅返回仍处于"已发布"状态、未删除的文章；按收藏时间倒序排列
     *
     * @param page   页码，从1开始
     * @param size   每页大小
     * @param userId 当前登录用户ID
     * @return 收藏文章的分页结果
     */
    public IPage<ArticleVO> getFavoriteArticles(Integer page, Integer size, Long userId) {
        Page<ArticleVO> pageParam = new Page<>(page, size);
        IPage<ArticleVO> result = articleFavoriteMapper.selectFavoriteArticlePage(pageParam, userId);

        // 加载标签列表，并标记为已收藏
        result.getRecords().forEach(article -> {
            List<String> tags = tagMapper.selectTagNamesByArticleId(article.getId());
            article.setTags(tags);
            article.setFavorited(true);
        });

        return result;
    }
}
