package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.client.BaiduFaceClient;
import com.example.config.BaiduFaceProperties;
import com.example.domain.po.FaceFeature;
import com.example.domain.po.FaceRecognitionLog;
import com.example.domain.vo.RecognitionResult;
import com.example.mapper.FaceFeatureMapper;
import com.example.mapper.FaceRecognitionLogMapper;
import com.example.service.FaceRecognitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Date;

@Service
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceRecognitionLogMapper faceRecognitionLogMapper;
    private final BaiduFaceClient baiduFaceClient;
    private final BaiduFaceProperties props;
    private final Object registerLock = new Object();

    public FaceRecognitionServiceImpl(FaceFeatureMapper faceFeatureMapper,
                                      FaceRecognitionLogMapper faceRecognitionLogMapper,
                                      BaiduFaceClient baiduFaceClient,
                                      BaiduFaceProperties props) {
        this.faceFeatureMapper = faceFeatureMapper;
        this.faceRecognitionLogMapper = faceRecognitionLogMapper;
        this.baiduFaceClient = baiduFaceClient;
        this.props = props;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerFeature(Long userId, String imageBase64) {
        String cleaned = cleanBase64(imageBase64);
        synchronized (registerLock) {
            try {
                BaiduFaceClient.FaceRegisterResult remote = baiduFaceClient.register(userId, cleaned);
                if (!remote.success()) {
                    recordLog(userId, null, false, 0.0, "baidu_register_fail");
                    return false;
                }

                FaceFeature feature = new FaceFeature();
                feature.setUserId(userId);
                feature.setFaceImage(Base64.getDecoder().decode(cleaned));
                feature.setFeature(null);
                feature.setFeatureHash("baidu");
                Date now = new Date();
                feature.setUpdatedAt(now);
                feature.setCreatedAt(now);

                FaceFeature existing = faceFeatureMapper.selectById(userId);
                if (existing == null) {
                    faceFeatureMapper.insert(feature);
                } else {
                    feature.setCreatedAt(existing.getCreatedAt());
                    faceFeatureMapper.updateById(feature);
                }
                recordLog(userId, null, true, 100.0, "baidu_register_ok");
                return true;
            } catch (Exception e) {
                recordLog(userId, null, false, 0.0, "register_fail");
                throw new RuntimeException("register face failed", e);
            }
        }
    }

    @Override
    public RecognitionResult recognize(String imageBase64, String reqIp) {
        String cleaned = cleanBase64(imageBase64);
        try {
            Long count = faceFeatureMapper.selectCount(new LambdaQueryWrapper<>());
            if (count == null || count == 0) {
                recordLog(null, reqIp, false, 0.0, "no_registered");
                return new RecognitionResult(false, null, 0.0, "暂无已注册人脸，请先注册");
            }

            BaiduFaceClient.FaceSearchResult searchResult = baiduFaceClient.search(cleaned);
            if (!searchResult.success()) {
                recordLog(null, reqIp, false, 0.0, "baidu_search_fail");
                return new RecognitionResult(false, null, 0.0, "识别失败：" + searchResult.error());
            }

            Long bestUserId = toLong(searchResult.userId());
            double bestScore = searchResult.score();
            if (bestUserId != null && bestScore >= props.getMatchThreshold()) {
                recordLog(bestUserId, reqIp, true, bestScore, "baidu_match");
                return new RecognitionResult(true, bestUserId, bestScore, "识别成功");
            }

            recordLog(bestUserId, reqIp, false, bestScore, "baidu_no_match");
            return new RecognitionResult(false, bestUserId, bestScore, "未匹配到人脸");
        } catch (Exception e) {
            recordLog(null, reqIp, false, 0.0, "recognize_error");
            return new RecognitionResult(false, null, 0.0, "识别失败");
        }
    }

    private String cleanBase64(String data) {
        if (!StringUtils.hasText(data)) {
            throw new IllegalArgumentException("image data is blank");
        }
        int comma = data.indexOf(',');
        return comma >= 0 ? data.substring(comma + 1) : data;
    }

    private Long toLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void recordLog(Long userId, String reqIp, boolean matched, Double score, String remark) {
        FaceRecognitionLog log = new FaceRecognitionLog();
        log.setUserId(userId);
        log.setReqIp(reqIp);
        log.setMatched(matched ? 1 : 0);
        log.setScore(score);
        log.setRemark(remark);
        log.setCreatedAt(new Date());
        faceRecognitionLogMapper.insert(log);
    }
}
