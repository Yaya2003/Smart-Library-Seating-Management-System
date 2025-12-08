package com.example.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    @NotEmpty(message = "oldPassword cannot be empty")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotEmpty(message = "newPassword cannot be empty")
    private String newPassword;
}

