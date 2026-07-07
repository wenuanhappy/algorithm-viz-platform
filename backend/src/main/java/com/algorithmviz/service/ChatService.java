package com.algorithmviz.service;

import com.algorithmviz.entity.AppUser;
import com.algorithmviz.entity.ChatMessage;
import com.algorithmviz.entity.ChatSession;
import com.algorithmviz.repository.AppUserRepository;
import com.algorithmviz.repository.ChatMessageRepository;
import com.algorithmviz.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AppUserRepository userRepository;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       AppUserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatSession createSession(Long userId, String title) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(title);
        return sessionRepository.save(session);
    }

    public List<ChatSession> getUserSessions(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return sessionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        return messageRepository.findBySessionOrderByCreatedAtAsc(session);
    }

    @Transactional
    public ChatMessage addMessage(Long sessionId, String role, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
