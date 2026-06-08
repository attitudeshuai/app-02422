package com.blog.service;

import com.blog.entity.Category;
import com.blog.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void testGetAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        assertNotNull(categories);
        assertTrue(categories.size() >= 3); // 初始化了3个分类
    }

    @Test
    void testGetCategoryById() {
        Category category = categoryService.getCategoryById(1L);
        assertNotNull(category);
        assertEquals("技术", category.getName());
    }

    @Test
    void testCreateCategory() {
        Category category = new Category();
        category.setName("测试分类");
        category.setDescription("测试描述");

        Long id = categoryService.createCategory(category);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void testCreateCategoryDuplicateName() {
        Category category = new Category();
        category.setName("技术"); // 已存在的分类名

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(category);
        });
    }

    @Test
    void testUpdateCategory() {
        Category category = new Category();
        category.setName("更新后的分类");
        category.setDescription("更新后的描述");

        assertDoesNotThrow(() -> {
            categoryService.updateCategory(1L, category);
        });
    }

    @Test
    void testUpdateCategoryNotFound() {
        Category category = new Category();
        category.setName("不存在的分类");

        assertThrows(BusinessException.class, () -> {
            categoryService.updateCategory(99999L, category);
        });
    }

    @Test
    void testDeleteCategory() {
        // 先创建一个测试分类
        Category category = new Category();
        category.setName("待删除分类");
        Long id = categoryService.createCategory(category);

        // 删除
        assertDoesNotThrow(() -> {
            categoryService.deleteCategory(id);
        });
    }
}
