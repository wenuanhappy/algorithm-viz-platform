package com.algorithmviz.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.algorithmviz.service.CompetitionRoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

@Component
public class SignalingHandler extends TextWebSocketHandler {
    private static final Set<String> RELAYED_SIGNAL_TYPES = Set.of("offer", "answer", "ice-candidate");

    private final ObjectMapper objectMapper;
    private final CompetitionRoomService competitionRoomService;
    private final Map<String, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();
    private final Map<String, String> roomBySession = new ConcurrentHashMap<>();
    private final Map<String, Long> userBySession = new ConcurrentHashMap<>();
    private final ScheduledExecutorService disconnectScheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "competition-disconnect-check");
        thread.setDaemon(true);
        return thread;
    });

    public SignalingHandler(ObjectMapper objectMapper, CompetitionRoomService competitionRoomService) {
        this.objectMapper = objectMapper;
        this.competitionRoomService = competitionRoomService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.path("type").asText();
        String roomId = payload.path("roomId").asText("").trim().toUpperCase(Locale.ROOT);
        if (type.isBlank() || roomId.isBlank()) {
            return;
        }

        if ("join-room".equals(type)) {
            Long userId = payload.hasNonNull("userId") ? payload.path("userId").asLong() : null;
            if (!competitionRoomService.isPlayerInRoom(roomId, userId)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            joinRoom(session, roomId, userId, message);
            return;
        }

        String sessionRoomId = roomBySession.get(session.getId());
        if (sessionRoomId == null || !sessionRoomId.equals(roomId)) {
            return;
        }
        Long sessionUserId = userBySession.get(session.getId());
        if (!RELAYED_SIGNAL_TYPES.contains(type)
                || !payload.hasNonNull("from")
                || sessionUserId == null
                || payload.path("from").asLong() != sessionUserId) {
            return;
        }
        broadcast(sessionRoomId, session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = roomBySession.remove(session.getId());
        Long userId = userBySession.remove(session.getId());
        if (roomId != null) {
            Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                broadcast(roomId, session, new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "peer-left",
                        "roomId", roomId
                ))));
                if (sessions.isEmpty()) {
                    sessionsByRoom.remove(roomId);
                }
            }
            if (userId != null) {
                scheduleDisconnectCheck(roomId, userId);
            }
        }
    }

    private void joinRoom(WebSocketSession session, String roomId, Long userId, TextMessage message) throws IOException {
        String previousRoom = roomBySession.put(session.getId(), roomId);
        userBySession.put(session.getId(), userId);
        if (previousRoom != null && !previousRoom.equals(roomId)) {
            Set<WebSocketSession> previousSessions = sessionsByRoom.get(previousRoom);
            if (previousSessions != null) {
                previousSessions.remove(session);
            }
        }
        Set<WebSocketSession> roomSessions = sessionsByRoom.computeIfAbsent(
                roomId,
                ignored -> ConcurrentHashMap.newKeySet()
        );
        roomSessions.stream()
                .filter(existing -> existing.isOpen() && !userId.equals(userBySession.get(existing.getId())))
                .findFirst()
                .ifPresent(existing -> sendPeerPresent(session, roomId, userBySession.get(existing.getId())));
        roomSessions.add(session);
        broadcast(roomId, session, message);
    }

    private void sendPeerPresent(WebSocketSession session, String roomId, Long peerUserId) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "peer-present",
                    "roomId", roomId,
                    "peerUserId", peerUserId
            ))));
        } catch (IOException ignored) {
            // A closed joining socket will be handled by afterConnectionClosed.
        }
    }

    private void scheduleDisconnectCheck(String roomId, Long userId) {
        disconnectScheduler.schedule(() -> {
            if (hasActiveSession(roomId, userId)) return;
            try {
                competitionRoomService.leaveRoom(roomId, userId);
            } catch (IllegalArgumentException ignored) {
                // The player or room may already have been removed through the REST leave endpoint.
            }
        }, 15, TimeUnit.SECONDS);
    }

    private boolean hasActiveSession(String roomId, Long userId) {
        Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        return sessions != null && sessions.stream().anyMatch(session ->
                session.isOpen() && userId.equals(userBySession.get(session.getId())));
    }

    @PreDestroy
    public void shutdown() {
        disconnectScheduler.shutdownNow();
    }

    private void broadcast(String roomId, WebSocketSession sender, TextMessage message) throws IOException {
        Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        if (sessions == null) return;

        for (WebSocketSession candidate : sessions) {
            if (!candidate.getId().equals(sender.getId()) && candidate.isOpen()) {
                candidate.sendMessage(message);
            }
        }
    }
}
