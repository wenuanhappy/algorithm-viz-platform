package com.algorithmviz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class DPRequest {

    @NotNull(message = "算法类型不能为空")
    private String algorithm;

    @NotEmpty(message = "物品列表不能为空")
    private List<KnapsackItemDto> items;

    @Positive(message = "背包容量必须大于0")
    private int capacity;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public List<KnapsackItemDto> getItems() { return items; }
    public void setItems(List<KnapsackItemDto> items) { this.items = items; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public static class KnapsackItemDto {
        private String name;
        private int weight;
        private int value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}

