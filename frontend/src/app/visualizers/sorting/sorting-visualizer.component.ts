import { Component, computed, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlgorithmStore } from '../../store/algorithm.store';
import { SortStep } from '../../models/algorithm.models';

@Component({
  selector: 'app-sorting-visualizer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sorting-visualizer.component.html',
})
export class SortingVisualizerComponent {
  @Input() source: 'primary' | 'compare' = 'primary';

  step = computed(() => {
    const data = this.source === 'primary'
      ? this.store.currentStepData()
      : this.store.compareCurrentStepData();
    return data as SortStep | null;
  });

  constructor(public store: AlgorithmStore) {}

  barHeight(value: number): number {
    const step = this.step();
    if (!step) return 0;
    const max = Math.max(...step.array);
    return Math.max(4, (value / max) * 220);
  }

  barColor(index: number): string {
    const s = this.step();
    if (!s) return '#3b82f6';
    if (s.sorted?.includes(index)) return '#10b981';
    if (s.swapping?.includes(index)) return '#ef4444';
    if (s.comparing?.includes(index)) return '#f59e0b';
    if (s.pivot === index) return '#a855f7';
    return '#3b82f6';
  }

  trackByIndex(i: number) { return i; }
}
