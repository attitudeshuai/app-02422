package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import com.blog.vo.ArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    IPage<ArticleVO> selectArticlePage(Page<?> page, @Param("keyword") String keyword, 
                                        @Param("categoryId") Long categoryId, 
                                        @Param("userId") Long userId,
                                        @Param("status") Integer status);
    
    ArticleVO selectArticleVOById(@Param("id") Long id);
    
    List<ArticleVO> searchArticles(@Param("keyword") String keyword);
    
    /**
     * 原子增加文章浏览量
     * 使用SQL的自增操作避免并发更新丢失
     * 
     * @param id 文章ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("id") Long id);
}
