package com.algorithmviz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SearchRequest {

    @NotNull(message = "算法类型不能为空")
    private String algorithm;

    @NotEmpty(message = "数组不能为空")
    private List<Integer> array;

    @NotNull(message = "目标值不能为空")
    private Integer target;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public List<Integer> getArray() { return array; }
    public void setArray(List<Integer> array) { this.array = array; }
    public Integer getTarget() { return target; }
    public void setTarget(Integer target) { this.target = target; }
}

