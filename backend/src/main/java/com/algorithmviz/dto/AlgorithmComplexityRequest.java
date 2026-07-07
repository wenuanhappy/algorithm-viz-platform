package com.algorithmviz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlgorithmComplexityRequest {
    @NotBlank(message = "算法内容不能为空")
    @Size(max = 8000, message = "算法内容不能超过 8000 个字符")
    private String code;

    private String language = "pseudocode";

    private String caseType = "worst";

    private Long sessionId;
}
