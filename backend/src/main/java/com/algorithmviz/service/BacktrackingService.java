package com.algorithmviz.service;

import com.algorithmviz.model.NQueensStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BacktrackingService {

    public List<NQueensStep> generateSteps(String algorithm, int n) {
        return switch (algorithm) {
            case "n-queens" -> nQueens(n);
            default -> throw new IllegalArgumentException("Unknown backtracking algorithm: " + algorithm);
        };
    }

    private List<NQueensStep> nQueens(int n) {
        List<NQueensStep> steps = new ArrayList<>();
        int[] board = new int[n];
        Arrays.fill(board, -1);
        List<List<Integer>> allSolutions = new ArrayList<>();
        int[] backtracks = {0};
        int[] solutionsFound = {0};

        steps.add(NQueensStep.builder()
                .board(toList(board)).n(n).currentRow(0).currentCol(-1)
                .conflicts(List.of()).placing(null).removing(null)
                .description("N 皇后初始化：创建 " + n + "×" + n + " 棋盘，开始尝试放置")
                .codeLine(1).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                .solutions(List.of()).phase("init").build());

        solveNQueens(0, n, board, allSolutions, backtracks, solutionsFound, steps);

        steps.add(NQueensStep.builder()
                .board(toList(board)).n(n).currentRow(n).currentCol(-1)
                .conflicts(List.of()).placing(null).removing(null)
                .description("✓ N 皇后求解完成！共找到 " + solutionsFound[0] + " 个解")
                .codeLine(6).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                .solutions(new ArrayList<>(allSolutions)).phase("done").build());

        return steps;
    }

    private void solveNQueens(int row, int n, int[] board,
                               List<List<Integer>> allSolutions,
                               int[] backtracks, int[] solutionsFound,
                               List<NQueensStep> steps) {
        if (row == n) {
            solutionsFound[0]++;
            List<Integer> sol = toList(board);
            allSolutions.add(sol);
            steps.add(NQueensStep.builder()
                    .board(new ArrayList<>(sol)).n(n).currentRow(row).currentCol(-1)
                    .conflicts(List.of()).placing(null).removing(null)
                    .description("🎉 找到第 " + solutionsFound[0] + " 个解！棋盘配置：[" + sol.toString().replaceAll("[\\[\\] ]", "") + "]")
                    .codeLine(2).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                    .solutions(new ArrayList<>(allSolutions)).phase("solution_found").build());
            return;
        }

        for (int col = 0; col < n; col++) {
            List<int[]> conflicts = isSafe(board, row, col);

            steps.add(NQueensStep.builder()
                    .board(toList(board)).n(n).currentRow(row).currentCol(col)
                    .conflicts(conflicts).placing(new int[]{row, col}).removing(null)
                    .description(conflicts.isEmpty()
                            ? "尝试在 (" + row + ", " + col + ") 放置皇后 → 安全！"
                            : "尝试在 (" + row + ", " + col + ") 放置皇后 → 冲突！")
                    .codeLine(3).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                    .solutions(new ArrayList<>(allSolutions)).phase("try_place").build());

            if (conflicts.isEmpty()) {
                board[row] = col;
                steps.add(NQueensStep.builder()
                        .board(toList(board)).n(n).currentRow(row).currentCol(col)
                        .conflicts(List.of()).placing(new int[]{row, col}).removing(null)
                        .description("✓ 在 (" + row + ", " + col + ") 放置皇后，进入下一行")
                        .codeLine(4).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                        .solutions(new ArrayList<>(allSolutions)).phase("place").build());

                solveNQueens(row + 1, n, board, allSolutions, backtracks, solutionsFound, steps);

                board[row] = -1;
                backtracks[0]++;
                steps.add(NQueensStep.builder()
                        .board(toList(board)).n(n).currentRow(row).currentCol(col)
                        .conflicts(List.of()).placing(null).removing(new int[]{row, col})
                        .description("回溯：撤销 (" + row + ", " + col + ") 的皇后，尝试下一列")
                        .codeLine(5).backtracks(backtracks[0]).solutionsFound(solutionsFound[0])
                        .solutions(new ArrayList<>(allSolutions)).phase("backtrack").build());
            }
        }
    }

    private List<int[]> isSafe(int[] board, int row, int col) {
        List<int[]> conflicts = new ArrayList<>();
        for (int r = 0; r < row; r++) {
            int c = board[r];
            if (c == col || Math.abs(c - col) == Math.abs(r - row)) {
                conflicts.add(new int[]{r, c});
            }
        }
        return conflicts;
    }

    private List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }
}
