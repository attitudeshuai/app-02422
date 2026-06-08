package com.blog.service;

import com.blog.entity.Tag;
import com.blog.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Test
    void testGetAllTags() {
        List<Tag> tags = tagService.getAllTags();
        assertNotNull(tags);
        assertTrue(tags.size() >= 5); // 初始化了5个标签
    }

    @Test
    void testGetTagById() {
        Tag tag = tagService.getTagById(1L);
        assertNotNull(tag);
        assertEquals("Java", tag.getName());
    }

    @Test
    void testCreateTag() {
        Tag tag = new Tag();
        tag.setName("测试标签");

        Long id = tagService.createTag(tag);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void testCreateTagDuplicateName() {
        Tag tag = new Tag();
        tag.setName("Java"); // 已存在的标签名

        assertThrows(BusinessException.class, () -> {
            tagService.createTag(tag);
        });
    }

    @Test
    void testUpdateTag() {
        Tag tag = new Tag();
        tag.setName("更新后的标签");

        assertDoesNotThrow(() -> {
            tagService.updateTag(1L, tag);
        });
    }

    @Test
    void testUpdateTagNotFound() {
        Tag tag = new Tag();
        tag.setName("不存在的标签");

        assertThrows(BusinessException.class, () -> {
            tagService.updateTag(99999L, tag);
        });
    }

    @Test
    void testDeleteTag() {
        // 先创建一个测试标签
        Tag tag = new Tag();
        tag.setName("待删除标签");
        Long id = tagService.createTag(tag);

        // 删除
        assertDoesNotThrow(() -> {
            tagService.deleteTag(id);
        });
    }
}
