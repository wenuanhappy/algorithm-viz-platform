package com.algorithmviz.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerEvaluationResponse {
    private boolean correct;
    private String feedback;
    private String correctAnswer;
    private double confidence;
}
