package com.blog.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.StatusConstant;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 使用Spring Security的DisabledException处理禁用账号
        // 这会被AuthenticationProvider正确捕获并返回401/403而非500
        if (user.getStatus().equals(StatusConstant.DISABLED)) {
            throw new DisabledException("用户已被禁用");
        }

        String role = user.getRole() == 0 ? "ROLE_ADMIN" : "ROLE_USER";
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
