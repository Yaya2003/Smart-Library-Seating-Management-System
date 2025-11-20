package com.example.domain.dto;

import com.example.common.gson.StringToString1DTypeAdaptor;
import com.google.gson.annotations.JsonAdapter;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NonNull;

@Data
public class UserRegisterDTO {
    @NotNull(message = "id不能为空")
    private Long userId;
    @NotEmpty(message = "用户名不能为空")
    private String userName;
    private String gender;
    private Integer age;

    @NonNull
    private String password = "$2a$10$LZIYw70bwSUTiNTAT6slZOaORtjMWbQPVFjYTovrtV6WECkiSypxO";
    private String className;
    private String department;

    private String email;

    private String phone;

    private String state;

    @JsonAdapter(value = StringToString1DTypeAdaptor.class)
    private String roles;
}
