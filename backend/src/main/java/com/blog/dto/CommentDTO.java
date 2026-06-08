package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CommentDTO {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
