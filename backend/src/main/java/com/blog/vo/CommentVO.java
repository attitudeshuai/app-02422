package com.blog.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long parentId;
    private String content;
    private LocalDateTime createTime;
}
