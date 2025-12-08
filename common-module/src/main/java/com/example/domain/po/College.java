package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 学院实体
 */
@Data
@TableName("college")
public class College {
    @TableId(type = IdType.AUTO)
    private Long collegeId;
    private String collegeName;
    private String collegeCode;
    private String remark;
    private Date createAt;
    private Date updateAt;
}
