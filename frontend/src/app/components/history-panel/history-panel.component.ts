import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlgorithmService } from '../../services/algorithm.service';
import { RunHistory } from '../../models/algorithm.models';

@Component({
  selector: 'app-history-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './history-panel.component.html',
})
export class HistoryPanelComponent implements OnInit {
  history: RunHistory[] = [];
  loading = false;

  constructor(private svc: AlgorithmService) {}

  ngOnInit(): void { this.load(); }

  load(category?: string): void {
    this.loading = true;
    this.svc.getHistory(category).subscribe({
      next: h => { this.history = h; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  delete(id: number): void {
    this.svc.deleteHistory(id).subscribe(() => {
      this.history = this.history.filter(h => h.id !== id);
    });
  }
}
