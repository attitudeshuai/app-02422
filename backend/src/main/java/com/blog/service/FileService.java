package com.blog.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.blog.constant.RoleConstant;
import com.blog.entity.FileUpload;
import com.blog.exception.BusinessException;
import com.blog.mapper.FileUploadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 文件服务类
 * 
 * 提供文件管理的核心业务功能，包括：
 * - 文件上传（支持图片、文档等）
 * - 文件下载
 * - 文件删除
 * - 文件信息查询
 * 
 * 安全特性：
 * - 文件大小限制（默认10MB）
 * - 文件名UUID重命名（防止文件名冲突和路径遍历攻击）
 * - 权限控制（只能删除自己上传的文件，管理员除外）
 * 
 * @author blog-system
 * @since 1.0.0
 */
@Service
public class FileService {

    @Autowired
    private FileUploadMapper fileUploadMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.max-size}")
    private Long maxSize;

    /**
     * 上传文件
     * 
     * 业务逻辑：
     * 1. 验证文件是否为空
     * 2. 验证文件大小是否超过限制
     * 3. 生成UUID文件名（防止文件名冲突）
     * 4. 保存文件到磁盘
     * 5. 记录文件信息到数据库
     * 
     * 安全措施：
     * - 使用UUID重命名文件，防止路径遍历攻击
     * - 限制文件大小，防止磁盘空间耗尽
     * 
     * @param file 上传的文件
     * @param userId 上传者ID（当前登录用户）
     * @return 文件信息，包含文件ID、原始文件名、存储路径等
     * @throws BusinessException 文件为空、超过大小限制或上传失败时抛出异常
     */
    public FileUpload uploadFile(MultipartFile file, Long userId) {
        // 验证文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);
        
        // 生成UUID文件名（防止文件名冲突和路径遍历攻击）
        String storedName = IdUtil.simpleUUID() + "." + extension;
        String filePath = uploadPath + File.separator + storedName;

        try {
            // 确保上传目录存在
            File dest = new File(filePath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            
            // 保存文件到磁盘
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        // 记录文件信息到数据库
        FileUpload fileUpload = new FileUpload();
        fileUpload.setUserId(userId);
        fileUpload.setOriginalName(originalFilename);
        fileUpload.setStoredName(storedName);
        fileUpload.setFilePath(filePath);
        fileUpload.setFileSize(file.getSize());
        fileUpload.setFileType(extension);

        fileUploadMapper.insert(fileUpload);

        return fileUpload;
    }

    /**
     * 根据ID获取文件信息
     * 
     * @param id 文件ID
     * @return 文件信息
     * @throws BusinessException 文件不存在时抛出异常
     */
    public FileUpload getFileById(Long id) {
        FileUpload file = fileUploadMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        return file;
    }

    /**
     * 删除文件
     * 
     * 权限控制：
     * - 文件上传者可以删除自己的文件
     * - 管理员可以删除任何文件
     * 
     * 业务逻辑：
     * 1. 验证文件是否存在
     * 2. 验证当前用户是否有权限删除
     * 3. 删除磁盘上的物理文件
     * 4. 删除数据库中的文件记录
     * 
     * @param id 文件ID
     * @param currentUserId 当前登录用户ID
     * @param currentUserRole 当前用户角色
     * @throws BusinessException 文件不存在或无权限时抛出异常
     */
    public void deleteFile(Long id, Long currentUserId, Integer currentUserRole) {
        // 验证文件是否存在
        FileUpload file = fileUploadMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 权限验证：只有上传者本人或管理员可以删除
        if (!file.getUserId().equals(currentUserId) && !currentUserRole.equals(RoleConstant.ADMIN)) {
            throw new BusinessException("无权删除此文件");
        }

        // 删除磁盘上的物理文件
        File diskFile = new File(file.getFilePath());
        if (diskFile.exists()) {
            diskFile.delete();
        }

        // 删除数据库记录
        fileUploadMapper.deleteById(id);
    }
}
