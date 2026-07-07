package com.algorithmviz.dto;

import com.algorithmviz.model.AssessmentQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerEvaluationRequest {
    @NotNull(message = "题目信息不能为空")
    private AssessmentQuestion question;

    @NotBlank(message = "答案不能为空")
    private String userAnswer;
}
