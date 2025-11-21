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
        container.setMaxTextMessageBufferSize(512 * 1024);   // 512KB
        container.setMaxBinaryMessageBufferSize(512 * 1024); // 512KB
        return container;
    }
}
