package com.blog;

import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BlogApplicationTests {

    @Autowired
    private AuthService authService;

    @Test
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    void testLogin() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        
        Map<String, Object> result = authService.login(dto);
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));
    }
}
