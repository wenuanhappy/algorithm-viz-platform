package com.algorithmviz.service;

import com.algorithmviz.model.SearchStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    public List<SearchStep> generateSteps(String algorithm, List<Integer> array, int target) {
        return switch (algorithm) {
            case "binary-search" -> binarySearch(array, target);
            default -> throw new IllegalArgumentException("Unknown search algorithm: " + algorithm);
        };
    }

    private List<SearchStep> binarySearch(List<Integer> sortedArr, int target) {
        List<SearchStep> steps = new ArrayList<>();
        List<Integer> array = new ArrayList<>(sortedArr);
        int comparisons = 0;
        int left = 0, right = array.size() - 1;

        steps.add(SearchStep.builder()
                .array(array).left(left).right(right)
                .mid((left + right) / 2).target(target)
                .found(null).eliminated(null)
                .description("在有序数组中查找目标值 " + target + "，初始范围 [0, " + right + "]")
                .codeLine(1).comparisons(comparisons).phase("init").build());

        while (left <= right) {
            int mid = (left + right) / 2;
            comparisons++;

            steps.add(SearchStep.builder()
                    .array(array).left(left).right(right).mid(mid).target(target)
                    .found(null).eliminated(null)
                    .description("计算中间索引 mid = (" + left + " + " + right + ") / 2 = " + mid
                            + "，arr[mid] = " + array.get(mid))
                    .codeLine(2).comparisons(comparisons).phase("calculate_mid").build());

            if (array.get(mid).equals(target)) {
                steps.add(SearchStep.builder()
                        .array(array).left(left).right(right).mid(mid).target(target)
                        .found(true).eliminated(null)
                        .description("✓ 找到目标值 " + target + "，位于索引 " + mid + "！")
                        .codeLine(3).comparisons(comparisons).phase("found").build());
                return steps;
            } else if (array.get(mid) < target) {
                steps.add(SearchStep.builder()
                        .array(array).left(left).right(right).mid(mid).target(target)
                        .found(null).eliminated("left")
                        .description("arr[mid]=" + array.get(mid) + " < " + target + "，排除左半部分，left = " + (mid + 1))
                        .codeLine(4).comparisons(comparisons).phase("eliminate").build());
                left = mid + 1;
            } else {
                steps.add(SearchStep.builder()
                        .array(array).left(left).right(right).mid(mid).target(target)
                        .found(null).eliminated("right")
                        .description("arr[mid]=" + array.get(mid) + " > " + target + "，排除右半部分，right = " + (mid - 1))
                        .codeLine(5).comparisons(comparisons).phase("eliminate").build());
                right = mid - 1;
            }
        }

        steps.add(SearchStep.builder()
                .array(array).left(left).right(right)
                .mid((left + right) / 2).target(target)
                .found(false).eliminated(null)
                .description("✗ 未找到目标值 " + target)
                .codeLine(6).comparisons(comparisons).phase("not_found").build());
        return steps;
    }
}
