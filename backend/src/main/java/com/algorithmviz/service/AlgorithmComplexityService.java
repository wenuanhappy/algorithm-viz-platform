package com.algorithmviz.service;

import com.algorithmviz.dto.AlgorithmComplexityRequest;
import com.algorithmviz.model.AlgorithmComplexityAnalysis;
import com.algorithmviz.entity.ChatMessage;
import com.algorithmviz.entity.ChatSession;
import com.algorithmviz.repository.ChatMessageRepository;
import com.algorithmviz.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AlgorithmComplexityService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @Value("${ai.complexity.api-url}")
    private String apiUrl;

    @Value("${ai.complexity.api-key}")
    private String apiKey;

    @Value("${ai.complexity.model}")
    private String model;

    public AlgorithmComplexityService(ObjectMapper objectMapper,
                                      ChatSessionRepository sessionRepository,
                                      ChatMessageRepository messageRepository) {
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.restClient = RestClient.builder().build();
    }

    public AlgorithmComplexityAnalysis analyze(AlgorithmComplexityRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI_COMPLEXITY_API_KEY 未配置");
        }

        // 加载历史消息作为上下文
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "你是一个算法学习与编程帮助的对话助手。请用中文直接、清晰地回答用户问题：既可以分析算法复杂度，也可以解释代码、改进方案、给示例或逐步推理。返回自然语言答案即可（可使用必要的代码块和列表），不要强制输出 JSON。"
        ));

        boolean hasHistory = false;
        if (request.getSessionId() != null) {
            ChatSession session = sessionRepository.findById(request.getSessionId()).orElse(null);
            if (session != null) {
                List<ChatMessage> history = messageRepository.findBySessionOrderByCreatedAtAsc(session);
                if (!history.isEmpty()) {
                    hasHistory = true;
                    // 获取最近的 10 条消息作为上下文
                    int start = Math.max(0, history.size() - 10);
                    for (int i = start; i < history.size(); i++) {
                        ChatMessage msg = history.get(i);
                        messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                    }
                }
            }
        }

        // 如果没有历史记录，使用 buildPrompt 构建完整的初始输入
        // 如果有历史记录，直接使用用户当前的输入 code (在对话模式下 code 字段承载的是用户的提问)
        String currentInput = hasHistory ? request.getCode() : buildPrompt(request);

        // 添加当前用户输入到上下文
        messages.add(Map.of("role", "user", "content", currentInput));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.3
        );

        String response = restClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        AlgorithmComplexityAnalysis analysis = parseFreeFormResponse(response);

        // 如果有 sessionId，保存对话到数据库
        if (request.getSessionId() != null) {
            sessionRepository.findById(request.getSessionId()).ifPresent(session -> {
                // 保存用户提问
                ChatMessage userMsg = new ChatMessage();
                userMsg.setSession(session);
                userMsg.setRole("user");
                userMsg.setContent(currentInput);
                messageRepository.save(userMsg);

                // 保存 AI 回答
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setSession(session);
                aiMsg.setRole("assistant");
                aiMsg.setContent(analysis.getRawText());
                messageRepository.save(aiMsg);
            });
        }

        return analysis;
    }

    private String buildPrompt(AlgorithmComplexityRequest request) {
        return """
                用户提供了如下内容（可为伪代码/脚本/问题），请直接以对话形式作答：
                
                - 语言类型：%s
                - 关注场景：%s
                - 内容：
                %s

                要求：
                - 可进行复杂度分析、代码解释、优化建议、示例演示或逐步推理。
                - 直接输出自然语言答案；如需代码或公式可正常使用代码块。
                """.formatted(
                safe(request.getLanguage()),
                safe(request.getCaseType()),
                request.getCode()
        );
    }

    // 新的解析：直接拿 content 文本，填充到 rawText，结构化字段留空或默认
    private AlgorithmComplexityAnalysis parseFreeFormResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            return AlgorithmComplexityAnalysis.builder()
                    .timeComplexityWorst("")
                    .timeComplexityAverage("")
                    .timeComplexityBest("")
                    .spaceComplexity("")
                    .reasoningSteps(List.of())
                    .assumptions(List.of())
                    .optimizationSuggestions(List.of())
                    .confidence(0.0)
                    .rawText(content) // 直接把对话答案返回给前端
                    .build();
        } catch (Exception e) {
            return AlgorithmComplexityAnalysis.builder()
                    .timeComplexityWorst("")
                    .timeComplexityAverage("")
                    .timeComplexityBest("")
                    .spaceComplexity("")
                    .reasoningSteps(List.of())
                    .assumptions(List.of())
                    .optimizationSuggestions(List.of())
                    .confidence(0.0)
                    .rawText("AI 返回结果解析失败：" + e.getMessage())
                    .build();
        }
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }

        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未指定" : value;
    }
}
