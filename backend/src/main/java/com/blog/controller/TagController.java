package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.entity.Tag;
import com.blog.service.TagService;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public Result<List<Tag>> getAllTags() {
        return Result.success(tagService.getAllTags());
    }

    @GetMapping("/{id}")
    public Result<Tag> getTagById(@PathVariable Long id) {
        return Result.success(tagService.getTagById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("创建标签")
    public Result<Long> createTag(@Valid @RequestBody Tag tag) {
        Long id = tagService.createTag(tag);
        return Result.success("创建成功", id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("更新标签")
    public Result<Object> updateTag(@PathVariable Long id, @Valid @RequestBody Tag tag) {
        tagService.updateTag(id, tag);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除标签")
    public Result<Object> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success("删除成功");
    }
}
