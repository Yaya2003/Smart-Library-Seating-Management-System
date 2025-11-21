package com.example.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TeacherImportDTO {
    @ExcelProperty("工号")
    private Long userId;

    @ExcelProperty("老师名")
    private String userName;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("年龄")
    private Integer age;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("学院")
    private String collegeName;
}
