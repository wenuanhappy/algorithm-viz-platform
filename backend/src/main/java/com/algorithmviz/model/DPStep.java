package com.algorithmviz.model;

import java.util.List;

public class DPStep {
    private List<List<Integer>> dp;
    private int currentItem;
    private int currentWeight;
    private String decision;
    private String description;
    private int codeLine;
    private int totalValue;
    private List<Integer> selectedItems;
    private List<int[]> tracePath;
    private int comparisons;
    private String phase;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DPStep s = new DPStep();
        public Builder dp(List<List<Integer>> v) { s.dp = v; return this; }
        public Builder currentItem(int v) { s.currentItem = v; return this; }
        public Builder currentWeight(int v) { s.currentWeight = v; return this; }
        public Builder decision(String v) { s.decision = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder codeLine(int v) { s.codeLine = v; return this; }
        public Builder totalValue(int v) { s.totalValue = v; return this; }
        public Builder selectedItems(List<Integer> v) { s.selectedItems = v; return this; }
        public Builder tracePath(List<int[]> v) { s.tracePath = v; return this; }
        public Builder comparisons(int v) { s.comparisons = v; return this; }
        public Builder phase(String v) { s.phase = v; return this; }
        public DPStep build() { return s; }
    }

    public List<List<Integer>> getDp() { return dp; }
    public int getCurrentItem() { return currentItem; }
    public int getCurrentWeight() { return currentWeight; }
    public String getDecision() { return decision; }
    public String getDescription() { return description; }
    public int getCodeLine() { return codeLine; }
    public int getTotalValue() { return totalValue; }
    public List<Integer> getSelectedItems() { return selectedItems; }
    public List<int[]> getTracePath() { return tracePath; }
    public int getComparisons() { return comparisons; }
    public String getPhase() { return phase; }
}

