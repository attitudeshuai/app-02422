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
import java.util.Objects;

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

        if (!Objects.equals(article.getStatus(), ArticleStatusConstant.PUBLISHED)) {
            throw new BusinessException("只能收藏已发布的文章");
        }

        if (Objects.equals(article.getUserId(), userId)) {
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

        article.setFavoriteCount(article.getFavoriteCount() == null ? 1 : article.getFavoriteCount() + 1);
        articleMapper.updateById(article);
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

        article.setFavoriteCount(Math.max(0, article.getFavoriteCount() == null ? 0 : article.getFavoriteCount() - 1));
        articleMapper.updateById(article);
    }

    public IPage<ArticleVO> getFavoriteList(Long userId, Integer page, Integer size) {
        Page<ArticleVO> pageParam = new Page<>(page, size);
        IPage<ArticleVO> articlePage = articleFavoriteMapper.selectFavoriteArticlePage(pageParam, userId);
        articlePage.getRecords().forEach(article -> {
            List<String> tags = tagMapper.selectTagNamesByArticleId(article.getId());
            article.setTags(tags);
        });
        return articlePage;
    }
}
