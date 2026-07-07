package com.algorithmviz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssessmentConfigRequest {

    @Min(1)
    @Max(15)
    private int questionCount = 5;

    private List<String> categories = new ArrayList<>();

    private List<String> algorithms = new ArrayList<>();

    private String difficulty = "medium";

    private String mode = "ai";

    private List<String> questionTypes = new ArrayList<>();
}
