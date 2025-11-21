package com.example.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ClassImportDTO {
    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("学院")
    private String collegeName;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("备注")
    private String remark;
}
