import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlgorithmCategory, AlgorithmId, AssessmentConfig, UserQuestionType } from '../../../models/algorithm.models';

interface CategoryAlgo {
  id: AlgorithmId;
  label: string;
}

@Component({
  selector: 'app-assessment-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assessment-settings.component.html',
})
export class AssessmentSettingsComponent {
  @Output() start = new EventEmitter<AssessmentConfig>();
  @Input() aiAvailable = false;

  questionCount = signal(5);
  customCount = signal('');
  mode = signal<'ai' | 'fixed'>('ai');
  difficulty = signal<'easy' | 'medium' | 'hard' | 'mixed'>('medium');

  categories = signal<AlgorithmCategory[]>([
    'sorting', 'search', 'graph', 'dp'
  ]);

  questionTypes = signal<UserQuestionType[]>(['fill', 'choice', 'short-answer']);

  readonly categoryAlgorithmMap: Record<AlgorithmCategory, CategoryAlgo[]> = {
    sorting: [
      { id: 'quick-sort', label: '快速排序' },
      { id: 'merge-sort', label: '归并排序' },
      { id: 'bubble-sort', label: '冒泡排序' },
      { id: 'heap-sort', label: '堆排序' },
      { id: 'insertion-sort', label: '插入排序' },
    ],
    search: [
      { id: 'binary-search', label: '二分查找' },
    ],
    graph: [
      { id: 'dijkstra', label: 'Dijkstra' },
      { id: 'bfs', label: 'BFS' },
      { id: 'dfs', label: 'DFS' },
      { id: 'prim', label: 'Prim' },
      { id: 'kruskal', label: 'Kruskal' },
      { id: 'astar', label: 'A*' },
    ],
    dp: [
      { id: 'knapsack', label: '0/1 背包' },
    ],
    backtracking: [
      { id: 'n-queens', label: 'N 皇后' },
    ],
    'divide-conquer': [
      { id: 'karatsuba', label: 'Karatsuba 乘法' },
    ],
    'vr-3d': [],
  };

  readonly categoryLabels: Record<AlgorithmCategory, string> = {
    sorting: '排序',
    search: '搜索',
    graph: '图算法',
    dp: '动态规划',
    backtracking: '回溯',
    'divide-conquer': '分治',
    'vr-3d': '3D 数据结构',
  };

  readonly allCategories: AlgorithmCategory[] = [
    'sorting', 'search', 'graph', 'dp', 'backtracking', 'divide-conquer'
  ];

  readonly questionTypeLabels: Record<UserQuestionType, string> = {
    fill: '填空题',
    choice: '选择题',
    'short-answer': '简答题',
  };

  readonly allQuestionTypes: UserQuestionType[] = ['fill', 'choice', 'short-answer'];

  readonly difficulties = [
    { value: 'easy' as const, label: '入门' },
    { value: 'medium' as const, label: '中等' },
    { value: 'hard' as const, label: '困难' },
    { value: 'mixed' as const, label: '混合' },
  ];

  // per-category algorithm selections (all selected by default when category checked)
  selectedAlgos = signal<Record<AlgorithmCategory, AlgorithmId[]>>({
    sorting: ['quick-sort', 'merge-sort', 'bubble-sort', 'heap-sort', 'insertion-sort'],
    search: ['binary-search'],
    graph: ['dijkstra', 'bfs', 'dfs', 'prim', 'kruskal', 'astar'],
    dp: ['knapsack'],
    backtracking: ['n-queens'],
    'divide-conquer': ['karatsuba'],
    'vr-3d': [],
  });

  toggleCategory(cat: AlgorithmCategory): void {
    const current = this.categories();
    if (current.includes(cat)) {
      this.categories.set(current.filter(c => c !== cat));
    } else {
      this.categories.set([...current, cat]);
    }
  }

  toggleAlgo(cat: AlgorithmCategory, algo: AlgorithmId): void {
    const map = { ...this.selectedAlgos() };
    const list = map[cat];
    if (list.includes(algo)) {
      map[cat] = list.filter(a => a !== algo);
    } else {
      map[cat] = [...list, algo];
    }
    this.selectedAlgos.set(map);
  }

  toggleAlgoClick(cat: AlgorithmCategory, algo: AlgorithmId, event: Event): void {
    event.stopPropagation();
    this.toggleAlgo(cat, algo);
  }

  setCount(count: number): void {
    this.questionCount.set(count);
    this.customCount.set('');
  }

  setCustomCount(value: string): void {
    this.customCount.set(value);
    const n = parseInt(value, 10);
    if (n >= 1 && n <= 15) {
      this.questionCount.set(n);
    }
  }

  toggleQuestionType(type: UserQuestionType): void {
    const current = this.questionTypes();
    if (current.includes(type) && current.length > 1) {
      this.questionTypes.set(current.filter(t => t !== type));
    } else if (!current.includes(type)) {
      this.questionTypes.set([...current, type]);
    }
  }

  getFlatAlgorithms(): AlgorithmId[] {
    return this.categories().flatMap(cat => this.selectedAlgos()[cat]);
  }

  isCategoryChecked(cat: AlgorithmCategory): boolean {
    return this.categories().includes(cat);
  }

  isAlgoChecked(cat: AlgorithmCategory, algo: AlgorithmId): boolean {
    return this.selectedAlgos()[cat]?.includes(algo) ?? false;
  }

  getAlgoCountForCategory(cat: AlgorithmCategory): number {
    const algos = this.selectedAlgos()[cat] ?? [];
    return algos.length;
  }

  getTotalAlgoCountForCategory(cat: AlgorithmCategory): number {
    return this.categoryAlgorithmMap[cat].length;
  }

  isTypeChecked(type: UserQuestionType): boolean {
    return this.questionTypes().includes(type);
  }

  trackAlgo(index: number, algo: CategoryAlgo): string {
    return algo.id;
  }

  startAssessment(): void {
    this.start.emit({
      questionCount: this.questionCount(),
      categories: this.categories(),
      algorithms: this.getFlatAlgorithms(),
      difficulty: this.difficulty(),
      mode: this.mode(),
      questionTypes: this.questionTypes(),
    });
  }
}
