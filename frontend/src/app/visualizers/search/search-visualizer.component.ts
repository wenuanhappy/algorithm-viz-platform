import { Component, computed, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlgorithmStore } from '../../store/algorithm.store';
import { SearchStep } from '../../models/algorithm.models';

@Component({
  selector: 'app-search-visualizer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-visualizer.component.html',
})
export class SearchVisualizerComponent {
  @Input() source: 'primary' | 'compare' = 'primary';

  step = computed(() => {
    const data = this.source === 'primary'
      ? this.store.currentStepData()
      : this.store.compareCurrentStepData();
    return data as SearchStep | null;
  });

  constructor(public store: AlgorithmStore) {}

  cellColor(index: number): string {
    const s = this.step();
    if (!s) return 'bg-slate-700';
    if (s.found && index === s.mid) return 'bg-green-600';
    if (index === s.mid)   return 'bg-purple-600';
    if (index === s.left || index === s.right) return 'bg-yellow-600/60';
    if (s.eliminated === 'left'  && index < s.mid)  return 'bg-slate-800 opacity-40';
    if (s.eliminated === 'right' && index > s.mid)  return 'bg-slate-800 opacity-40';
    if (index >= s.left && index <= s.right) return 'bg-blue-700/60';
    return 'bg-slate-700 opacity-30';
  }

  trackByIndex(i: number) { return i; }
}
