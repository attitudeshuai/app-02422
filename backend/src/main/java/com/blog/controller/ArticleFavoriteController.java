package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.annotation.OperationLog;
import com.blog.service.ArticleFavoriteService;
import com.blog.vo.ArticleVO;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ArticleFavoriteController {

    @Autowired
    private ArticleFavoriteService articleFavoriteService;

    @PostMapping("/articles/{id}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("收藏文章")
    public Result<Object> favoriteArticle(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleFavoriteService.favoriteArticle(id, userId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/articles/{id}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("取消收藏")
    public Result<Object> unfavoriteArticle(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleFavoriteService.unfavoriteArticle(id, userId);
        return Result.success("取消收藏成功");
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<ArticleVO>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(articleFavoriteService.getFavoriteList(userId, page, size));
    }
}
