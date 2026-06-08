package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Tag;
import com.blog.exception.BusinessException;
import com.blog.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务类
 * 
 * 提供文章标签管理的核心业务功能，包括：
 * - 标签的增删改查操作
 * - 标签名称唯一性验证
 * 
 * 权限要求：
 * - 查询标签：所有用户（包括未登录）
 * - 创建/更新/删除标签：登录用户
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class TagService {

    @Autowired
    private TagMapper tagMapper;

    /**
     * 获取所有标签
     * 
     * 返回所有标签列表，按创建时间倒序排列
     * 
     * @return 标签列表
     */
    public List<Tag> getAllTags() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getCreateTime);
        return tagMapper.selectList(wrapper);
    }

    /**
     * 根据ID获取标签
     * 
     * @param id 标签ID
     * @return 标签信息
     * @throws BusinessException 标签不存在时抛出异常
     */
    public Tag getTagById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }

    /**
     * 创建标签
     * 
     * 业务规则：
     * - 标签名称必须唯一
     * 
     * @param tag 标签信息，包含名称
     * @return 新创建的标签ID
     * @throws BusinessException 标签名称已存在时抛出异常
     */
    public Long createTag(Tag tag) {
        // 检查标签名称是否已存在
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, tag.getName());
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("标签名称已存在");
        }
        
        tagMapper.insert(tag);
        return tag.getId();
    }

    /**
     * 更新标签
     * 
     * 业务规则：
     * - 标签名称必须唯一（不能与其他标签重名）
     * 
     * @param id 标签ID
     * @param tag 更新的标签信息
     * @throws BusinessException 标签不存在或名称重复时抛出异常
     */
    public void updateTag(Long id, Tag tag) {
        // 验证标签是否存在
        Tag existTag = tagMapper.selectById(id);
        if (existTag == null) {
            throw new BusinessException("标签不存在");
        }

        // 检查新名称是否与其他标签重复
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, tag.getName()).ne(Tag::getId, id);
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("标签名称已存在");
        }

        // 更新标签名称
        existTag.setName(tag.getName());
        tagMapper.updateById(existTag);
    }

    /**
     * 删除标签
     * 
     * 注意：使用逻辑删除，不会物理删除数据
     * 
     * @param id 标签ID
     * @throws BusinessException 标签不存在时抛出异常
     */
    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        tagMapper.deleteById(id);
    }
}
