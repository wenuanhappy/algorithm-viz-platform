package com.algorithmviz.service;

import com.algorithmviz.dto.DPRequest;
import com.algorithmviz.model.DPStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DPService {

    public List<DPStep> generateSteps(String algorithm, List<DPRequest.KnapsackItemDto> items, int capacity) {
        return switch (algorithm) {
            case "knapsack" -> knapsack(items, capacity);
            default -> throw new IllegalArgumentException("Unknown DP algorithm: " + algorithm);
        };
    }

    private List<DPStep> knapsack(List<DPRequest.KnapsackItemDto> items, int capacity) {
        List<DPStep> steps = new ArrayList<>();
        int n = items.size();
        int[][] dp = new int[n + 1][capacity + 1];
        int comparisons = 0;

        steps.add(DPStep.builder()
                .dp(copyDp(dp)).currentItem(-1).currentWeight(-1)
                .decision("init")
                .description("初始化 DP 表：dp[i][w] 表示前 i 件物品、容量为 w 时的最大价值")
                .codeLine(1).totalValue(0).selectedItems(List.of()).tracePath(List.of()).comparisons(0)
                .phase("init").build());

        for (int i = 1; i <= n; i++) {
            DPRequest.KnapsackItemDto item = items.get(i - 1);
            for (int w = 0; w <= capacity; w++) {
                comparisons++;
                if (item.getWeight() > w) {
                    dp[i][w] = dp[i - 1][w];
                    steps.add(DPStep.builder()
                            .dp(copyDp(dp)).currentItem(i).currentWeight(w)
                            .decision("skip")
                            .description("物品 " + i + "（" + item.getName() + ", 重量=" + item.getWeight()
                                    + "）超过当前容量 " + w + "，跳过 → dp[" + i + "][" + w + "]=" + dp[i][w])
                            .codeLine(3).totalValue(dp[i][w]).selectedItems(List.of()).tracePath(List.of()).comparisons(comparisons)
                            .phase("skip_weight").build());
                } else {
                    int withItem = dp[i - 1][w - item.getWeight()] + item.getValue();
                    int withoutItem = dp[i - 1][w];
                    comparisons++;
                    steps.add(DPStep.builder()
                            .dp(copyDp(dp)).currentItem(i).currentWeight(w)
                            .decision("compare")
                            .description("比较：不取物品 " + i + " = dp[" + (i - 1) + "][" + w + "]=" + withoutItem
                                    + "，取物品 " + i + " = dp[" + (i - 1) + "][" + (w - item.getWeight()) + "]+" + item.getValue() + "=" + withItem)
                            .codeLine(4).totalValue(Math.max(withItem, withoutItem)).selectedItems(List.of()).tracePath(List.of()).comparisons(comparisons)
                            .phase("compare").build());

                    if (withItem > withoutItem) {
                        dp[i][w] = withItem;
                        steps.add(DPStep.builder()
                                .dp(copyDp(dp)).currentItem(i).currentWeight(w)
                                .decision("take")
                                .description("选择取物品 " + i + "（" + item.getName() + "）→ dp[" + i + "][" + w + "] = " + withItem)
                                .codeLine(5).totalValue(withItem).selectedItems(List.of()).tracePath(List.of()).comparisons(comparisons)
                                .phase("take").build());
                    } else {
                        dp[i][w] = withoutItem;
                        steps.add(DPStep.builder()
                                .dp(copyDp(dp)).currentItem(i).currentWeight(w)
                                .decision("skip")
                                .description("选择不取物品 " + i + "（" + item.getName() + "）→ dp[" + i + "][" + w + "] = " + withoutItem)
                                .codeLine(5).totalValue(withoutItem).selectedItems(List.of()).tracePath(List.of()).comparisons(comparisons)
                                .phase("skip").build());
                    }
                }
            }
        }

        // Traceback
        List<Integer> selectedItems = new ArrayList<>();
        List<int[]> tracePath = new ArrayList<>();
        int w = capacity;
        for (int i = n; i > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                selectedItems.add(i - 1);
                tracePath.add(new int[]{i, w});
                w -= items.get(i - 1).getWeight();
            } else {
                tracePath.add(new int[]{i, w});
            }
        }

        String selectedNames = selectedItems.stream()
                .map(idx -> items.get(idx).getName())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);

        steps.add(DPStep.builder()
                .dp(copyDp(dp)).currentItem(-1).currentWeight(-1)
                .decision(null)
                .description("✓ 背包问题求解完成！最大价值 = " + dp[n][capacity] + "，选取物品：" + selectedNames)
                .codeLine(6).totalValue(dp[n][capacity]).selectedItems(selectedItems).tracePath(tracePath).comparisons(comparisons)
                .phase("traceback").build());

        return steps;
    }

    private List<List<Integer>> copyDp(int[][] dp) {
        List<List<Integer>> copy = new ArrayList<>();
        for (int[] row : dp) {
            List<Integer> r = new ArrayList<>();
            for (int v : row) r.add(v);
            copy.add(r);
        }
        return copy;
    }
}
