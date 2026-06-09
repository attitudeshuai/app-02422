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

@Service
public class ArticleFavoriteService {

    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private TagMapper tagMapper;

    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        if (!ArticleStatusConstant.PUBLISHED.equals(article.getStatus())) {
            throw new BusinessException("只能收藏已发布的文章");
        }

        if (article.getUserId().equals(userId)) {
            throw new BusinessException("不能收藏自己的文章");
        }

        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        if (articleFavoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经收藏过了");
        }

        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setArticleId(articleId);
        favorite.setUserId(userId);
        articleFavoriteMapper.insert(favorite);

        articleMapper.incrementFavoriteCount(articleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        if (articleFavoriteMapper.selectCount(wrapper) == 0) {
            throw new BusinessException("还未收藏");
        }

        articleFavoriteMapper.delete(wrapper);

        articleMapper.decrementFavoriteCount(articleId);
    }

    public IPage<ArticleVO> getFavoriteArticles(Integer page, Integer size, Long userId) {
        Page<ArticleVO> pageParam = new Page<>(page, size);
        IPage<ArticleVO> articlePage = articleFavoriteMapper.selectFavoriteArticlesByUserId(pageParam, userId);

        articlePage.getRecords().forEach(article -> {
            article.setTags(tagMapper.selectTagNamesByArticleId(article.getId()));
            article.setFavorited(true);
        });

        return articlePage;
    }

    public boolean isFavorited(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId);
        return articleFavoriteMapper.selectCount(wrapper) > 0;
    }
}
