package com.algorithmviz.dto;

public class JoinCompetitionRoomRequest {
    private Long userId;
    private String displayName;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
