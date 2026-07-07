package com.algorithmviz.controller;

import com.algorithmviz.entity.ChatMessage;
import com.algorithmviz.entity.ChatSession;
import com.algorithmviz.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createSession(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String title = (String) body.get("title");
        return ResponseEntity.ok(chatService.createSession(userId, title));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getUserSessions(@RequestParam Long userId) {
        return ResponseEntity.ok(chatService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessage> addMessage(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        String content = body.get("content");
        return ResponseEntity.ok(chatService.addMessage(sessionId, role, content));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
