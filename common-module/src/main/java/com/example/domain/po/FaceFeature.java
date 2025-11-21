package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("face_feature")
public class FaceFeature {

    @TableId(type = IdType.INPUT)
    private Long userId;

    /**
     * 存储计算后的人脸特征。当前占位为特征摘要的字节，后续可替换为实际 embedding。
     */
    private byte[] feature;

    /**
     * 可选：原始或裁剪后的人脸图片（Base64 解码后存储）。
     */
    private byte[] faceImage;

    /**
     * 特征摘要，用于快速比对/去重（如 SHA-256）。
     */
    private String featureHash;

    private Date createdAt;
    private Date updatedAt;
}
