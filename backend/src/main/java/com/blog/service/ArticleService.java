package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.constant.ArticleStatusConstant;
import com.blog.constant.RoleConstant;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.ArticleLike;
import com.blog.entity.ArticleTag;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.vo.ArticleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章服务类
 * 
 * 提供文章的核心业务功能，包括：
 * - 文章的增删改查操作
 * - 文章分页查询和搜索
 * - 文章点赞和取消点赞
 * - 文章标签关联管理
 * - 文章浏览量统计
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private TagMapper tagMapper;

    /**
     * 分页查询文章列表
     * 
     * 支持多条件组合查询：关键词搜索、分类筛选、用户筛选、状态筛选
     * 查询结果包含文章基本信息、作者信息、分类信息和关联标签
     * 
     * 访问控制：
     * - 未登录用户只能查看已发布文章
     * - 普通用户可以查看已发布文章 + 自己的草稿
     * - 管理员可以查看所有文章
     * 
     * @param page 页码，从1开始
     * @param size 每页大小
     * @param keyword 搜索关键词，可选，用于标题和内容模糊匹配
     * @param categoryId 分类ID，可选，筛选指定分类的文章
     * @param userId 用户ID，可选，筛选指定作者的文章
     * @param status 文章状态，可选，0-草稿 1-已发布
     * @param currentUserId 当前登录用户ID，未登录时为null
     * @param currentUserRole 当前用户角色，未登录时为null
     * @return 分页结果，包含文章列表和分页信息
     */
    public IPage<ArticleVO> getArticleList(Integer page, Integer size, String keyword, 
                                           Long categoryId, Long userId, Integer status,
                                           Long currentUserId, Integer currentUserRole) {
        // 构建分页参数
        Page<ArticleVO> pageParam = new Page<>(page, size);
        
        // 访问控制：确定实际可查询的状态
        Integer effectiveStatus = status;
        Long effectiveUserId = userId;
        
        // 非管理员尝试查看草稿时，需要限制只能看自己的
        if (status != null && status.equals(ArticleStatusConstant.DRAFT)) {
            if (currentUserRole == null || !currentUserRole.equals(RoleConstant.ADMIN)) {
                // 非管理员只能查看自己的草稿
                if (currentUserId == null) {
                    // 未登录用户不能查看草稿，强制改为查看已发布
                    effectiveStatus = ArticleStatusConstant.PUBLISHED;
                } else {
                    // 已登录但非管理员，只能查看自己的草稿
                    effectiveUserId = currentUserId;
                }
            }
        }
        
        // 如果未指定状态且非管理员，默认只显示已发布文章
        if (status == null && (currentUserRole == null || !currentUserRole.equals(RoleConstant.ADMIN))) {
            effectiveStatus = ArticleStatusConstant.PUBLISHED;
        }
        
        // 执行分页查询，使用自定义SQL实现多表关联和条件筛选
        IPage<ArticleVO> articlePage = articleMapper.selectArticlePage(pageParam, keyword, categoryId, effectiveUserId, effectiveStatus);
        
        // 为每篇文章加载关联的标签列表
        articlePage.getRecords().forEach(article -> {
            List<String> tags = tagMapper.selectTagNamesByArticleId(article.getId());
            article.setTags(tags);
        });
        
        return articlePage;
    }

    /**
     * 根据ID获取文章详情
     * 
     * 业务逻辑：
     * 1. 查询文章详细信息（包含作者和分类信息）
     * 2. 访问控制检查（草稿文章只有作者和管理员可以查看）
     * 3. 加载文章关联的标签列表
     * 4. 如果用户已登录，查询该用户是否已点赞此文章
     * 5. 自动增加文章浏览量（使用原子更新避免并发丢失）
     * 
     * @param id 文章ID
     * @param currentUserId 当前登录用户ID，未登录时为null
     * @param currentUserRole 当前用户角色，未登录时为null
     * @return 文章详情VO，包含完整的文章信息、标签、点赞状态
     * @throws BusinessException 文章不存在或无权访问时抛出异常
     */
    public ArticleVO getArticleById(Long id, Long currentUserId, Integer currentUserRole) {
        // 查询文章详情（包含作者和分类信息）
        ArticleVO article = articleMapper.selectArticleVOById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 访问控制：草稿文章只有作者本人或管理员可以查看
        if (article.getStatus() != null && article.getStatus().equals(ArticleStatusConstant.DRAFT)) {
            boolean isOwner = currentUserId != null && currentUserId.equals(article.getUserId());
            boolean isAdmin = currentUserRole != null && currentUserRole.equals(RoleConstant.ADMIN);
            if (!isOwner && !isAdmin) {
                throw new BusinessException("无权访问此文章");
            }
        }

        // 加载文章关联的标签列表
        List<String> tags = tagMapper.selectTagNamesByArticleId(id);
        article.setTags(tags);

        // 如果用户已登录，查询是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, currentUserId);
            article.setLiked(articleLikeMapper.selectCount(wrapper) > 0);
        }

        // 增加浏览量（使用原子更新，避免并发丢失计数）
        articleMapper.incrementViewCount(id);

        return article;
    }

    /**
     * 创建新文章
     * 
     * 业务逻辑：
     * 1. 创建文章基本信息（标题、内容、分类、状态等）
     * 2. 初始化统计字段（浏览量、点赞数、评论数）
     * 3. 建立文章与标签的多对多关联关系
     * 
     * 使用事务保证数据一致性：文章创建和标签关联要么全部成功，要么全部回滚
     * 
     * @param dto 文章数据传输对象，包含标题、内容、分类ID、状态、标签ID列表
     * @param userId 文章作者ID（当前登录用户）
     * @return 新创建的文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(ArticleDTO dto, Long userId) {
        // 创建文章实体并复制属性
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setUserId(userId);
        
        // 初始化统计字段
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);

        // 插入文章记录（MyBatis-Plus会自动填充ID和时间字段）
        articleMapper.insert(article);

        // 建立文章与标签的关联关系
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }

        return article.getId();
    }

    /**
     * 更新文章信息
     * 
     * 权限控制：
     * - 文章作者可以修改自己的文章
     * - 管理员可以修改任何文章
     * 
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 验证当前用户是否有权限修改（作者本人或管理员）
     * 3. 更新文章基本信息
     * 4. 重新建立文章与标签的关联关系（先删除旧关联，再创建新关联）
     * 
     * @param id 文章ID
     * @param dto 更新的文章数据
     * @param currentUserId 当前登录用户ID
     * @param currentUserRole 当前用户角色（0-管理员 1-普通用户）
     * @throws BusinessException 文章不存在或无权限时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long id, ArticleDTO dto, Long currentUserId, Integer currentUserRole) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 权限验证：只有作者本人或管理员可以修改
        if (!article.getUserId().equals(currentUserId) && !currentUserRole.equals(RoleConstant.ADMIN)) {
            throw new BusinessException("无权修改此文章");
        }

        // 更新文章基本信息
        BeanUtils.copyProperties(dto, article);
        articleMapper.updateById(article);

        // 删除旧的标签关联
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, id);
        articleTagMapper.delete(wrapper);

        // 创建新的标签关联
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(id);
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }
    }

    /**
     * 删除文章
     * 
     * 权限控制：
     * - 文章作者可以删除自己的文章
     * - 管理员可以删除任何文章
     * 
     * 注意：使用MyBatis-Plus的逻辑删除，实际是更新deleted字段，不会物理删除数据
     * 
     * @param id 文章ID
     * @param currentUserId 当前登录用户ID
     * @param currentUserRole 当前用户角色
     * @throws BusinessException 文章不存在或无权限时抛出异常
     */
    public void deleteArticle(Long id, Long currentUserId, Integer currentUserRole) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 权限验证：只有作者本人或管理员可以删除
        if (!article.getUserId().equals(currentUserId) && !currentUserRole.equals(RoleConstant.ADMIN)) {
            throw new BusinessException("无权删除此文章");
        }

        // 逻辑删除文章
        articleMapper.deleteById(id);
    }

    /**
     * 全文搜索文章
     * 
     * 使用MySQL的FULLTEXT全文索引进行搜索，支持中文分词
     * 搜索范围：文章标题和内容
     * 
     * @param keyword 搜索关键词
     * @return 匹配的文章列表，按相关度排序
     */
    public List<ArticleVO> searchArticles(String keyword) {
        // 执行全文搜索
        List<ArticleVO> articles = articleMapper.searchArticles(keyword);
        
        // 为每篇文章加载标签列表
        articles.forEach(article -> {
            List<String> tags = tagMapper.selectTagNamesByArticleId(article.getId());
            article.setTags(tags);
        });
        
        return articles;
    }

    /**
     * 点赞文章
     * 
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 检查用户是否已经点赞过（防止重复点赞）
     * 3. 创建点赞记录
     * 4. 增加文章的点赞计数
     * 
     * 使用事务保证点赞记录和点赞计数的一致性
     * 
     * @param id 文章ID
     * @param userId 点赞用户ID
     * @throws BusinessException 文章不存在或已点赞时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long id, Long userId) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 检查是否已经点赞（防止重复点赞）
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, userId);
        
        if (articleLikeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经点赞过了");
        }

        // 创建点赞记录
        ArticleLike like = new ArticleLike();
        like.setArticleId(id);
        like.setUserId(userId);
        articleLikeMapper.insert(like);

        // 增加文章点赞计数
        article.setLikeCount(article.getLikeCount() + 1);
        articleMapper.updateById(article);
    }

    /**
     * 取消点赞文章
     * 
     * 业务逻辑：
     * 1. 验证文章是否存在
     * 2. 检查用户是否已点赞（只能取消已点赞的文章）
     * 3. 删除点赞记录
     * 4. 减少文章的点赞计数
     * 
     * 使用事务保证点赞记录和点赞计数的一致性
     * 
     * @param id 文章ID
     * @param userId 取消点赞的用户ID
     * @throws BusinessException 文章不存在或未点赞时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long id, Long userId) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 检查是否已点赞（只能取消已点赞的）
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, userId);
        
        if (articleLikeMapper.selectCount(wrapper) == 0) {
            throw new BusinessException("还未点赞");
        }

        // 删除点赞记录
        articleLikeMapper.delete(wrapper);

        // 减少文章点赞计数
        article.setLikeCount(article.getLikeCount() - 1);
        articleMapper.updateById(article);
    }
}
