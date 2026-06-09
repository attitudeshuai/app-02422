package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.ArticleFavorite;
import com.blog.vo.ArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite> {

    /**
     * 分页查询指定用户收藏的文章列表（仅返回已发布文章）
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 收藏的文章分页结果
     */
    IPage<ArticleVO> selectFavoriteArticlePage(Page<?> page, @Param("userId") Long userId);
}
