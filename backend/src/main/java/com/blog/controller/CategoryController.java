package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.entity.Category;
import com.blog.service.CategoryService;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("创建分类")
    public Result<Long> createCategory(@Valid @RequestBody Category category) {
        Long id = categoryService.createCategory(category);
        return Result.success("创建成功", id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("更新分类")
    public Result<Object> updateCategory(@PathVariable Long id, @Valid @RequestBody Category category) {
        categoryService.updateCategory(id, category);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog("删除分类")
    public Result<Object> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}
