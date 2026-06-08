package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Category;
import com.blog.exception.BusinessException;
import com.blog.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务类
 * 
 * 提供文章分类管理的核心业务功能，包括：
 * - 分类的增删改查操作
 * - 分类名称唯一性验证
 * 
 * 权限要求：
 * - 查询分类：所有用户（包括未登录）
 * - 创建/更新/删除分类：仅管理员
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 获取所有分类
     * 
     * 返回所有分类列表，按创建时间倒序排列
     * 
     * @return 分类列表
     */
    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Category::getCreateTime);
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 根据ID获取分类
     * 
     * @param id 分类ID
     * @return 分类信息
     * @throws BusinessException 分类不存在时抛出异常
     */
    public Category getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    /**
     * 创建分类
     * 
     * 业务规则：
     * - 分类名称必须唯一
     * 
     * @param category 分类信息，包含名称和描述
     * @return 新创建的分类ID
     * @throws BusinessException 分类名称已存在时抛出异常
     */
    public Long createCategory(Category category) {
        // 检查分类名称是否已存在
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, category.getName());
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        
        categoryMapper.insert(category);
        return category.getId();
    }

    /**
     * 更新分类
     * 
     * 业务规则：
     * - 分类名称必须唯一（不能与其他分类重名）
     * 
     * @param id 分类ID
     * @param category 更新的分类信息
     * @throws BusinessException 分类不存在或名称重复时抛出异常
     */
    public void updateCategory(Long id, Category category) {
        // 验证分类是否存在
        Category existCategory = categoryMapper.selectById(id);
        if (existCategory == null) {
            throw new BusinessException("分类不存在");
        }

        // 检查新名称是否与其他分类重复
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, category.getName()).ne(Category::getId, id);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }

        // 更新分类信息
        existCategory.setName(category.getName());
        existCategory.setDescription(category.getDescription());
        categoryMapper.updateById(existCategory);
    }

    /**
     * 删除分类
     * 
     * 注意：使用逻辑删除，不会物理删除数据
     * 
     * @param id 分类ID
     * @throws BusinessException 分类不存在时抛出异常
     */
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        categoryMapper.deleteById(id);
    }
}
