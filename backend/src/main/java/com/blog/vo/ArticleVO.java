package com.blog.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private String coverImage;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    private Boolean liked;
    private List<String> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
