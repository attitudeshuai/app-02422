package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.service.AuthService;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @RateLimit(limit = 5)
    public Result<Map<String, Object>> register(@Validated @RequestBody RegisterDTO dto) {
        Map<String, Object> result = authService.register(dto);
        return Result.success("注册成功", result);
    }

    @PostMapping("/login")
    @RateLimit(limit = 10)
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO dto) {
        Map<String, Object> result = authService.login(dto);
        return Result.success("登录成功", result);
    }

    @GetMapping("/info")
    public Result<Object> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(authService.getCurrentUser(userId));
    }

    @PostMapping("/logout")
    public Result<Object> logout() {
        return Result.success("登出成功");
    }
}
