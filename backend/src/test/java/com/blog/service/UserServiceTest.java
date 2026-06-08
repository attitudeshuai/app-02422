package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.constant.RoleConstant;
import com.blog.dto.UserUpdateDTO;
import com.blog.exception.BusinessException;
import com.blog.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testGetUserList() {
        IPage<UserVO> page = userService.getUserList(1, 10, null);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 2); // 至少有admin和user两个用户
    }

    @Test
    void testGetUserListWithKeyword() {
        IPage<UserVO> page = userService.getUserList(1, 10, "admin");
        assertNotNull(page);
        assertTrue(page.getRecords().stream()
                .anyMatch(user -> user.getUsername().contains("admin")));
    }

    @Test
    void testGetUserById() {
        UserVO user = userService.getUserById(1L);
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }

    @Test
    void testGetUserByIdNotFound() {
        assertThrows(BusinessException.class, () -> {
            userService.getUserById(99999L);
        });
    }

    @Test
    void testUpdateUser() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("测试昵称");
        dto.setEmail("newemail@test.com");

        // 用户更新自己的信息
        assertDoesNotThrow(() -> {
            userService.updateUser(2L, 2L, RoleConstant.USER, dto);
        });
    }

    @Test
    void testUpdateUserUnauthorized() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("测试昵称");

        // 普通用户尝试更新其他用户信息
        assertThrows(BusinessException.class, () -> {
            userService.updateUser(1L, 2L, RoleConstant.USER, dto);
        });
    }

    @Test
    void testUpdateUserByAdmin() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("管理员修改");

        // 管理员可以更新任何用户信息
        assertDoesNotThrow(() -> {
            userService.updateUser(2L, 1L, RoleConstant.ADMIN, dto);
        });
    }

    @Test
    void testUpdateUserRole() {
        assertDoesNotThrow(() -> {
            userService.updateUserRole(2L, RoleConstant.ADMIN);
        });
    }

    @Test
    void testUpdateUserStatus() {
        assertDoesNotThrow(() -> {
            userService.updateUserStatus(2L, 0);
        });
    }

    @Test
    void testDeleteUserCannotDeleteAdmin() {
        assertThrows(BusinessException.class, () -> {
            userService.deleteUser(1L);
        });
    }
}
