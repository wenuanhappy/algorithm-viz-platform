package com.algorithmviz.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRoom {
    private String roomId;
    private String algorithm;
    private String status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant lastActivityAt;
    private int questionCount;
    private List<CompetitionPlayer> players = new ArrayList<>();
    private List<CompetitionQuestion> questions = new ArrayList<>();

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public List<CompetitionPlayer> getPlayers() { return players; }
    public void setPlayers(List<CompetitionPlayer> players) { this.players = players; }
    public List<CompetitionQuestion> getQuestions() { return questions; }
    public void setQuestions(List<CompetitionQuestion> questions) { this.questions = questions; }
}
