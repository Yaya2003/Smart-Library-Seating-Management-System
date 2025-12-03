package com.example.websocket;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Push seat status changes (lock/release) in real time.
 */
@Component
public class SeatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SeatWebSocketHandler.class);

    private final Map<String, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String key = resolveKey(session);
        session.getAttributes().put("seatKey", key);
        roomSessionMap.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Seat websocket connected: {}", key);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Seat websocket transport error", exception);
        removeSession(session);
    }

    public void broadcast(SeatStatusMessage message) {
        if (message == null) {
            return;
        }
        String key = buildKey(message.getRoomId(), message.getDate(), message.getTimeSlot());
        String payload = gson.toJson(message);
        Set<WebSocketSession> sessions = roomSessionMap.getOrDefault(key, Collections.emptySet());
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.debug("Send seat websocket message failed", e);
                }
            }
        }
    }

    private void removeSession(WebSocketSession session) {
        Object keyObj = session.getAttributes().get("seatKey");
        if (keyObj == null) {
            return;
        }
        String key = keyObj.toString();
        Set<WebSocketSession> sessions = roomSessionMap.get(key);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessionMap.remove(key);
            }
        }
    }

    private String resolveKey(WebSocketSession session) {
        URI uri = session.getUri();
        Map<String, String> params = parseQuery(uri);
        Long roomId = parseLong(params.get("roomId"));
        String date = params.getOrDefault("date", "");
        String timeSlot = params.getOrDefault("timeSlot", "");
        return buildKey(roomId, date, timeSlot);
    }

    private Map<String, String> parseQuery(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new ConcurrentHashMap<>();
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private String buildKey(Long roomId, String date, String timeSlot) {
        return (roomId == null ? "any" : roomId) + "|" + date + "|" + timeSlot;
    }

    private Long parseLong(String num) {
        try {
            return num == null ? null : Long.parseLong(num);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

