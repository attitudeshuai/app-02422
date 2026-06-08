package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.constant.RoleConstant;
import com.blog.dto.UserUpdateDTO;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.UserMapper;
import com.blog.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 * 
 * 提供用户管理的核心业务功能，包括：
 * - 用户信息的查询和更新
 * - 用户列表的分页查询和搜索
 * - 用户角色和状态管理（管理员功能）
 * - 用户权限验证
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询用户列表
     * 
     * 支持关键词搜索，可在用户名、昵称、邮箱中模糊匹配
     * 结果按创建时间倒序排列
     * 
     * 权限要求：仅管理员可调用
     * 
     * @param page 页码，从1开始
     * @param size 每页大小
     * @param keyword 搜索关键词，可选，支持用户名/昵称/邮箱模糊搜索
     * @return 分页结果，包含用户列表和分页信息
     */
    public IPage<UserVO> getUserList(Integer page, Integer size, String keyword) {
        // 构建分页参数
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 如果提供了关键词，进行多字段模糊搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                   .or().like(User::getNickname, keyword)
                   .or().like(User::getEmail, keyword);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> userPage = userMapper.selectPage(pageParam, wrapper);
        
        // 转换为VO对象（隐藏敏感信息如密码）
        return userPage.convert(this::convertToVO);
    }

    /**
     * 根据ID获取用户信息
     * 
     * 访问控制：
     * - 用户查看自己的信息：返回完整信息
     * - 管理员查看任何用户：返回完整信息
     * - 其他情况：隐藏敏感信息（email等）
     * 
     * @param id 用户ID
     * @return 用户信息VO
     * @throws BusinessException 用户不存在时抛出异常
     */
    public UserVO getUserById(Long id) {
        return getUserById(id, null, null);
    }

    /**
     * 根据ID获取用户信息（带访问控制）
     * 
     * 访问控制：
     * - 用户查看自己的信息：返回完整信息
     * - 管理员查看任何用户：返回完整信息
     * - 其他情况：隐藏敏感信息（email等）
     * 
     * @param id 用户ID
     * @param currentUserId 当前登录用户ID，未登录时为null
     * @param currentUserRole 当前用户角色，未登录时为null
     * @return 用户信息VO（可能隐藏敏感字段）
     * @throws BusinessException 用户不存在时抛出异常
     */
    public UserVO getUserById(Long id, Long currentUserId, Integer currentUserRole) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserVO vo = convertToVO(user);
        
        // 访问控制：非自己且非管理员，隐藏敏感信息
        boolean isSelf = currentUserId != null && currentUserId.equals(id);
        boolean isAdmin = currentUserRole != null && currentUserRole.equals(RoleConstant.ADMIN);
        
        if (!isSelf && !isAdmin) {
            // 隐藏敏感字段
            vo.setEmail(null);
            vo.setStatus(null);
        }
        
        return vo;
    }

    /**
     * 更新用户信息
     * 
     * 权限控制：
     * - 用户可以修改自己的信息（昵称、邮箱、头像）
     * - 管理员可以修改任何用户的信息
     * 
     * 业务规则：
     * - 邮箱不能与其他用户重复
     * - 只更新提供的字段，未提供的字段保持不变
     * 
     * @param userId 要修改的用户ID
     * @param currentUserId 当前登录用户ID
     * @param currentUserRole 当前用户角色
     * @param dto 更新的用户数据
     * @throws BusinessException 无权限、用户不存在或邮箱重复时抛出异常
     */
    public void updateUser(Long userId, Long currentUserId, Integer currentUserRole, UserUpdateDTO dto) {
        // 权限验证：只能修改自己的信息，或者管理员可以修改任何人
        if (!userId.equals(currentUserId) && !currentUserRole.equals(RoleConstant.ADMIN)) {
            throw new BusinessException("无权修改其他用户信息");
        }

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 如果要修改邮箱，检查邮箱是否已被其他用户使用
        if (dto.getEmail() != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, dto.getEmail()).ne(User::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("邮箱已被使用");
            }
            user.setEmail(dto.getEmail());
        }

        // 更新昵称（如果提供）
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }

        // 更新头像（如果提供）
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }

        // 保存更新
        userMapper.updateById(user);
    }

    /**
     * 删除用户
     * 
     * 权限要求：仅管理员可调用
     * 
     * 业务规则：
     * - 不能删除管理员账号（防止误删）
     * - 使用逻辑删除，不会物理删除数据
     * 
     * @param id 用户ID
     * @throws BusinessException 用户不存在或尝试删除管理员时抛出异常
     */
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 防止删除管理员账号
        if (user.getRole().equals(RoleConstant.ADMIN)) {
            throw new BusinessException("不能删除管理员账号");
        }
        
        // 逻辑删除用户
        userMapper.deleteById(id);
    }

    /**
     * 更新用户角色
     * 
     * 权限要求：仅管理员可调用
     * 
     * @param id 用户ID
     * @param role 新角色，0-管理员 1-普通用户
     * @throws BusinessException 用户不存在时抛出异常
     */
    public void updateUserRole(Long id, Integer role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    /**
     * 更新用户状态
     * 
     * 权限要求：仅管理员可调用
     * 
     * 业务规则：
     * - 不能禁用管理员账号（防止误操作导致无法管理系统）
     * 
     * @param id 用户ID
     * @param status 新状态，0-禁用 1-启用
     * @throws BusinessException 用户不存在或尝试禁用管理员时抛出异常
     */
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 防止禁用管理员账号
        if (user.getRole().equals(RoleConstant.ADMIN)) {
            throw new BusinessException("不能禁用管理员账号");
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
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
