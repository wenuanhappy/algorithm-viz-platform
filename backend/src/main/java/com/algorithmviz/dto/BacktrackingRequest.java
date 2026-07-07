package com.algorithmviz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BacktrackingRequest {

    @NotNull(message = "算法类型不能为空")
    private String algorithm;

    @Min(value = 1, message = "N 至少为 1")
    @Max(value = 12, message = "N 最大为 12")
    private int n;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public int getN() { return n; }
    public void setN(int n) { this.n = n; }
}

