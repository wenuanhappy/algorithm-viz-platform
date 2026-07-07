package com.algorithmviz.model;

public class CompetitionPlayer {
    private Long userId;
    private String displayName;
    private boolean ready;
    private int score;
    private int correctCount;
    private int submittedCount;
    private boolean finished;
    private boolean forfeited;
    private long totalTimeMs;

    public CompetitionPlayer() {}

    public CompetitionPlayer(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(int submittedCount) { this.submittedCount = submittedCount; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public boolean isForfeited() { return forfeited; }
    public void setForfeited(boolean forfeited) { this.forfeited = forfeited; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long totalTimeMs) { this.totalTimeMs = totalTimeMs; }
}
