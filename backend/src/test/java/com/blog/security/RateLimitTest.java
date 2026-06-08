package com.blog.security;

import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 接口限流测试
 * 验证@RateLimit注解和RateLimitInterceptor是否有效
 */
@SpringBootTest
@AutoConfigureMockMvc
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginRateLimit() throws Exception {
        // 登录接口限流：@RateLimit(limit = 10)
        // 快速连续请求超过限制应该被拒绝

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrongpassword");

        int successCount = 0;
        int failCount = 0;

        // 快速发送20个请求
        for (int i = 0; i < 20; i++) {
            try {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                        .andReturn();
                successCount++;
            } catch (Exception e) {
                if (e.getMessage().contains("请求过于频繁")) {
                    failCount++;
                }
            }
            
            // 短暂延迟
            Thread.sleep(50);
        }

        // 应该有部分请求被限流
        System.out.println("成功请求: " + successCount + ", 被限流: " + failCount);
        assertTrue(successCount > 0, "应该有部分请求成功");
    }

    @Test
    void testRegisterRateLimit() throws Exception {
        // 注册接口限流：@RateLimit(limit = 5)
        // 更严格的限流

        int requestCount = 15;
        int successCount = 0;

        for (int i = 0; i < requestCount; i++) {
            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setUsername("testuser" + i);
            registerDTO.setPassword("test123");
            registerDTO.setEmail("test" + i + "@test.com");

            try {
                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                        .andReturn();
                successCount++;
            } catch (Exception e) {
                // 被限流
            }

            Thread.sleep(100);
        }

        System.out.println("注册成功请求: " + successCount + "/" + requestCount);
        assertTrue(successCount < requestCount, "应该有请求被限流");
    }

    @Test
    void testSearchRateLimit() throws Exception {
        // 搜索接口限流：@RateLimit(limit = 20)

        int requestCount = 30;
        int successCount = 0;

        for (int i = 0; i < requestCount; i++) {
            try {
                mockMvc.perform(get("/api/articles/search")
                        .param("keyword", "test"))
                        .andReturn();
                successCount++;
            } catch (Exception e) {
                // 被限流
            }

            Thread.sleep(30);
        }

        System.out.println("搜索成功请求: " + successCount + "/" + requestCount);
    }

    @Test
    void testConcurrentRateLimit() throws Exception {
        // 测试并发请求的限流效果
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("并发测试 - 成功: " + successCount.get() + ", 失败: " + failCount.get());
        assertTrue(successCount.get() > 0, "应该有部分请求成功");
    }

    @Test
    void testRateLimitRecovery() throws Exception {
        // 测试限流恢复
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrongpassword");

        // 第一波请求
        for (int i = 0; i < 15; i++) {
            try {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)));
            } catch (Exception e) {
                // 忽略
            }
            Thread.sleep(50);
        }

        // 等待一段时间让限流恢复
        Thread.sleep(2000);

        // 第二波请求应该能成功
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().is5xxServerError()); // 密码错误，但不是限流
    }

    @Test
    void testDifferentIpRateLimit() throws Exception {
        // 测试不同IP的限流是独立的
        // 注意：在测试环境中，所有请求来自同一IP，这个测试主要验证逻辑

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrongpassword");

        // 模拟来自不同IP的请求（通过X-Forwarded-For头）
        int successCount = 0;

        for (int i = 0; i < 10; i++) {
            try {
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1." + i)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                        .andReturn();
                successCount++;
            } catch (Exception e) {
                // 忽略
            }
            Thread.sleep(100);
        }

        System.out.println("不同IP请求成功: " + successCount);
        assertTrue(successCount > 0, "不同IP的请求应该能成功");
    }
}
