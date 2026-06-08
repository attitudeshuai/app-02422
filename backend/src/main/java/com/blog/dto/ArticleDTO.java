package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ArticleDTO {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Long categoryId;

    private String coverImage;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private List<Long> tagIds;
}
