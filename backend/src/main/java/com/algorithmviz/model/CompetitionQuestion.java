package com.algorithmviz.model;

import java.util.List;
import java.util.Map;

public class CompetitionQuestion {
    private String id;
    private String algorithm;
    private String title;
    private String prompt;
    private List<String> options;
    private Map<String, Object> input;
    private int points;

    public CompetitionQuestion() {}

    public CompetitionQuestion(String id, String algorithm, String title, String prompt,
                               List<String> options, Map<String, Object> input, int points) {
        this.id = id;
        this.algorithm = algorithm;
        this.title = title;
        this.prompt = prompt;
        this.options = options;
        this.input = input;
        this.points = points;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
