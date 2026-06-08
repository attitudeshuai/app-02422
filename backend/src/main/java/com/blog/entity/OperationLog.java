package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String operation;

    private String method;

    private String params;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
