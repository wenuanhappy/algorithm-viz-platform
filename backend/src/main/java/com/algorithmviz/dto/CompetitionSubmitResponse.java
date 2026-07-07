package com.algorithmviz.dto;

public class CompetitionSubmitResponse {
    private boolean correct;
    private String correctAnswer;
    private int awardedPoints;
    private int score;
    private boolean duplicate;

    public CompetitionSubmitResponse(boolean correct, String correctAnswer, int awardedPoints, int score) {
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.awardedPoints = awardedPoints;
        this.score = score;
    }

    public CompetitionSubmitResponse(boolean correct, String correctAnswer, int awardedPoints, int score, boolean duplicate) {
        this(correct, correctAnswer, awardedPoints, score);
        this.duplicate = duplicate;
    }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public int getAwardedPoints() { return awardedPoints; }
    public void setAwardedPoints(int awardedPoints) { this.awardedPoints = awardedPoints; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
}
