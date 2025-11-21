package com.example.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    private String userName;

    @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式错误")
    private String email;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式错误")
    private String phone;

    private String className;

    private String department;

    /**
     * 性别：1 男 2 女
     */
    private String gender;

    private Integer age;
}
