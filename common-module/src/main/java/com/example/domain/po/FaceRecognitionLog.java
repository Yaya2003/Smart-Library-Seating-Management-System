package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("face_recognition_log")
public class FaceRecognitionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String reqIp;

    /**
     * 是否匹配成功，1 成功，0 失败。
     */
    private Integer matched;

    /**
     * 匹配得分，0-100 或 0-1。
     */
    private Double score;

    private String remark;

    private Date createdAt;
}
