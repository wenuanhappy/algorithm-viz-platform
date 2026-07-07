package com.algorithmviz.dto;

import jakarta.validation.constraints.NotBlank;

public class DivideConquerRequest {
    @NotBlank
    private String algorithm;

    @NotBlank
    private String x;

    @NotBlank
    private String y;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}