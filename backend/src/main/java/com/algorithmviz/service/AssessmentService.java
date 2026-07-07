package com.algorithmviz.service;

import com.algorithmviz.dto.AssessmentConfigRequest;
import com.algorithmviz.dto.AnswerEvaluationRequest;
import com.algorithmviz.dto.DPRequest;
import com.algorithmviz.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SortingService sortingService;
    private final SearchService searchService;
    private final GraphService graphService;
    private final DPService dpService;
    private final BacktrackingService backtrackingService;

    @Value("${ai.assessment.api-url}")
    private String apiUrl;

    @Value("${ai.assessment.api-key}")
    private String apiKey;

    @Value("${ai.assessment.model}")
    private String model;

    private static final Set<String> SORT_ALGOS = Set.of(
            "quick-sort", "merge-sort", "bubble-sort", "heap-sort", "insertion-sort");
    private static final Set<String> GRAPH_ALGOS = Set.of(
            "dijkstra", "bfs", "dfs", "prim", "kruskal", "astar");

    public AssessmentService(ObjectMapper objectMapper, SortingService sortingService,
            SearchService searchService, GraphService graphService, DPService dpService,
            BacktrackingService backtrackingService) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
        this.sortingService = sortingService;
        this.searchService = searchService;
        this.graphService = graphService;
        this.dpService = dpService;
        this.backtrackingService = backtrackingService;
    }

    // ==================== PUBLIC API ====================

    /** Returns true if the API key is configured and AI assessment is available. */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<AssessmentQuestion> generateQuestions(AssessmentConfigRequest config) {
        ensureApiKey();

        String prompt = buildGenerationPrompt(config);
        String response = callLLM(buildGenerationSystemPrompt(), prompt);
        List<AssessmentQuestion> questions = parseQuestions(response);

        if (questions.isEmpty()) {
            throw new IllegalStateException("LLM 未能生成有效题目，请重试");
        }

        questions = crossValidate(questions);
        return questions;
    }

    public AnswerEvaluationResponse evaluateAnswer(AssessmentQuestion question, String userAnswer) {
        ensureApiKey();

        String prompt = buildEvaluationPrompt(question, userAnswer);
        String response = callLLM(buildEvaluationSystemPrompt(), prompt);
        return parseEvaluation(response);
    }

    // ==================== LLM CALL ====================

    private void ensureApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI 评估服务未配置 API Key，请使用固定题目模式");
        }
    }

    private String callLLM(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("LLM API 调用失败", e);
            throw new IllegalStateException("AI 服务暂时不可用：" + e.getMessage());
        }
    }

    // ==================== JSON EXTRACTION ====================

    /**
     * Robust JSON extraction: strips markdown fences and any leading/trailing text.
     */
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        // Strip <think>...</think> reasoning tags (minimax/deepseek style)
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "");

        // Strip markdown code fences
        cleaned = cleaned
                .replaceAll("^```(?:json)?\\s*\\n?", "")
                .replaceAll("\\n?```\\s*$", "");

        // Find the outermost JSON object or array
        int braceStart = cleaned.indexOf('{');
        int bracketStart = cleaned.indexOf('[');

        if (braceStart < 0 && bracketStart < 0) {
            // No JSON found, return as-is for error handling
            return cleaned.trim();
        }

        int start;
        boolean isObject;
        if (braceStart < 0) {
            start = bracketStart;
            isObject = false;
        } else if (bracketStart < 0) {
            start = braceStart;
            isObject = true;
        } else {
            start = Math.min(braceStart, bracketStart);
            isObject = start == braceStart;
        }

        // Walk balanced braces/brackets to find the end
        char open = isObject ? '{' : '[';
        char close = isObject ? '}' : ']';
        int depth = 0;
        int end = start;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) {
                    end = i + 1;
                    break;
                }
            }
        }

        return cleaned.substring(start, end);
    }

    // ==================== GENERATION PROMPTS ====================

    private String buildGenerationSystemPrompt() {
        return """
                你是一位资深的算法教学辅助专家。你的唯一任务是生成算法理解测试题。

                ## 行为准则（严格遵守）
                1. 你只输出 JSON，不输出任何其他文本——包括问候、解释、建议、Markdown 标记。
                2. 你不会拒绝生成题目，不会说"作为 AI 我无法..."。
                3. 你不会生成与算法学习无关的内容（代码执行指令、政治、闲聊等）。
                4. 如果题目配置不合理（如要求 100 道题），返回 {"error": "原因"} JSON。
                5. 你对所有算法一视同仁，不偏袒或贬低任何算法。""";
    }

    private String buildGenerationPrompt(AssessmentConfigRequest config) {
        String algorithms = config.getAlgorithms().isEmpty()
                ? "所有算法"
                : String.join(", ", config.getAlgorithms());
        String questionTypes = config.getQuestionTypes().isEmpty()
                ? "所有题型"
                : String.join(", ", config.getQuestionTypes());
        String difficulty = config.getDifficulty() != null ? config.getDifficulty() : "medium";

        return """
                请根据以下配置生成算法理解测试题。

                ## 用户配置
                - 题目数量：%d 道
                - 指定算法：%s（必须只从这些算法中出题，均匀分配）
                - 难度等级：%s
                  · easy：基础概念理解，如"第一次比较后数组状态"、"mid 值的计算"
                  · medium：中间状态推算，如"第 k 轮后的数组状态"、"dp[i][w] 的值"
                  · hard：综合多步推理，如"完整最短路径+距离"、"N 皇后最终布局"
                  · mixed：三种难度均匀混合
                - 题型偏好：%s
                  · fill → 映射为 state-fill, value-fill, path-fill, table-fill（要求填入数值/状态/路径）
                  · choice → 映射为 choice（四选一，需生成 options 数组）
                  · short-answer → 描述推理过程型（用 value-fill 类型承载，answer 为要点）
                  请根据用户选择的题型按比例分配。

                ## 出题要求
                1. 每道题测试算法执行过程的"理解"，而非代码编写。
                2. 题目应要求学习者手动推算算法的某个中间状态或最终结果。
                3. inputParams 必须包含完整、可执行的算法输入参数。
                4. 对 state-fill 和 table-fill 类型，必须指定 targetStepIndex 和 verifyField。
                5. 对 choice 类型，必须生成包含 4 个 options 的数组。
                6. explanation 要详细，逐步解释推理过程。
                7. 不同题目使用不同的输入数据，增强多样性。

                ## 算法及可用输入/输出格式

                ### sorting (quick-sort/merge-sort/bubble-sort/heap-sort/insertion-sort)
                输入: array (int[], 长度 5-8)
                可用: state-fill(取某步array状态), choice, short-answer

                ### search (binary-search)
                输入: array (有序 int[]), target (int)
                可用: value-fill(比较次数/mid序列), choice, short-answer

                ### graph (dijkstra/bfs/dfs/prim/kruskal/astar)
                输入: graph ({nodes,edges,directed,weighted}), startId, endId
                可用: path-fill(最短路径+距离), choice, short-answer

                ### dp (knapsack)
                输入: items ([{name,weight,value}]), capacity (int 7-10)
                可用: table-fill(dp[i][w]), choice, short-answer

                ### backtracking (n-queens)
                输入: n (int 4-8)
                可用: state-fill(某行皇后列位置), choice, short-answer

                ## 输出格式
                只输出一个 JSON 对象，包含 questions 数组。不要 Markdown 代码块，不要任何前导或尾随文字，不要 <think> 标签：
                {
                  "questions": [
                    {
                      "id": 1,
                      "title": "...",
                      "category": "sorting|search|graph|dp|backtracking",
                      "algorithm": "...",
                      "questionType": "value-fill|state-fill|path-fill|table-fill|choice",
                      "description": "...",
                      "inputParams": { ... },
                      "answer": { ... },
                      "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
                      "explanation": "...",
                      "targetStepIndex": 10,
                      "verifyField": "array|dp|board|..."
                    }
                  ]
                }
                """.formatted(
                config.getQuestionCount(),
                algorithms,
                difficulty,
                questionTypes);
    }

    // ==================== EVALUATION PROMPTS ====================

    private String buildEvaluationSystemPrompt() {
        return """
                你是一位严格但公正的算法教学评测专家。你的唯一任务是评判学生对算法题的回答。

                ## 行为准则（严格遵守）
                1. 你只输出 JSON，不输出任何其他文本——包括问候、解释、Markdown 标记。
                2. 你不会拒绝评测，不会说"作为 AI 我无法判断"。
                3. 你不会生成与评测无关的内容。
                4. 你对所有学生一视同仁，评分标准一致。""";
    }

    private String buildEvaluationPrompt(AssessmentQuestion question, String userAnswer) {
        try {
            String questionJson = objectMapper.writeValueAsString(question);
            return """
                    请评判学生对以下算法题的回答。

                    ## 题目信息
                    %s

                    ## 学生回答
                    \"\"\"
                    %s
                    \"\"\"

                    ## 评测要求
                    1. 语义等价判断：回答在语义上是否与标准答案一致？允许格式差异和同等含义的不同表述。
                    2. 选择题：学生给出选项字母或完整文本，只要匹配正确选项即可。
                    3. 简答题：不要求逐字匹配，判断是否涵盖核心推理要点。
                    4. 反馈要具体：指出哪里对了、哪里错了、为什么。
                    5. 评分只有正确(true)或错误(false)，不考虑部分正确。

                    ## 输出格式
                    只输出 JSON，不要 Markdown 代码块包裹：
                    {
                      "correct": true/false,
                      "feedback": "详细的评价文字...",
                      "correctAnswer": "标准答案的友好展示形式",
                      "confidence": 0.0-1.0
                    }
                    """.formatted(questionJson, userAnswer);
        } catch (Exception e) {
            throw new IllegalStateException("构建评测请求失败：" + e.getMessage());
        }
    }

    // ==================== RESPONSE PARSING ====================

    @SuppressWarnings("unchecked")
    private List<AssessmentQuestion> parseQuestions(String raw) {
        String json = extractJson(raw);
        try {
            // Try parsing as JSON array
            if (json.startsWith("[")) {
                List<Map<String, Object>> list = objectMapper.readValue(
                        json, new TypeReference<List<Map<String, Object>>>() {});
                return list.stream()
                        .map(this::mapToQuestion)
                        .filter(q -> q.getTitle() != null && q.getAnswer() != null)
                        .collect(Collectors.toList());
            }

            // Try parsing as JSON object
            Map<String, Object> map = objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {});

            // Check for "questions" wrapper (response_format json_object forces object output)
            if (map.containsKey("questions") && map.get("questions") instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("questions");
                return list.stream()
                        .map(this::mapToQuestion)
                        .filter(q -> q.getTitle() != null && q.getAnswer() != null)
                        .collect(Collectors.toList());
            }

            if (map.containsKey("error")) {
                log.warn("LLM returned error: {}", map.get("error"));
                return List.of();
            }
            // Single question object fallback
            AssessmentQuestion q = mapToQuestion(map);
            if (q.getTitle() != null && q.getAnswer() != null) {
                return List.of(q);
            }
            log.warn("LLM returned unrecognized JSON object: {}", json.substring(0, Math.min(200, json.length())));
            return List.of();
        } catch (Exception e) {
            log.error("解析题目 JSON 失败: {} — raw: {}", e.getMessage(),
                    json.substring(0, Math.min(300, json.length())));
            return List.of();
        }
    }

    private AssessmentQuestion mapToQuestion(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> inputParams = (Map<String, Object>) map.getOrDefault(
                "inputParams", Map.of());

        @SuppressWarnings("unchecked")
        List<String> options = map.containsKey("options")
                ? (List<String>) map.get("options")
                : null;

        Integer targetStepIndex = map.get("targetStepIndex") instanceof Number n
                ? n.intValue() : null;

        return AssessmentQuestion.builder()
                .id(map.get("id") instanceof Number n ? n.intValue() : 0)
                .title((String) map.get("title"))
                .category((String) map.get("category"))
                .algorithm((String) map.get("algorithm"))
                .questionType((String) map.get("questionType"))
                .description((String) map.get("description"))
                .inputParams(inputParams)
                .answer(map.get("answer"))
                .options(options)
                .explanation((String) map.get("explanation"))
                .targetStepIndex(targetStepIndex)
                .verifyField((String) map.get("verifyField"))
                .validated(false)
                .build();
    }

    private AnswerEvaluationResponse parseEvaluation(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            return AnswerEvaluationResponse.builder()
                    .correct(node.path("correct").asBoolean(false))
                    .feedback(node.path("feedback").asText("评测解析失败"))
                    .correctAnswer(node.path("correctAnswer").asText(""))
                    .confidence(node.path("confidence").asDouble(0.5))
                    .build();
        } catch (Exception e) {
            log.error("解析评测 JSON 失败: {}", e.getMessage());
            return AnswerEvaluationResponse.builder()
                    .correct(false)
                    .feedback("评测系统暂时无法处理该答案，请手动比对或重试。")
                    .correctAnswer("")
                    .confidence(0.0)
                    .build();
        }
    }

    // ==================== CROSS-VALIDATION ====================

    private List<AssessmentQuestion> crossValidate(List<AssessmentQuestion> questions) {
        return questions.stream().map(q -> {
            if (!"state-fill".equals(q.getQuestionType())
                    && !"table-fill".equals(q.getQuestionType())) {
                return q; // value-fill/path-fill/choice — skip validation
            }
            try {
                AssessmentQuestion validated = doCrossValidate(q);
                return validated != null ? validated : q;
            } catch (Exception e) {
                log.warn("题目 {} (id={}) 交叉验证失败: {}", q.getTitle(), q.getId(), e.getMessage());
                return q; // Return original on validation failure
            }
        }).collect(Collectors.toList());
    }

    private AssessmentQuestion doCrossValidate(AssessmentQuestion question) {
        String algo = question.getAlgorithm();
        Map<String, Object> params = question.getInputParams();
        Integer targetIdx = question.getTargetStepIndex();
        if (algo == null || params == null || targetIdx == null) return null;

        Object computedAnswer = null;

        if (SORT_ALGOS.contains(algo)) {
            @SuppressWarnings("unchecked")
            List<Integer> array = ((List<Number>) params.get("array")).stream()
                    .map(Number::intValue).collect(Collectors.toList());
            List<SortStep> steps = sortingService.generateSteps(algo, array);
            if (targetIdx >= 0 && targetIdx < steps.size()) {
                computedAnswer = extractSortField(steps.get(targetIdx), question.getVerifyField());
            }
        } else if ("binary-search".equals(algo)) {
            @SuppressWarnings("unchecked")
            List<Integer> array = ((List<Number>) params.get("array")).stream()
                    .map(Number::intValue).collect(Collectors.toList());
            int target = ((Number) params.get("target")).intValue();
            List<SearchStep> steps = searchService.generateSteps(algo, array, target);
            if (targetIdx >= 0 && targetIdx < steps.size()) {
                computedAnswer = extractSearchField(steps.get(targetIdx), question.getVerifyField());
            }
        } else if ("knapsack".equals(algo)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) params.get("items");
            int capacity = ((Number) params.get("capacity")).intValue();
            List<DPRequest.KnapsackItemDto> items = itemsRaw.stream().map(m -> {
                DPRequest.KnapsackItemDto dto = new DPRequest.KnapsackItemDto();
                dto.setName((String) m.get("name"));
                dto.setWeight(((Number) m.get("weight")).intValue());
                dto.setValue(((Number) m.get("value")).intValue());
                return dto;
            }).collect(Collectors.toList());
            List<DPStep> steps = dpService.generateSteps(algo, items, capacity);
            if (targetIdx >= 0 && targetIdx < steps.size()) {
                computedAnswer = extractDPField(steps.get(targetIdx), question.getVerifyField());
            }
        } else if ("n-queens".equals(algo)) {
            int n = ((Number) params.get("n")).intValue();
            List<NQueensStep> steps = backtrackingService.generateSteps(algo, n);
            if (targetIdx >= 0 && targetIdx < steps.size()) {
                computedAnswer = extractNQueensField(steps.get(targetIdx), question.getVerifyField());
            }
        }
        // Graph algorithms: skipped (complex structure reconstruction)

        if (computedAnswer == null) return null;

        // Compare and update
        boolean match = compareAnswers(question.getAnswer(), computedAnswer);
        if (!match) {
            log.info("题目 '{}' 交叉验证不一致，用算法执行结果替换 LLM 答案", question.getTitle());
            return AssessmentQuestion.builder()
                    .id(question.getId())
                    .title(question.getTitle())
                    .category(question.getCategory())
                    .algorithm(question.getAlgorithm())
                    .questionType(question.getQuestionType())
                    .description(question.getDescription())
                    .inputParams(question.getInputParams())
                    .answer(computedAnswer)
                    .options(question.getOptions())
                    .explanation(question.getExplanation())
                    .targetStepIndex(question.getTargetStepIndex())
                    .verifyField(question.getVerifyField())
                    .validated(true)
                    .build();
        }

        return AssessmentQuestion.builder()
                .id(question.getId())
                .title(question.getTitle())
                .category(question.getCategory())
                .algorithm(question.getAlgorithm())
                .questionType(question.getQuestionType())
                .description(question.getDescription())
                .inputParams(question.getInputParams())
                .answer(question.getAnswer())
                .options(question.getOptions())
                .explanation(question.getExplanation())
                .targetStepIndex(question.getTargetStepIndex())
                .verifyField(question.getVerifyField())
                .validated(true)
                .build();
    }

    private Object extractSortField(SortStep step, String field) {
        if (field == null) return step.getArray();
        return switch (field) {
            case "array" -> step.getArray();
            case "comparing" -> step.getComparing();
            case "swapping" -> step.getSwapping();
            case "sorted" -> step.getSorted();
            case "pivot" -> step.getPivot();
            default -> step.getArray();
        };
    }

    private Object extractSearchField(SearchStep step, String field) {
        if (field == null) return Map.of("mid", step.getMid(), "left", step.getLeft(), "right", step.getRight());
        return switch (field) {
            case "mid" -> step.getMid();
            case "left" -> step.getLeft();
            case "right" -> step.getRight();
            case "found" -> step.getFound();
            case "array" -> step.getArray();
            default -> Map.of("mid", step.getMid(), "left", step.getLeft(), "right", step.getRight());
        };
    }

    private Object extractDPField(DPStep step, String field) {
        if (field == null) return step.getDp();
        if ("dp".equals(field)) return step.getDp();
        if ("dpValue".equals(field) && step.getDp() != null) {
            List<List<Integer>> dp = step.getDp();
            int itemIdx = step.getCurrentItem();
            int weight = step.getCurrentWeight();
            if (itemIdx < dp.size()) {
                List<Integer> row = dp.get(itemIdx);
                if (weight < row.size()) {
                    return Map.of("dp[" + itemIdx + "][" + weight + "]", row.get(weight));
                }
            }
        }
        return step.getDp();
    }

    private Object extractNQueensField(NQueensStep step, String field) {
        if (field == null) return step.getBoard();
        return switch (field) {
            case "board" -> step.getBoard();
            case "currentRow" -> step.getCurrentRow();
            case "solutions" -> step.getSolutions();
            default -> step.getBoard();
        };
    }

    /**
     * Compare the LLM-generated answer with the computed answer from actual algorithm execution.
     */
    private boolean compareAnswers(Object llmAnswer, Object computedAnswer) {
        if (llmAnswer == null || computedAnswer == null) return false;
        try {
            // Normalize both to JSON strings and compare
            String llmJson = objectMapper.writeValueAsString(llmAnswer);
            String computedJson = objectMapper.writeValueAsString(computedAnswer);
            return llmJson.equals(computedJson);
        } catch (Exception e) {
            return false;
        }
    }
}
