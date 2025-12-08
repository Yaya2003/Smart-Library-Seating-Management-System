package com.example.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CollegeImportDTO {
    @ExcelProperty("学院")
    private String collegeName;

    @ExcelProperty("学院编码")
    private String collegeCode;

    @ExcelProperty("备注")
    private String remark;
}
