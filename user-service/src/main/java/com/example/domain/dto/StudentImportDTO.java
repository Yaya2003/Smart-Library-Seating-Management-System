package com.example.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportDTO {
    @ExcelProperty("学号")
    private Long userId;

    @ExcelProperty("学生名")
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

    @ExcelProperty("班级")
    private String className;
}
