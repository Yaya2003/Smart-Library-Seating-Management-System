package com.example.service;

public interface FaceRecognitionService {

    /**
     * 注册人脸特征。
     *
     * @param userId      用户 ID
     * @param imageBase64 去掉前缀的 Base64 图片数据
     * @return 是否成功
     */
    boolean registerFeature(Long userId, String imageBase64);

    /**
     * 识别并返回匹配到的用户 ID。
     *
     * @param imageBase64 去掉前缀的 Base64 图片数据
     * @param reqIp       请求来源 IP
     * @return 匹配到的用户 ID
     */
    com.example.domain.vo.RecognitionResult recognize(String imageBase64, String reqIp);
}
