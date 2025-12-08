package com.example.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ResetPasswordByEmailDTO {

    @Email(message = "email format error")
    @NotEmpty(message = "email cannot be empty")
    private String email;

    @NotEmpty(message = "code cannot be empty")
    private String code;

    @NotEmpty(message = "newPassword cannot be empty")
    private String newPassword;
}

