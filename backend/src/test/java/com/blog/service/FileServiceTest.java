package com.blog.service;

import com.blog.constant.RoleConstant;
import com.blog.entity.FileUpload;
import com.blog.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FileServiceTest {

    @Autowired
    private FileService fileService;

    @Test
    void testUploadFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "测试文件内容".getBytes()
        );

        FileUpload fileUpload = fileService.uploadFile(file, 1L);
        assertNotNull(fileUpload);
        assertNotNull(fileUpload.getId());
        assertEquals("test.txt", fileUpload.getOriginalName());
        assertEquals("txt", fileUpload.getFileType());
    }

    @Test
    void testUploadEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(BusinessException.class, () -> {
            fileService.uploadFile(file, 1L);
        });
    }

    @Test
    void testUploadLargeFile() {
        // 创建一个超过10MB的文件
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        assertThrows(BusinessException.class, () -> {
            fileService.uploadFile(file, 1L);
        });
    }

    @Test
    void testGetFileById() {
        // 先上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "测试内容".getBytes()
        );
        FileUpload uploaded = fileService.uploadFile(file, 1L);

        // 获取文件
        FileUpload retrieved = fileService.getFileById(uploaded.getId());
        assertNotNull(retrieved);
        assertEquals(uploaded.getId(), retrieved.getId());
    }

    @Test
    void testGetFileByIdNotFound() {
        assertThrows(BusinessException.class, () -> {
            fileService.getFileById(99999L);
        });
    }

    @Test
    void testDeleteFile() {
        // 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete.txt",
                "text/plain",
                "待删除".getBytes()
        );
        FileUpload uploaded = fileService.uploadFile(file, 1L);

        // 删除文件（用户删除自己的文件）
        assertDoesNotThrow(() -> {
            fileService.deleteFile(uploaded.getId(), 1L, RoleConstant.USER);
        });
    }

    @Test
    void testDeleteFileUnauthorized() {
        // 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "other.txt",
                "text/plain",
                "其他用户文件".getBytes()
        );
        FileUpload uploaded = fileService.uploadFile(file, 1L);

        // 其他用户尝试删除
        assertThrows(BusinessException.class, () -> {
            fileService.deleteFile(uploaded.getId(), 2L, RoleConstant.USER);
        });
    }

    @Test
    void testDeleteFileByAdmin() {
        // 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "user_file.txt",
                "text/plain",
                "用户文件".getBytes()
        );
        FileUpload uploaded = fileService.uploadFile(file, 2L);

        // 管理员删除
        assertDoesNotThrow(() -> {
            fileService.deleteFile(uploaded.getId(), 1L, RoleConstant.ADMIN);
        });
    }
}
