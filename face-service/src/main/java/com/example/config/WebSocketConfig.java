package com.example.config;

import com.example.websocket.RecognizeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RecognizeWebSocketHandler recognizeWebSocketHandler;

    public WebSocketConfig(RecognizeWebSocketHandler recognizeWebSocketHandler) {
        this.recognizeWebSocketHandler = recognizeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(recognizeWebSocketHandler, "/ws/recognize")
                .setAllowedOrigins("*");
    }
}
