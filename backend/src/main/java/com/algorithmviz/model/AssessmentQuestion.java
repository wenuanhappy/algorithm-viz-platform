package com.algorithmviz.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AssessmentQuestion {
    private int id;
    private String title;
    private String category;
    private String algorithm;
    private String questionType;
    private String description;
    private Map<String, Object> inputParams;
    private Object answer;
    private List<String> options;
    private String explanation;
    private Integer targetStepIndex;
    private String verifyField;
    private boolean validated;
}
