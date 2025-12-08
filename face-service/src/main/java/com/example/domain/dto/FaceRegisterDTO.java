package com.example.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaceRegisterDTO {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    /**
     * Base64（不含 data:image/jpeg;base64, 前缀）
     */
    @NotBlank(message = "file 不能为空")
    private String file;
}
