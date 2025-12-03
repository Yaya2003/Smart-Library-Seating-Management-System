package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.gson.GsonIgnore;
import com.example.common.gson.StringToString1DTypeAdaptor;
import com.google.gson.annotations.JsonAdapter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "user")
public class User {
    @TableId
    private Long userId;
    private String userName;
    private Integer age;
    private String gender;
    private String className;
    private Long classId;
    private String department;
    private Long collegeId;

    @GsonIgnore
    private String password;

    @Email
    private String email;

    @Pattern(regexp = "^1[34578]\\d{9}$")
    private String phone;

    private String state;

    private Integer faceInfo;

    @JsonAdapter(value = StringToString1DTypeAdaptor.class)
    private String roles;
    private Date createAt;
    private Date updateAt;

    /**
     * 违约封禁截止时间；当前时间早于该值则禁止预约。
     */
    @TableField("ban_until")
    private Date banUntil;

    /**
     * 累计违约次数。
     */
    @TableField("violation_count")
    private Integer violationCount;

    @TableField(exist = false)
    private Boolean defaultPassword;
}
