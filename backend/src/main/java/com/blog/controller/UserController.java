package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.annotation.OperationLog;
import com.blog.dto.UserUpdateDTO;
import com.blog.service.ArticleFavoriteService;
import com.blog.service.UserService;
import com.blog.utils.JwtUtil;
import com.blog.vo.ArticleVO;
import com.blog.vo.Result;
import com.blog.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleFavoriteService articleFavoriteService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.getUserList(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id, HttpServletRequest request) {
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
        return Result.success(userService.getUserById(id, currentUserId, currentUserRole));
    }

    @PutMapping("/{id}")
    @OperationLog("更新用户信息")
    public Result<Object> updateUser(@PathVariable Long id,
                                     @Validated @RequestBody UserUpdateDTO dto,
                                     HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long currentUserId = jwtUtil.getUserIdFromToken(token);
        Integer currentUserRole = jwtUtil.getRoleFromToken(token);
        
        userService.updateUser(id, currentUserId, currentUserRole, dto);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("删除用户")
    public Result<Object> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("设置用户角色")
    public Result<Object> updateUserRole(@PathVariable Long id, @RequestParam Integer role) {
        userService.updateUserRole(id, role);
        return Result.success("设置成功");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("设置用户状态")
    public Result<Object> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success("设置成功");
    }

    /**
     * 获取当前登录用户的收藏文章列表（分页）
     *
     * 个人中心"我的收藏"页面使用，仅当前登录用户可访问自己的收藏列表
     *
     * @param page 页码，从1开始
     * @param size 每页大小
     * @param authentication Spring Security 注入的当前认证信息
     * @return 收藏文章的分页结果
     */
    @GetMapping("/me/favorites")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<ArticleVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(articleFavoriteService.getFavoriteArticles(page, size, userId));
    }
}
