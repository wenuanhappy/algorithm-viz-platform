package com.algorithmviz.model;

import java.util.List;
import java.util.Map;

public class GraphStep {
    private Map<String, String> nodeStates;
    private Map<String, String> edgeStates;
    private Map<String, Double> distances;
    private List<String> queue;
    private List<String> stack;
    private String current;
    private List<String> path;
    private String description;
    private int codeLine;
    private int visitedCount;
    private double pathLength;
    private int comparisons;
    private Double mstCost;
    private String phase;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final GraphStep s = new GraphStep();
        public Builder nodeStates(Map<String, String> v) { s.nodeStates = v; return this; }
        public Builder edgeStates(Map<String, String> v) { s.edgeStates = v; return this; }
        public Builder distances(Map<String, Double> v) { s.distances = v; return this; }
        public Builder queue(List<String> v) { s.queue = v; return this; }
        public Builder stack(List<String> v) { s.stack = v; return this; }
        public Builder current(String v) { s.current = v; return this; }
        public Builder path(List<String> v) { s.path = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder codeLine(int v) { s.codeLine = v; return this; }
        public Builder visitedCount(int v) { s.visitedCount = v; return this; }
        public Builder pathLength(double v) { s.pathLength = v; return this; }
        public Builder comparisons(int v) { s.comparisons = v; return this; }
        public Builder mstCost(Double v) { s.mstCost = v; return this; }
        public Builder phase(String v) { s.phase = v; return this; }
        public GraphStep build() { return s; }
    }

    public Map<String, String> getNodeStates() { return nodeStates; }
    public Map<String, String> getEdgeStates() { return edgeStates; }
    public Map<String, Double> getDistances() { return distances; }
    public List<String> getQueue() { return queue; }
    public List<String> getStack() { return stack; }
    public String getCurrent() { return current; }
    public List<String> getPath() { return path; }
    public String getDescription() { return description; }
    public int getCodeLine() { return codeLine; }
    public int getVisitedCount() { return visitedCount; }
    public double getPathLength() { return pathLength; }
    public int getComparisons() { return comparisons; }
    public Double getMstCost() { return mstCost; }
    public String getPhase() { return phase; }
}

