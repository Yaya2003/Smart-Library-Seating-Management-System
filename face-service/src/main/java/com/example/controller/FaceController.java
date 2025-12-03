package com.example.controller;

import com.example.common.result.ResultView;
import com.example.domain.dto.FaceRegisterDTO;
import com.example.service.FaceRecognitionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {
        "http://localhost:8080",
        "http://127.0.0.1:8080",
        "http://localhost:9527",
        "http://192.168.96.136:9527",
        "https://192.168.96.136:9527"
})
public class FaceController {

    private final FaceRecognitionService faceRecognitionService;

    public FaceController(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    @PostMapping("/register")
    public ResultView<Boolean> registerFace(@RequestBody @Valid FaceRegisterDTO body) {
        try {
            boolean ok = faceRecognitionService.registerFeature(body.getUserId(), body.getFile());
            if (ok) {
                return ResultView.success(true);
            }
            return ResultView.error(500, "注册失败，请重试");
        } catch (IllegalStateException ex) {
            return ResultView.error(400, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResultView.error(400, "图片数据无效");
        } catch (Exception ex) {
            return ResultView.error(500, "注册失败");
        }
    }
}
