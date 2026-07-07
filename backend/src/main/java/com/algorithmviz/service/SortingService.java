package com.algorithmviz.service;

import com.algorithmviz.model.SortStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class SortingService {

    public List<SortStep> generateSteps(String algorithm, List<Integer> input) {
        return switch (algorithm) {
            case "quick-sort"      -> quickSort(new ArrayList<>(input));
            case "merge-sort"      -> mergeSort(new ArrayList<>(input));
            case "bubble-sort"     -> bubbleSort(new ArrayList<>(input));
            case "heap-sort"       -> heapSort(new ArrayList<>(input));
            case "insertion-sort"  -> insertionSort(new ArrayList<>(input));
            default -> throw new IllegalArgumentException("Unknown sort algorithm: " + algorithm);
        };
    }

    // ==================== QUICK SORT ====================
    private List<SortStep> quickSort(List<Integer> input) {
        List<SortStep> steps = new ArrayList<>();
        int[] arr = input.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> sorted = new ArrayList<>();
        int[] comparisons = {0}, swaps = {0}, accesses = {0};

        quickSortHelper(arr, 0, arr.length - 1, sorted, comparisons, swaps, accesses, steps);

        List<Integer> allSorted = IntStream.range(0, arr.length).boxed().collect(Collectors.toList());
        steps.add(buildSortStep(arr, allSorted, comparisons[0], swaps[0], accesses[0],
                List.of(), List.of(), null, 0, arr.length - 1, "✓ 快速排序完成！", 8, "done"));
        return steps;
    }

    private int partition(int[] arr, int low, int high,
                          List<Integer> sorted, int[] cmp, int[] swp, int[] acc,
                          List<SortStep> steps) {
        int pivot = arr[high];
        acc[0]++;
        steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                List.of(), List.of(), high, low, high,
                "选择基准值 arr[" + high + "] = " + pivot + "，开始分区 [" + low + ", " + high + "]", 1, "select_pivot"));

        int i = low - 1;
        for (int j = low; j < high; j++) {
            cmp[0]++;
            acc[0] += 2;
            steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                    List.of(j, high), List.of(), high, low, high,
                    "比较 arr[" + j + "]=" + arr[j] + " 与基准值 " + pivot, 3, "compare"));

            if (arr[j] <= pivot) {
                i++;
                if (i != j) {
                    int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
                    swp[0]++;
                    acc[0] += 2;
                    steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                            List.of(), List.of(i, j), high, low, high,
                            "交换 arr[" + i + "]↔arr[" + j + "]（现在为 " + arr[i] + ", " + arr[j] + "）", 5, "swap"));
                }
            }
        }

        int tmp = arr[i + 1]; arr[i + 1] = arr[high]; arr[high] = tmp;
        swp[0]++;
        acc[0] += 2;
        int pivotFinal = i + 1;
        sorted.add(pivotFinal);
        steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                List.of(), List.of(i + 1, high), pivotFinal, low, high,
                "基准值归位：arr[" + pivotFinal + "] = " + arr[pivotFinal], 7, "pivot_placed"));
        return pivotFinal;
    }

    private void quickSortHelper(int[] arr, int low, int high,
                                 List<Integer> sorted, int[] cmp, int[] swp, int[] acc,
                                 List<SortStep> steps) {
        if (low >= high) {
            if (low == high) sorted.add(low);
            return;
        }
        int p = partition(arr, low, high, sorted, cmp, swp, acc, steps);
        quickSortHelper(arr, low, p - 1, sorted, cmp, swp, acc, steps);
        quickSortHelper(arr, p + 1, high, sorted, cmp, swp, acc, steps);
    }

    // ==================== MERGE SORT ====================
    private List<SortStep> mergeSort(List<Integer> input) {
        List<SortStep> steps = new ArrayList<>();
        int[] arr = input.stream().mapToInt(Integer::intValue).toArray();
        int[] cmp = {0}, swp = {0}, acc = {0};

        mergeSortHelper(arr, 0, arr.length - 1, cmp, swp, acc, steps);

        List<Integer> allSorted = IntStream.range(0, arr.length).boxed().collect(Collectors.toList());
        steps.add(buildSortStep(arr, allSorted, cmp[0], swp[0], acc[0],
                List.of(), List.of(), null, 0, arr.length - 1, "✓ 归并排序完成！", 7, "done"));
        return steps;
    }

    private void mergeSortHelper(int[] arr, int left, int right,
                                 int[] cmp, int[] swp, int[] acc, List<SortStep> steps) {
        if (left >= right) return;
        int mid = (left + right) / 2;
        steps.add(buildSortStep(arr, List.of(), cmp[0], swp[0], acc[0],
                List.of(), List.of(), null, left, right,
                "分割 [" + left + ".." + right + "] → [" + left + ".." + mid + "] 和 [" + (mid + 1) + ".." + right + "]", 1, "divide"));
        mergeSortHelper(arr, left, mid, cmp, swp, acc, steps);
        mergeSortHelper(arr, mid + 1, right, cmp, swp, acc, steps);
        merge(arr, left, mid, right, cmp, swp, acc, steps);
    }

    private void merge(int[] arr, int left, int mid, int right,
                       int[] cmp, int[] swp, int[] acc, List<SortStep> steps) {
        int[] leftArr = Arrays.copyOfRange(arr, left, mid + 1);
        int[] rightArr = Arrays.copyOfRange(arr, mid + 1, right + 1);
        acc[0] += leftArr.length + rightArr.length;

        List<Integer> leftList = toList(leftArr);
        List<Integer> rightList = toList(rightArr);
        steps.add(SortStep.builder()
                .array(toList(arr)).comparing(List.of()).swapping(List.of())
                .sorted(List.of()).pivot(null).rangeLeft(left).rangeRight(right)
                .mergeLeft(leftList).mergeRight(rightList).mergeTarget(left)
                .description("合并子数组 [" + left + ".." + mid + "] 和 [" + (mid + 1) + ".." + right + "]")
                .codeLine(2).comparisons(cmp[0]).swaps(swp[0]).accesses(acc[0])
                .phase("merge_setup").build());

        int i = 0, j = 0, k = left;
        while (i < leftArr.length && j < rightArr.length) {
            cmp[0]++;
            acc[0] += 2;
            if (leftArr[i] <= rightArr[j]) {
                arr[k] = leftArr[i++];
            } else {
                arr[k] = rightArr[j++];
            }
            swp[0]++;
            steps.add(SortStep.builder()
                    .array(toList(arr)).comparing(List.of()).swapping(List.of(k))
                    .sorted(List.of()).pivot(null).rangeLeft(left).rangeRight(right)
                    .mergeLeft(toList(Arrays.copyOfRange(leftArr, i, leftArr.length)))
                    .mergeRight(toList(Arrays.copyOfRange(rightArr, j, rightArr.length)))
                    .mergeTarget(k + 1)
                    .description("放入元素 " + arr[k] + " → arr[" + k + "]")
                    .codeLine(4).comparisons(cmp[0]).swaps(swp[0]).accesses(acc[0])
                    .phase("merge_place").build());
            k++;
        }
        while (i < leftArr.length) {
            arr[k] = leftArr[i++];
            swp[0]++;
            steps.add(SortStep.builder()
                    .array(toList(arr)).comparing(List.of()).swapping(List.of(k))
                    .sorted(List.of()).pivot(null).rangeLeft(left).rangeRight(right)
                    .mergeLeft(toList(Arrays.copyOfRange(leftArr, i, leftArr.length)))
                    .mergeRight(List.of()).mergeTarget(k + 1)
                    .description("放入剩余左半 " + arr[k] + " → arr[" + k + "]")
                    .codeLine(5).comparisons(cmp[0]).swaps(swp[0]).accesses(acc[0])
                    .phase("merge_place").build());
            k++;
        }
        while (j < rightArr.length) {
            arr[k] = rightArr[j++];
            swp[0]++;
            steps.add(SortStep.builder()
                    .array(toList(arr)).comparing(List.of()).swapping(List.of(k))
                    .sorted(List.of()).pivot(null).rangeLeft(left).rangeRight(right)
                    .mergeLeft(List.of())
                    .mergeRight(toList(Arrays.copyOfRange(rightArr, j, rightArr.length)))
                    .mergeTarget(k + 1)
                    .description("放入剩余右半 " + arr[k] + " → arr[" + k + "]")
                    .codeLine(6).comparisons(cmp[0]).swaps(swp[0]).accesses(acc[0])
                    .phase("merge_place").build());
            k++;
        }
    }

    // ==================== BUBBLE SORT ====================
    private List<SortStep> bubbleSort(List<Integer> input) {
        List<SortStep> steps = new ArrayList<>();
        int[] arr = input.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> sorted = new ArrayList<>();
        int cmp = 0, swp = 0, acc = 0;

        for (int i = 0; i < arr.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < arr.length - 1 - i; j++) {
                cmp++;
                acc += 2;
                steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                        List.of(j, j + 1), List.of(), null, 0, arr.length - 1 - i,
                        "比较 arr[" + j + "]=" + arr[j] + " 与 arr[" + (j + 1) + "]=" + arr[j + 1], 2, "compare"));
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = tmp;
                    swp++;
                    acc += 2;
                    swapped = true;
                    steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                            List.of(), List.of(j, j + 1), null, 0, arr.length - 1 - i,
                            "交换 arr[" + j + "]↔arr[" + (j + 1) + "]（现在为 " + arr[j] + ", " + arr[j + 1] + "）", 3, "swap"));
                }
            }
            sorted.add(arr.length - 1 - i);
            steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                    List.of(), List.of(), null, 0, arr.length - 1 - i,
                    "第 " + (arr.length - 1 - i) + " 轮结束，元素 " + arr[arr.length - 1 - i] + " 已归位", 4, "bubble_complete"));
            if (!swapped) break;
        }

        List<Integer> allSorted = IntStream.range(0, arr.length).boxed().collect(Collectors.toList());
        steps.add(buildSortStep(arr, allSorted, cmp, swp, acc,
                List.of(), List.of(), null, 0, arr.length - 1, "✓ 冒泡排序完成！", 5, "done"));
        return steps;
    }

    // ==================== HEAP SORT ====================
    private List<SortStep> heapSort(List<Integer> input) {
        List<SortStep> steps = new ArrayList<>();
        int[] arr = input.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> sorted = new ArrayList<>();
        int[] cmp = {0}, swp = {0}, acc = {0};
        int n = arr.length;

        steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                List.of(), List.of(), null, 0, arr.length - 1,
                "开始建堆：从最后一个非叶节点开始向下调整", 1, "build_heap"));

        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i, sorted, cmp, swp, acc, steps);
        }

        for (int i = n - 1; i > 0; i--) {
            int tmp = arr[0]; arr[0] = arr[i]; arr[i] = tmp;
            swp[0]++;
            acc[0] += 2;
            sorted.add(i);
            steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                    List.of(), List.of(0, i), null, 0, arr.length - 1,
                    "将堆顶 " + arr[i] + " 移至末尾，堆大小减为 " + i, 5, "extract_max"));
            heapify(arr, i, 0, sorted, cmp, swp, acc, steps);
        }
        sorted.add(0);
        List<Integer> allSorted = IntStream.range(0, arr.length).boxed().collect(Collectors.toList());
        steps.add(buildSortStep(arr, allSorted, cmp[0], swp[0], acc[0],
                List.of(), List.of(), null, 0, arr.length - 1, "✓ 堆排序完成！", 7, "done"));
        return steps;
    }

    private void heapify(int[] arr, int n, int i,
                         List<Integer> sorted, int[] cmp, int[] swp, int[] acc,
                         List<SortStep> steps) {
        int largest = i;
        int l = 2 * i + 1, r = 2 * i + 2;

        if (l < n) {
            cmp[0]++;
            acc[0] += 2;
            steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                    List.of(l, largest), List.of(), null, 0, arr.length - 1,
                    "比较左子节点 arr[" + l + "]=" + arr[l] + " 与父节点 arr[" + largest + "]=" + arr[largest], 2, "heapify_compare"));
            if (arr[l] > arr[largest]) largest = l;
        }
        if (r < n) {
            cmp[0]++;
            acc[0] += 2;
            steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                    List.of(r, largest), List.of(), null, 0, arr.length - 1,
                    "比较右子节点 arr[" + r + "]=" + arr[r] + " 与当前最大 arr[" + largest + "]=" + arr[largest], 3, "heapify_compare"));
            if (arr[r] > arr[largest]) largest = r;
        }
        if (largest != i) {
            int tmp = arr[i]; arr[i] = arr[largest]; arr[largest] = tmp;
            swp[0]++;
            acc[0] += 2;
            steps.add(buildSortStep(arr, sorted, cmp[0], swp[0], acc[0],
                    List.of(), List.of(i, largest), null, 0, arr.length - 1,
                    "交换 arr[" + i + "]↔arr[" + largest + "]（" + arr[i] + " ↔ " + arr[largest] + "）", 4, "heapify_swap"));
            heapify(arr, n, largest, sorted, cmp, swp, acc, steps);
        }
    }

    // ==================== INSERTION SORT ====================
    private List<SortStep> insertionSort(List<Integer> input) {
        List<SortStep> steps = new ArrayList<>();
        int[] arr = input.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> sorted = new ArrayList<>();
        int cmp = 0, swp = 0, acc = 0;

        sorted.add(0);
        steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                List.of(), List.of(), null, 0, arr.length - 1,
                "arr[0]=" + arr[0] + " 单独构成有序序列", 1, "key_select"));

        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            acc++;
            steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                    List.of(i), List.of(), null, 0, i,
                    "取出 key = arr[" + i + "] = " + key + "，准备插入有序区", 2, "key_select"));
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                cmp++;
                acc += 2;
                steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                        List.of(j, j + 1), List.of(), null, 0, i,
                        "arr[" + j + "]=" + arr[j] + " > key=" + key + "，后移", 3, "shift"));
                arr[j + 1] = arr[j];
                swp++;
                acc++;
                j--;
            }
            if (j >= 0) {
                cmp++;
                steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                        List.of(j), List.of(), null, 0, i,
                        "arr[" + j + "]=" + arr[j] + " ≤ key=" + key + "，停止移动", 3, "shift"));
            }
            arr[j + 1] = key;
            acc++;
            sorted.add(i);
            steps.add(buildSortStep(arr, sorted, cmp, swp, acc,
                    List.of(), List.of(j + 1), null, 0, i,
                    "将 key=" + key + " 插入到位置 " + (j + 1), 4, "insert"));
        }

        List<Integer> allSorted = IntStream.range(0, arr.length).boxed().collect(Collectors.toList());
        steps.add(buildSortStep(arr, allSorted, cmp, swp, acc,
                List.of(), List.of(), null, 0, arr.length - 1, "✓ 插入排序完成！", 5, "done"));
        return steps;
    }

    // ==================== HELPERS ====================
    private SortStep buildSortStep(int[] arr, List<Integer> sorted,
                                   int cmp, int swp, int acc,
                                   List<Integer> comparing, List<Integer> swapping,
                                   Integer pivot, int rangeLeft, int rangeRight,
                                   String description, int codeLine, String phase) {
        return SortStep.builder()
                .array(toList(arr))
                .sorted(new ArrayList<>(sorted))
                .comparing(comparing)
                .swapping(swapping)
                .pivot(pivot)
                .rangeLeft(rangeLeft)
                .rangeRight(rangeRight)
                .description(description)
                .codeLine(codeLine)
                .comparisons(cmp)
                .swaps(swp)
                .accesses(acc)
                .phase(phase)
                .build();
    }

    private List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }
}
