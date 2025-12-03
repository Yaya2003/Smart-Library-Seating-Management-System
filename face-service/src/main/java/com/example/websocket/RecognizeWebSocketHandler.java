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
        String payload = message.getPayload();
        String ip = resolveIp(session);

        // 避免高频帧重复调用外部接口：若正在处理，直接忽略当前帧
        Object busy = session.getAttributes().getOrDefault("busy", false);
        if (busy instanceof Boolean && (Boolean) busy) {
            return;
        }
        session.getAttributes().put("busy", true);
        try {
            RecognitionResult result = faceRecognitionService.recognize(payload, ip);
            if (result.isMatched()) {
                writeMessage(session, gson.toJson(new WsResponse(200, result.getMessage(), result.getUserId(), result.getScore())));
            } else {
                int code = result.getUserId() == null ? 400 : 409;
                writeMessage(session, gson.toJson(new WsResponse(code, result.getMessage(), result.getUserId(), result.getScore())));
            }
        } catch (Exception e) {
            log.error("WebSocket recognize error", e);
            writeMessage(session, gson.toJson(new WsResponse(500, "识别失败", null, 0.0)));
        } finally {
            session.getAttributes().put("busy", false);
        }
    }

    private String resolveIp(WebSocketSession session) {
        InetSocketAddress address = session.getRemoteAddress();
        return address == null ? null : address.getHostString();
    }

    private void writeMessage(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
            System.out.println("发送的消息"+new TextMessage(payload));
        } catch (IOException e) {
            log.warn("send websocket message failed", e);
            System.out.println("报错的消息："+e);
        }
    }

    private record WsResponse(int code, String msg, Long data, double score) {
    }
}
