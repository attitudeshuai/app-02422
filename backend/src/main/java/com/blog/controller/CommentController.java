package com.blog.controller;

import com.blog.annotation.OperationLog;
import com.blog.dto.CommentDTO;
import com.blog.service.CommentService;
import com.blog.utils.JwtUtil;
import com.blog.vo.CommentVO;
import com.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<List<CommentVO>> getCommentsByArticleId(@RequestParam Long articleId) {
        return Result.success(commentService.getCommentsByArticleId(articleId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("发表评论")
    public Result<Long> createComment(@Validated @RequestBody CommentDTO dto, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long commentId = commentService.createComment(dto, userId);
        return Result.success("评论成功", commentId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @OperationLog("删除评论")
    public Result<Object> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long currentUserId = jwtUtil.getUserIdFromToken(token);
        Integer currentUserRole = jwtUtil.getRoleFromToken(token);
        
        commentService.deleteComment(id, currentUserId, currentUserRole);
        return Result.success("删除成功");
    }
}
