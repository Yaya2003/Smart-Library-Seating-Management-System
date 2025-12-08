package com.example.config;

import com.example.websocket.SeatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SeatWebSocketHandler seatWebSocketHandler;

    public WebSocketConfig(SeatWebSocketHandler seatWebSocketHandler) {
        this.seatWebSocketHandler = seatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(seatWebSocketHandler, "/ws/reservation")
                .setAllowedOrigins("*");
    }
}

