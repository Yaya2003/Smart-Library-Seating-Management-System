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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Date;

@Service
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FaceRecognitionServiceImpl.class);

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
        Date now = new Date();

        synchronized (registerLock) {

            // ============================
            // #1 本地是否已有记录
            // ============================
            FaceFeature existing = faceFeatureMapper.selectById(userId);

            // ============================
            // #2 先百度 search() → 识别库中是否已有相同人脸
            // ============================
            try {
                BaiduFaceClient.FaceSearchResult search = baiduFaceClient.search(cleaned);
                if (search.success()) {
                    Long matchUserId = toLong(search.userId());
                    double score = search.score();

                    if (matchUserId != null && score >= props.getMatchThreshold()) {

                        // ---- (1) 匹配到别人 → 拒绝 ----
                        if (!matchUserId.equals(userId)) {
                            recordLog(matchUserId, null, false, score, "duplicate_face_found");
                            throw new IllegalStateException("该人脸已被用户 " + matchUserId + " 注册");
                        }

                        // ---- (2) 匹配到自己 → 已注册 ----
                        if (existing == null) {
                            // 本地无记录 → 自动补充
                            FaceFeature feature = new FaceFeature();
                            feature.setUserId(userId);
                            feature.setFaceImage(Base64.getDecoder().decode(cleaned));
                            feature.setFeatureHash("baidu");
                            feature.setFeature(null);
                            feature.setCreatedAt(now);
                            feature.setUpdatedAt(now);
                            faceFeatureMapper.insert(feature);
                        }

                        recordLog(userId, null, true, score, "already_registered_by_self");
                        return true;
                    }
                }
            } catch (Exception e) {
                // 百度搜索失败不影响注册，但记录日志
                recordLog(null, null, false, 0.0, "baidu_search_fail_before_register");
            }

            // ============================
            // #3 走百度 register()
            // ============================
            BaiduFaceClient.FaceRegisterResult remote = null;
            try {
                remote = baiduFaceClient.register(userId, cleaned);
            } catch (Exception e) {
                recordLog(userId, null, false, 0.0, "baidu_register_exception");
                throw new IllegalStateException("调用百度接口失败，请稍后重试");
            }

            // 注册失败，但属于 “已存在” 类错误 → 视为成功
            if (remote != null && !remote.success()) {
                String err = remote.error() != null ? remote.error() : "";

                if (err.contains("223105") || err.contains("223106") || err.contains("exist")) {

                    // 223105/223106 = 已存在 → 本来就是成功
                    if (existing == null) {
                        FaceFeature feature = new FaceFeature();
                        feature.setUserId(userId);
                        feature.setFaceImage(Base64.getDecoder().decode(cleaned));
                        feature.setFeatureHash("baidu");
                        feature.setFeature(null);
                        feature.setCreatedAt(now);
                        feature.setUpdatedAt(now);

                        try {
                            faceFeatureMapper.insert(feature);
                        } catch (Exception ignore) {
                            // 若唯一主键冲突，不要再抛异常
                            // 说明本地之前已经同步过，只需要忽略
                        }
                    }

                    recordLog(userId, null, true, 100.0, "baidu_register_exist_sync");
                    return true;
                }

                // 其他错误 → 真正失败
                recordLog(userId, null, false, 0.0, "baidu_register_fail_" + err);
                throw new IllegalStateException("百度注册失败：" + err);
            }

            // ============================
            // #4 百度注册成功 → 同步本地
            // ============================
            try {
                FaceFeature feature = new FaceFeature();
                feature.setUserId(userId);
                feature.setFaceImage(Base64.getDecoder().decode(cleaned));
                feature.setFeatureHash("baidu");
                feature.setFeature(null);
                feature.setUpdatedAt(now);

                if (existing == null) {
                    feature.setCreatedAt(now);
                    faceFeatureMapper.insert(feature);
                } else {
                    feature.setCreatedAt(existing.getCreatedAt());
                    faceFeatureMapper.updateById(feature);
                }
            } catch (Exception e) {
                // **注意：这里不应该算失败！百度已成功 → 本地同步失败也要视为成功**
                recordLog(userId, null, true, 100.0, "local_sync_failed_but_baidu_ok");
                return true;
            }

            recordLog(userId, null, true, 100.0, "baidu_register_ok");
            return true;
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
