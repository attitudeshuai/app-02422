package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.Email;

@Data
public class UserUpdateDTO {
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    private String avatar;
}
