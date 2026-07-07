package com.algorithmviz.controller;

import com.algorithmviz.dto.*;
import com.algorithmviz.entity.RunHistory;
import com.algorithmviz.model.*;
import com.algorithmviz.repository.RunHistoryRepository;
import com.algorithmviz.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    private final SortingService sortingService;
    private final SearchService searchService;
    private final GraphService graphService;
    private final DPService dpService;
    private final BacktrackingService backtrackingService;
    private final DivideConquerService divideConquerService;
    private final AlgorithmComplexityService algorithmComplexityService;
    private final AssessmentService assessmentService;
    private final RunHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public AlgorithmController(SortingService sortingService, SearchService searchService,
            GraphService graphService, DPService dpService, BacktrackingService backtrackingService,
            DivideConquerService divideConquerService,
            AlgorithmComplexityService algorithmComplexityService,
            AssessmentService assessmentService,
            RunHistoryRepository historyRepository, ObjectMapper objectMapper) {
        this.sortingService = sortingService;
        this.searchService = searchService;
        this.graphService = graphService;
        this.dpService = dpService;
        this.backtrackingService = backtrackingService;
        this.divideConquerService = divideConquerService;
        this.algorithmComplexityService = algorithmComplexityService;
        this.assessmentService = assessmentService;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== SORTING ====================
    @PostMapping("/sort")
    public ResponseEntity<Map<String, Object>> sort(@Valid @RequestBody SortRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<SortStep> steps = sortingService.generateSteps(req.getAlgorithm(), req.getArray());
        long elapsed = System.currentTimeMillis() - start;

        SortStep last = steps.get(steps.size() - 1);
        saveHistory("sorting", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), last.getComparisons(), last.getSwaps(), elapsed);

        return ok(steps, steps.size(), last.getComparisons(), last.getSwaps(), elapsed);
    }

    // ==================== SEARCH ====================
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@Valid @RequestBody SearchRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<SearchStep> steps = searchService.generateSteps(req.getAlgorithm(), req.getArray(), req.getTarget());
        long elapsed = System.currentTimeMillis() - start;

        SearchStep last = steps.get(steps.size() - 1);
        saveHistory("search", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), last.getComparisons(), 0, elapsed);

        return ok(steps, steps.size(), last.getComparisons(), 0, elapsed);
    }

    // ==================== GRAPH ====================
    @PostMapping("/graph")
    public ResponseEntity<Map<String, Object>> graph(@Valid @RequestBody GraphRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<GraphStep> steps = graphService.generateSteps(
                req.getAlgorithm(), req.getGraph(), req.getStartId(), req.getEndId());
        long elapsed = System.currentTimeMillis() - start;

        GraphStep last = steps.get(steps.size() - 1);
        saveHistory("graph", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), last.getComparisons(), 0, elapsed);

        return ok(steps, steps.size(), last.getComparisons(), 0, elapsed);
    }

    // ==================== DYNAMIC PROGRAMMING ====================
    @PostMapping("/dp")
    public ResponseEntity<Map<String, Object>> dp(@Valid @RequestBody DPRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<DPStep> steps = dpService.generateSteps(req.getAlgorithm(), req.getItems(), req.getCapacity());
        long elapsed = System.currentTimeMillis() - start;

        DPStep last = steps.get(steps.size() - 1);
        saveHistory("dp", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), last.getComparisons(), 0, elapsed);

        return ok(steps, steps.size(), last.getComparisons(), 0, elapsed);
    }

    // ==================== BACKTRACKING ====================
    @PostMapping("/backtracking")
    public ResponseEntity<Map<String, Object>> backtracking(@Valid @RequestBody BacktrackingRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<NQueensStep> steps = backtrackingService.generateSteps(req.getAlgorithm(), req.getN());
        long elapsed = System.currentTimeMillis() - start;

        NQueensStep last = steps.get(steps.size() - 1);
        saveHistory("backtracking", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), 0, last.getBacktracks(), elapsed);

        return ok(steps, steps.size(), 0, last.getBacktracks(), elapsed);
    }

    // ==================== DIVIDE & CONQUER ====================
    @PostMapping("/divide-conquer")
    public ResponseEntity<Map<String, Object>> divideConquer(@Valid @RequestBody DivideConquerRequest req) throws Exception {
        long start = System.currentTimeMillis();
        List<DivideConquerStep> steps = divideConquerService.generateSteps(req.getAlgorithm(), req.getX(), req.getY());
        long elapsed = System.currentTimeMillis() - start;

        DivideConquerStep last = steps.get(steps.size() - 1);
        saveHistory("divide-conquer", req.getAlgorithm(), objectMapper.writeValueAsString(req),
                steps.size(), last.getMultiplications(), last.getAdditions(), elapsed);

        return ok(steps, steps.size(), last.getMultiplications(), last.getAdditions(), elapsed);
    }

    // ==================== AI CUSTOM COMPLEXITY ====================
    @PostMapping("/algorithm-complexity")
    public ResponseEntity<AlgorithmComplexityAnalysis> analyzeAlgorithmComplexity(
            @Valid @RequestBody AlgorithmComplexityRequest req) {
        return ResponseEntity.ok(algorithmComplexityService.analyze(req));
    }

    // ==================== HISTORY ====================
    @GetMapping("/history")
    public ResponseEntity<List<RunHistory>> getHistory(
            @RequestParam(required = false) String category) {
        List<RunHistory> history = (category != null && !category.isBlank())
                ? historyRepository.findByCategoryOrderByCreatedAtDesc(category)
                : historyRepository.findTop20ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        historyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== VERIFY STEP ====================
    @PostMapping("/verify-step")
    public ResponseEntity<Map<String, Object>> verifyStep(@RequestBody Map<String, Object> req) throws Exception {
        String algorithm = (String) req.get("algorithm");
        int targetStepIndex = req.get("targetStepIndex") != null ? (int) req.get("targetStepIndex") : 0;

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) req.get("params");

        if (algorithm == null || params == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "algorithm and params are required"));
        }

        Object stepData = null;
        String category = null;

        if (List.of("quick-sort", "merge-sort", "bubble-sort", "heap-sort", "insertion-sort").contains(algorithm)) {
            @SuppressWarnings("unchecked")
            List<Integer> array = ((List<Number>) params.get("array")).stream()
                    .map(Number::intValue).collect(java.util.stream.Collectors.toList());
            List<SortStep> steps = sortingService.generateSteps(algorithm, array);
            category = "sorting";
            if (targetStepIndex >= 0 && targetStepIndex < steps.size()) {
                stepData = steps.get(targetStepIndex);
            }
        } else if ("binary-search".equals(algorithm)) {
            @SuppressWarnings("unchecked")
            List<Integer> array = ((List<Number>) params.get("array")).stream()
                    .map(Number::intValue).collect(java.util.stream.Collectors.toList());
            int target = ((Number) params.get("target")).intValue();
            List<SearchStep> steps = searchService.generateSteps(algorithm, array, target);
            category = "search";
            if (targetStepIndex >= 0 && targetStepIndex < steps.size()) {
                stepData = steps.get(targetStepIndex);
            }
        } else if (List.of("dijkstra", "bfs", "dfs", "prim", "kruskal", "astar").contains(algorithm)) {
            // For graph, we need to reconstruct GraphData from params
            category = "graph";
            stepData = Map.of("message", "Graph verification requires running algorithm in visualizer");
        } else if ("knapsack".equals(algorithm)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) params.get("items");
            int capacity = ((Number) params.get("capacity")).intValue();
            List<com.algorithmviz.dto.DPRequest.KnapsackItemDto> items = itemsRaw.stream().map(m -> {
                com.algorithmviz.dto.DPRequest.KnapsackItemDto dto = new com.algorithmviz.dto.DPRequest.KnapsackItemDto();
                dto.setName((String) m.get("name"));
                dto.setWeight(((Number) m.get("weight")).intValue());
                dto.setValue(((Number) m.get("value")).intValue());
                return dto;
            }).collect(java.util.stream.Collectors.toList());
            List<DPStep> steps = dpService.generateSteps(algorithm, items, capacity);
            category = "dp";
            if (targetStepIndex >= 0 && targetStepIndex < steps.size()) {
                stepData = steps.get(targetStepIndex);
            }
        } else if ("n-queens".equals(algorithm)) {
            int n = ((Number) params.get("n")).intValue();
            List<NQueensStep> steps = backtrackingService.generateSteps(algorithm, n);
            category = "backtracking";
            if (targetStepIndex >= 0 && targetStepIndex < steps.size()) {
                stepData = steps.get(targetStepIndex);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("algorithm", algorithm);
        result.put("category", category);
        result.put("targetStepIndex", targetStepIndex);
        result.put("stepData", stepData);
        return ResponseEntity.ok(result);
    }

    // ==================== ASSESSMENT (LLM-ENHANCED) ====================
    @PostMapping("/assessment/generate")
    public ResponseEntity<?> generateAssessment(@RequestBody AssessmentConfigRequest config) {
        try {
            List<AssessmentQuestion> questions = assessmentService.generateQuestions(config);
            return ResponseEntity.ok(questions);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "题目生成失败：" + e.getMessage()));
        }
    }

    @PostMapping("/assessment/evaluate")
    public ResponseEntity<?> evaluateAnswer(@Valid @RequestBody AnswerEvaluationRequest request) {
        try {
            AnswerEvaluationResponse result = assessmentService.evaluateAnswer(
                    request.getQuestion(), request.getUserAnswer());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "评测失败：" + e.getMessage()));
        }
    }

    // ==================== ASSESSMENT HEALTH ====================
    @GetMapping("/assessment/health")
    public ResponseEntity<Map<String, Object>> assessmentHealth() {
        boolean configured = assessmentService.isApiKeyConfigured();
        Map<String, Object> result = new HashMap<>();
        result.put("aiAvailable", configured);
        result.put("mode", configured ? "ai + fixed" : "fixed only");
        result.put("message", configured
                ? "AI assessment is ready"
                : "AI API key not configured — only fixed-question mode is available");
        return ResponseEntity.ok(result);
    }

    // ==================== HEALTH ====================
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Algorithm Viz Backend"));
    }

    // ==================== HELPERS ====================
    private void saveHistory(String category, String algorithm, String inputData,
                              int stepCount, int comparisons, int swaps, long elapsedMs) {
        RunHistory h = new RunHistory();
        h.setCategory(category);
        h.setAlgorithm(algorithm);
        h.setInputData(inputData);
        h.setStepCount(stepCount);
        h.setComparisons(comparisons);
        h.setSwaps(swaps);
        h.setExecutionTimeMs(elapsedMs);
        historyRepository.save(h);
    }

    private ResponseEntity<Map<String, Object>> ok(Object steps, int stepCount,
                                                    int comparisons, int extra, long elapsed) {
        Map<String, Object> body = new HashMap<>();
        body.put("steps", steps);
        body.put("stepCount", stepCount);
        body.put("comparisons", comparisons);
        body.put("extra", extra);
        body.put("executionTimeMs", elapsed);
        return ResponseEntity.ok(body);
    }
}
