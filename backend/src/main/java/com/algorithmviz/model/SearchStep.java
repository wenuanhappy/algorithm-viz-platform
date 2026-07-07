package com.algorithmviz.model;

import java.util.List;

public class SearchStep {
    private List<Integer> array;
    private int left;
    private int right;
    private int mid;
    private int target;
    private Boolean found;
    private String eliminated;
    private String description;
    private int codeLine;
    private int comparisons;
    private String phase;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SearchStep s = new SearchStep();
        public Builder array(List<Integer> v) { s.array = v; return this; }
        public Builder left(int v) { s.left = v; return this; }
        public Builder right(int v) { s.right = v; return this; }
        public Builder mid(int v) { s.mid = v; return this; }
        public Builder target(int v) { s.target = v; return this; }
        public Builder found(Boolean v) { s.found = v; return this; }
        public Builder eliminated(String v) { s.eliminated = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder codeLine(int v) { s.codeLine = v; return this; }
        public Builder comparisons(int v) { s.comparisons = v; return this; }
        public Builder phase(String v) { s.phase = v; return this; }
        public SearchStep build() { return s; }
    }

    public List<Integer> getArray() { return array; }
    public int getLeft() { return left; }
    public int getRight() { return right; }
    public int getMid() { return mid; }
    public int getTarget() { return target; }
    public Boolean getFound() { return found; }
    public String getEliminated() { return eliminated; }
    public String getDescription() { return description; }
    public int getCodeLine() { return codeLine; }
    public int getComparisons() { return comparisons; }
    public String getPhase() { return phase; }
}

