package com.algorithmviz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GraphRequest {

    @NotNull(message = "算法类型不能为空")
    private String algorithm;

    @NotNull(message = "图数据不能为空")
    private GraphData graph;

    private String startId;
    private String endId;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public GraphData getGraph() { return graph; }
    public void setGraph(GraphData graph) { this.graph = graph; }
    public String getStartId() { return startId; }
    public void setStartId(String startId) { this.startId = startId; }
    public String getEndId() { return endId; }
    public void setEndId(String endId) { this.endId = endId; }

    public static class GraphData {
        private List<GraphNodeDto> nodes;
        private List<GraphEdgeDto> edges;
        private boolean directed;
        private boolean weighted;

        public List<GraphNodeDto> getNodes() { return nodes; }
        public void setNodes(List<GraphNodeDto> nodes) { this.nodes = nodes; }
        public List<GraphEdgeDto> getEdges() { return edges; }
        public void setEdges(List<GraphEdgeDto> edges) { this.edges = edges; }
        public boolean isDirected() { return directed; }
        public void setDirected(boolean directed) { this.directed = directed; }
        public boolean isWeighted() { return weighted; }
        public void setWeighted(boolean weighted) { this.weighted = weighted; }
    }

    public static class GraphNodeDto {
        private String id;
        private double x;
        private double y;
        private String label;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class GraphEdgeDto {
        private String from;
        private String to;
        private double weight;
        private boolean directed;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public boolean isDirected() { return directed; }
        public void setDirected(boolean directed) { this.directed = directed; }
    }
}

