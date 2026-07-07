package com.algorithmviz.model;

import java.util.List;

public class SortStep {
    private List<Integer> array;
    private List<Integer> comparing;
    private List<Integer> swapping;
    private List<Integer> sorted;
    private Integer pivot;
    private int rangeLeft;
    private int rangeRight;
    private String description;
    private int codeLine;
    private int comparisons;
    private int swaps;
    private int accesses;
    private List<Integer> mergeLeft;
    private List<Integer> mergeRight;
    private Integer mergeTarget;
    private String phase;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SortStep s = new SortStep();
        public Builder array(List<Integer> v) { s.array = v; return this; }
        public Builder comparing(List<Integer> v) { s.comparing = v; return this; }
        public Builder swapping(List<Integer> v) { s.swapping = v; return this; }
        public Builder sorted(List<Integer> v) { s.sorted = v; return this; }
        public Builder pivot(Integer v) { s.pivot = v; return this; }
        public Builder rangeLeft(int v) { s.rangeLeft = v; return this; }
        public Builder rangeRight(int v) { s.rangeRight = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder codeLine(int v) { s.codeLine = v; return this; }
        public Builder comparisons(int v) { s.comparisons = v; return this; }
        public Builder swaps(int v) { s.swaps = v; return this; }
        public Builder accesses(int v) { s.accesses = v; return this; }
        public Builder mergeLeft(List<Integer> v) { s.mergeLeft = v; return this; }
        public Builder mergeRight(List<Integer> v) { s.mergeRight = v; return this; }
        public Builder mergeTarget(Integer v) { s.mergeTarget = v; return this; }
        public Builder phase(String v) { s.phase = v; return this; }
        public SortStep build() { return s; }
    }

    public List<Integer> getArray() { return array; }
    public List<Integer> getComparing() { return comparing; }
    public List<Integer> getSwapping() { return swapping; }
    public List<Integer> getSorted() { return sorted; }
    public Integer getPivot() { return pivot; }
    public int getRangeLeft() { return rangeLeft; }
    public int getRangeRight() { return rangeRight; }
    public String getDescription() { return description; }
    public int getCodeLine() { return codeLine; }
    public int getComparisons() { return comparisons; }
    public int getSwaps() { return swaps; }
    public int getAccesses() { return accesses; }
    public List<Integer> getMergeLeft() { return mergeLeft; }
    public List<Integer> getMergeRight() { return mergeRight; }
    public Integer getMergeTarget() { return mergeTarget; }
    public String getPhase() { return phase; }
}

