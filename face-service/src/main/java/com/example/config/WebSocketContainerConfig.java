package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * 调大 WebSocket 文本帧大小，避免 Base64 图片过大被关闭。
 */
@Configuration
public class WebSocketContainerConfig {

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(10 * 1024 * 1024);   // 增加到10MB
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024); // 增加到10MB
        return container;
    }
}
