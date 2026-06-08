package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.RoleConstant;
import com.blog.constant.StatusConstant;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.UserMapper;
import com.blog.utils.JwtUtil;
import com.blog.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务类
 * 
 * 提供用户认证相关的核心业务功能，包括：
 * - 用户注册（账号创建和初始化）
 * - 用户登录（身份验证和Token生成）
 * - 获取当前登录用户信息
 * 
 * 安全特性：
 * - 使用BCrypt加密存储密码
 * - 使用Spring Security进行身份验证
 * - 使用JWT Token实现无状态认证
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 用户注册
     * 
     * 业务逻辑：
     * 1. 验证用户名是否已存在
     * 2. 验证邮箱是否已被注册
     * 3. 使用BCrypt加密密码
     * 4. 创建用户账号（默认为普通用户，启用状态）
     * 5. 生成JWT Token
     * 
     * @param dto 注册信息，包含用户名、密码、邮箱、昵称
     * @return Map包含token和用户信息
     * @throws BusinessException 用户名或邮箱已存在时抛出异常
     */
    public Map<String, Object> register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已被注册
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, dto.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建用户对象
        User user = new User();
        user.setUsername(dto.getUsername());
        // 使用BCrypt加密密码（单向加密，不可逆）
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        // 如果未提供昵称，使用用户名作为昵称
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        // 默认为普通用户角色
        user.setRole(RoleConstant.USER);
        // 默认为启用状态
        user.setStatus(StatusConstant.ENABLED);

        // 保存用户到数据库
        userMapper.insert(user);

        // 生成JWT Token（包含用户名、用户ID、角色信息）
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        // 返回Token和用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", convertToVO(user));
        return result;
    }

    /**
     * 用户登录
     * 
     * 业务逻辑：
     * 1. 使用Spring Security的AuthenticationManager验证用户名和密码
     * 2. 验证成功后查询用户完整信息
     * 3. 生成JWT Token
     * 
     * 安全机制：
     * - 密码验证由Spring Security完成，自动进行BCrypt解密比对
     * - 验证失败会抛出BadCredentialsException，由全局异常处理器处理
     * 
     * @param dto 登录信息，包含用户名和密码
     * @return Map包含token和用户信息
     * @throws BusinessException 用户名或密码错误时抛出异常
     */
    public Map<String, Object> login(LoginDTO dto) {
        // 使用Spring Security进行身份验证
        // 如果验证失败，会抛出BadCredentialsException
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        // 验证成功，查询用户完整信息
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        // 返回Token和用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", convertToVO(user));
        return result;
    }

    /**
     * 获取当前登录用户信息
     * 
     * 用于前端获取用户详细信息，或刷新用户状态
     * 
     * @param userId 用户ID（从JWT Token中解析得到）
     * @return 用户信息VO
     * @throws BusinessException 用户不存在时抛出异常
     */
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    /**
     * 将User实体转换为UserVO
     * 
     * 转换过程中会隐藏敏感信息（如密码）
     * 
     * @param user 用户实体
     * @return 用户VO对象
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
