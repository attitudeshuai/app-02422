package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.annotation.RateLimit;
import com.blog.entity.FileUpload;
import com.blog.service.FileService;
import com.blog.utils.JwtUtil;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @RateLimit(limit = 10)
    @OperationLog("上传文件")
    public Result<FileUpload> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FileUpload fileUpload = fileService.uploadFile(file, userId);
        return Result.success("上传成功", fileUpload);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws UnsupportedEncodingException {
        FileUpload fileUpload = fileService.getFileById(id);
        File file = new File(fileUpload.getFilePath());
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String filename = URLEncoder.encode(fileUpload.getOriginalName(), "UTF-8");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除文件")
    public Result<Object> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long currentUserId = jwtUtil.getUserIdFromToken(token);
        Integer currentUserRole = jwtUtil.getRoleFromToken(token);
        
        fileService.deleteFile(id, currentUserId, currentUserRole);
        return Result.success("删除成功");
    }
}
