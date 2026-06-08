package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.annotation.OperationLog;
import com.blog.annotation.RateLimit;
import com.blog.dto.ArticleDTO;
import com.blog.service.ArticleService;
import com.blog.utils.JwtUtil;
import com.blog.vo.ArticleVO;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<IPage<ArticleVO>> getArticleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long currentUserId = null;
        Integer currentUserRole = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                currentUserId = jwtUtil.getUserIdFromToken(token);
                currentUserRole = jwtUtil.getRoleFromToken(token);
            }
        }
        return Result.success(articleService.getArticleList(page, size, keyword, categoryId, userId, status, currentUserId, currentUserRole));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> getArticleById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = null;
        Integer currentUserRole = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                currentUserId = jwtUtil.getUserIdFromToken(token);
                currentUserRole = jwtUtil.getRoleFromToken(token);
            }
        }
        return Result.success(articleService.getArticleById(id, currentUserId, currentUserRole));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("创建文章")
    public Result<Long> createArticle(@Validated @RequestBody ArticleDTO dto, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long articleId = articleService.createArticle(dto, userId);
        return Result.success("创建成功", articleId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("更新文章")
    public Result<Object> updateArticle(@PathVariable Long id,
                                        @Validated @RequestBody ArticleDTO dto,
                                        HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long currentUserId = jwtUtil.getUserIdFromToken(token);
        Integer currentUserRole = jwtUtil.getRoleFromToken(token);
        
        articleService.updateArticle(id, dto, currentUserId, currentUserRole);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除文章")
    public Result<Object> deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long currentUserId = jwtUtil.getUserIdFromToken(token);
        Integer currentUserRole = jwtUtil.getRoleFromToken(token);
        
        articleService.deleteArticle(id, currentUserId, currentUserRole);
        return Result.success("删除成功");
    }

    @GetMapping("/search")
    @RateLimit(limit = 20)
    public Result<List<ArticleVO>> searchArticles(@RequestParam String keyword) {
        return Result.success(articleService.searchArticles(keyword));
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("点赞文章")
    public Result<Object> likeArticle(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleService.likeArticle(id, userId);
        return Result.success("点赞成功");
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("取消点赞")
    public Result<Object> unlikeArticle(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleService.unlikeArticle(id, userId);
        return Result.success("取消点赞成功");
    }
}
