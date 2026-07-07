package com.algorithmviz.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlgorithmComplexityAnalysis {
    private String timeComplexityWorst;
    private String timeComplexityAverage;
    private String timeComplexityBest;
    private String spaceComplexity;
    private List<String> reasoningSteps;
    private List<String> assumptions;
    private List<String> optimizationSuggestions;
    private double confidence;
    private String rawText;
}