package com.algorithmviz.model;

import java.util.List;

public class NQueensStep {
    private List<Integer> board;
    private int n;
    private int currentRow;
    private int currentCol;
    private List<int[]> conflicts;
    private int[] placing;
    private int[] removing;
    private String description;
    private int codeLine;
    private int backtracks;
    private int solutionsFound;
    private List<List<Integer>> solutions;
    private String phase;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final NQueensStep s = new NQueensStep();
        public Builder board(List<Integer> v) { s.board = v; return this; }
        public Builder n(int v) { s.n = v; return this; }
        public Builder currentRow(int v) { s.currentRow = v; return this; }
        public Builder currentCol(int v) { s.currentCol = v; return this; }
        public Builder conflicts(List<int[]> v) { s.conflicts = v; return this; }
        public Builder placing(int[] v) { s.placing = v; return this; }
        public Builder removing(int[] v) { s.removing = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder codeLine(int v) { s.codeLine = v; return this; }
        public Builder backtracks(int v) { s.backtracks = v; return this; }
        public Builder solutionsFound(int v) { s.solutionsFound = v; return this; }
        public Builder solutions(List<List<Integer>> v) { s.solutions = v; return this; }
        public Builder phase(String v) { s.phase = v; return this; }
        public NQueensStep build() { return s; }
    }

    public List<Integer> getBoard() { return board; }
    public int getN() { return n; }
    public int getCurrentRow() { return currentRow; }
    public int getCurrentCol() { return currentCol; }
    public List<int[]> getConflicts() { return conflicts; }
    public int[] getPlacing() { return placing; }
    public int[] getRemoving() { return removing; }
    public String getDescription() { return description; }
    public int getCodeLine() { return codeLine; }
    public int getBacktracks() { return backtracks; }
    public int getSolutionsFound() { return solutionsFound; }
    public List<List<Integer>> getSolutions() { return solutions; }
    public String getPhase() { return phase; }
}

