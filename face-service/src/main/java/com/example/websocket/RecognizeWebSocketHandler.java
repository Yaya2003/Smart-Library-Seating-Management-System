package com.example.websocket;

import com.example.domain.vo.RecognitionResult;
import com.example.service.FaceRecognitionService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component
public class RecognizeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RecognizeWebSocketHandler.class);

    private final FaceRecognitionService faceRecognitionService;
    private final Gson gson = new Gson();

    public RecognizeWebSocketHandler(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("收到WebSocket消息，sessionId: {}, 消息长度: {}", session.getId(), message.getPayloadLength());

        String payload = message.getPayload();
        String ip = resolveIp(session);

        // 检查会话是否还处于打开状态
        if (!session.isOpen()) {
            log.warn("WebSocket会话已关闭，无法处理消息");
            return;
        }

        // 避免高频帧重复调用外部接口：若正在处理，直接忽略当前帧
        Object busy = session.getAttributes().get("busy");
        if (busy instanceof Boolean && (Boolean) busy) {
            log.debug("会话繁忙，忽略当前帧");
            return;
        }

        session.getAttributes().put("busy", true);
        try {
            log.info("开始人脸识别处理，IP: {}", ip);

            // 这里直接调用，因为 recognize 方法内部已处理所有异常
            RecognitionResult result = faceRecognitionService.recognize(payload, ip);

            log.info("人脸识别完成，结果: matched={}, userId={}, score={}, message={}",
                    result.isMatched(), result.getUserId(), result.getScore(), result.getMessage());

            WsResponse wsResponse;
            if (result.isMatched()) {
                wsResponse = new WsResponse(200, result.getMessage(), result.getUserId(), result.getScore());
            } else {
                // 根据结果判断：未找到用户(400) vs 分数不够(409)
                int code = result.getUserId() == null ? 400 : 409;
                wsResponse = new WsResponse(code, result.getMessage(), result.getUserId(), result.getScore());
            }

            String jsonResponse = gson.toJson(wsResponse);
            log.info("准备发送WebSocket响应: {}", jsonResponse);

            writeMessage(session, jsonResponse);

        } catch (Exception e) {
            // 这个 catch 块理论上不应该被执行，除非 faceRecognitionService.recognize() 真的抛出异常
            log.error("WebSocket recognize error", e);
            try {
                String errorResponse = gson.toJson(new WsResponse(500, "识别失败", null, 0.0));
                writeMessage(session, errorResponse);
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        } finally {
            session.getAttributes().put("busy", false);
            log.info("处理完成，busy状态已重置");
        }
    }

    private String resolveIp(WebSocketSession session) {
        InetSocketAddress address = session.getRemoteAddress();
        return address == null ? null : address.getHostString();
    }

    private void writeMessage(WebSocketSession session, String payload) {
        try {
            if (!session.isOpen()) {
                log.warn("尝试发送消息时发现会话已关闭");
                return;
            }

            TextMessage message = new TextMessage(payload);
            session.sendMessage(message);
            log.info("成功发送WebSocket消息: {}", payload);

            // 添加这个调试输出
            System.out.println("发送的消息: " + payload);

        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
            System.out.println("发送消息失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("发送消息时发生未知异常", e);
            System.out.println("发送消息未知异常: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误，sessionId: {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接已建立，sessionId: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.info("WebSocket连接已关闭，sessionId: {}, 状态: {}", session.getId(), status);
    }

    private record WsResponse(int code, String msg, Long data, double score) {
    }
}
