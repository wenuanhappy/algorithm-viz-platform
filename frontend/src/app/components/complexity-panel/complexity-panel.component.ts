import { Component, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlgorithmStore } from '../../store/algorithm.store';
import { AlgorithmService } from '../../services/algorithm.service';
import {
  SortStep, SearchStep, GraphStep, DPStep, NQueensStep, DivideConquerStep,
} from '../../models/algorithm.models';

interface MetricItem {
  label: string;
  value: number;
  max: number | null;
  color: string;
}
interface MetricsResult {
  items: MetricItem[];
  extra?: string;
}

interface AlgoInfo {
  name: string;
  time: string;
  worstTime: string;
  space: string;
  timeClass: 'excellent' | 'good' | 'fair' | 'poor';
}

const ALGO_INFO: Record<string, AlgoInfo> = {
  'quick-sort':     { name: '快速排序',      time: 'O(n log n)', worstTime: 'O(n²)',      space: 'O(log n)', timeClass: 'good' },
  'merge-sort':     { name: '归并排序',      time: 'O(n log n)', worstTime: 'O(n log n)', space: 'O(n)',     timeClass: 'good' },
  'bubble-sort':    { name: '冒泡排序',      time: 'O(n²)',      worstTime: 'O(n²)',      space: 'O(1)',     timeClass: 'poor' },
  'heap-sort':      { name: '堆排序',        time: 'O(n log n)', worstTime: 'O(n log n)', space: 'O(1)',     timeClass: 'good' },
  'insertion-sort': { name: '插入排序',      time: 'O(n²)',      worstTime: 'O(n²)',      space: 'O(1)',     timeClass: 'poor' },
  'binary-search':  { name: '二分查找',      time: 'O(log n)',   worstTime: 'O(log n)',   space: 'O(1)',     timeClass: 'excellent' },
  'bfs':            { name: 'BFS 广度优先',  time: 'O(V+E)',     worstTime: 'O(V+E)',     space: 'O(V)',     timeClass: 'fair' },
  'dfs':            { name: 'DFS 深度优先',  time: 'O(V+E)',     worstTime: 'O(V+E)',     space: 'O(V)',     timeClass: 'fair' },
  'dijkstra':       { name: 'Dijkstra',      time: 'O((V+E)logV)', worstTime: 'O((V+E)logV)', space: 'O(V)', timeClass: 'fair' },
  'prim':           { name: "Prim's MST",    time: 'O(E log V)', worstTime: 'O(E log V)', space: 'O(V)',     timeClass: 'fair' },
  'kruskal':        { name: "Kruskal's MST", time: 'O(E log E)', worstTime: 'O(E log E)', space: 'O(V)',     timeClass: 'fair' },
  'astar':          { name: 'A* 启发搜索',   time: 'O(E log V)', worstTime: 'O(b^d)',     space: 'O(b^d)',   timeClass: 'good' },
  'knapsack':       { name: '0/1 背包',      time: 'O(nW)',      worstTime: 'O(nW)',      space: 'O(nW)',    timeClass: 'fair' },
  'n-queens':       { name: 'N 皇后',        time: 'O(n!)',      worstTime: 'O(n!)',      space: 'O(n)',     timeClass: 'poor' },
  'karatsuba':      { name: 'Karatsuba 大整数乘法', time: 'O(n^log₂3)', worstTime: 'O(n^1.585)', space: 'O(log n)', timeClass: 'good' },
};

@Component({
  selector: 'app-complexity-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexity-panel.component.html',
})
export class ComplexityPanelComponent {
  constructor(
      public store: AlgorithmStore,
      private algorithmService: AlgorithmService
  ) {
  }

  algoInfo = computed<AlgoInfo | null>(() => ALGO_INFO[this.store.selectedAlgo()] ?? null);

  // ---- Metrics extracted from current step ----
  metrics = computed<MetricsResult | null>(() => {
    const step = this.store.currentStepData();
    const cat = this.store.category();
    if (!step) return null;

    if (cat === 'sorting') {
      const s = step as SortStep;
      const n = s.array.length;
      const maxCmp = n * (n - 1) / 2;
      return {
        items: [
          {label: '比较次数', value: s.comparisons, max: maxCmp, color: 'yellow'},
          {label: '交换次数', value: s.swaps, max: maxCmp, color: 'red'},
          {label: '访问次数', value: s.accesses, max: maxCmp * 2, color: 'blue'},
        ],
        extra: `n = ${n}`,
      };
    }
    if (cat === 'search') {
      const s = step as SearchStep;
      const n = s.array.length;
      const maxCmp = Math.ceil(Math.log2(n)) + 1;
      return {
        items: [
          {label: '比较次数', value: s.comparisons, max: maxCmp, color: 'yellow'},
        ],
        extra: `n = ${n}，最多 ⌈log₂n⌉ = ${maxCmp} 次`,
      };
    }
    if (cat === 'graph') {
      const s = step as GraphStep;
      const V = this.store.graphData().nodes.length;
      const E = this.store.graphData().edges.length;
      return {
        items: [
          {label: '已访问节点', value: s.visitedCount, max: V, color: 'green'},
          {label: '比较次数', value: s.comparisons, max: V * V, color: 'yellow'},
        ],
        extra: `V = ${V}，E = ${E}`,
      };
    }
    if (cat === 'dp') {
      const s = step as DPStep;
      const n = this.store.knapsackItems().length;
      const W = this.store.knapsackCap();
      return {
        items: [
          {label: '子问题计算', value: s.comparisons, max: n * W, color: 'yellow'},
        ],
        extra: `n = ${n}，W = ${W}，最多 n×W = ${n * W} 次`,
      };
    }
    if (cat === 'backtracking') {
      const s = step as NQueensStep;
      const n = this.store.queensN();
      return {
        items: [
          {label: '回溯次数', value: s.backtracks, max: null, color: 'red'},
          {label: '解的数量', value: s.solutionsFound, max: null, color: 'green'},
        ],
        extra: `N = ${n}`,
      };
    }
    if (cat === 'divide-conquer') {
      const s = step as DivideConquerStep;
      const n = Math.max(this.store.divideX().length, this.store.divideY().length);
      const maxDepth = Math.ceil(Math.log2(Math.max(n, 1)));

      return {
        items: [
          {label: '基础乘法', value: s.multiplications, max: null, color: 'yellow'},
          {label: '加减/组合', value: s.additions, max: null, color: 'blue'},
          {label: '递归深度', value: s.depth, max: Math.max(maxDepth, 1), color: 'green'},
        ],
        extra: `n = ${n}，递推 T(n)=3T(n/2)+O(n)`,
      };
    }
    return null;
  });

  stepProgress = computed(() => {
    const total = this.store.totalSteps();
    const cur = this.store.currentStep();
    return total > 0 ? Math.round(((cur + 1) / total) * 100) : 0;
  });

  timeClass(cls: string): string {
    return ({
      excellent: 'text-green-400',
      good: 'text-blue-400',
      fair: 'text-yellow-400',
      poor: 'text-red-400',
    } as Record<string, string>)[cls] ?? 'text-slate-400';
  }

  barWidth(value: number, max: number | null): number {
    if (max === null || max === 0) return 0;
    return Math.min(100, Math.round((value / max) * 100));
  }

  metricColor(color: string): string {
    return ({
      yellow: 'bg-yellow-500',
      red: 'bg-red-500',
      blue: 'bg-blue-500',
      green: 'bg-green-500',
    } as Record<string, string>)[color] ?? 'bg-slate-500';
  }

  metricTextColor(color: string): string {
    return ({
      yellow: 'text-yellow-400',
      red: 'text-red-400',
      blue: 'text-blue-400',
      green: 'text-green-400',
    } as Record<string, string>)[color] ?? 'text-slate-400';
  }
}
