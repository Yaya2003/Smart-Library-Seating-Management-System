package com.example.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotNull(message = "id cannot be null")
    private Long userId;
    @NotEmpty(message = "userName cannot be empty")
    private String userName;
    @NotEmpty(message = "password cannot be empty")
    private String password;
}
