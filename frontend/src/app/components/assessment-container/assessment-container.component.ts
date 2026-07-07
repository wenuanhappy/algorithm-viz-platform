import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssessmentSettingsComponent } from './assessment-settings/assessment-settings.component';
import { AssessmentComponent } from '../assessment/assessment.component';
import { AlgorithmService } from '../../services/algorithm.service';
import { AssessmentConfig, AssessmentQuestion } from '../../models/algorithm.models';
import { TEST_SCENARIOS } from '../../data/test-scenarios';

@Component({
  selector: 'app-assessment-container',
  standalone: true,
  imports: [CommonModule, AssessmentSettingsComponent, AssessmentComponent],
  template: `
    @if (phase() === 'settings') {
      <app-assessment-settings
        (start)="onStart($event)"
        [aiAvailable]="aiAvailable()" />
    }
    @if (phase() === 'generating') {
      <div class="flex-1 flex flex-col items-center justify-center p-10">
        <div class="w-10 h-10 border-2 border-blue-400 border-t-transparent rounded-full animate-spin mb-4"></div>
        <p class="text-slate-400">AI 正在生成题目...</p>
        <p class="text-xs text-slate-600 mt-1">通常需要 5-15 秒</p>
      </div>
    }
    @if (phase() === 'quiz') {
      <app-assessment
        [questions]="questions()"
        [isAiMode]="isAiMode()"
        [isGenerating]="false"
        (restart)="onRestart()" />
    }
    @if (phase() === 'error') {
      <div class="flex-1 flex flex-col items-center justify-center p-10">
        <div class="text-4xl mb-4">⚠️</div>
        <h3 class="text-lg font-bold text-red-400 mb-2">AI 出题不可用</h3>
        <p class="text-sm text-slate-400 mb-4 text-center max-w-md">{{ errorMessage() }}</p>
        <div class="flex gap-3">
          <button (click)="fallbackToFixed()"
            class="px-4 py-2 rounded bg-blue-600 hover:bg-blue-500 text-sm font-medium">
            使用固定题目
          </button>
          <button (click)="onRestart()"
            class="px-4 py-2 rounded bg-slate-700 hover:bg-slate-600 text-sm text-slate-300">
            返回设置
          </button>
        </div>
      </div>
    }
  `,
})
export class AssessmentContainerComponent {
  phase = signal<'settings' | 'generating' | 'quiz' | 'error'>('settings');
  questions = signal<AssessmentQuestion[]>([]);
  isAiMode = signal(false);
  isGenerating = signal(false);
  aiAvailable = signal(false);
  errorMessage = signal('');

  constructor(private svc: AlgorithmService) {
    this.checkHealth();
  }

  /** Check if AI assessment API is configured on startup */
  private checkHealth(): void {
    this.svc.checkAssessmentHealth().subscribe({
      next: (res) => {
        this.aiAvailable.set(res.aiAvailable);
      },
      error: () => {
        this.aiAvailable.set(false);
      },
    });
  }

  onStart(config: AssessmentConfig): void {
    if (config.mode === 'fixed') {
      this.isAiMode.set(false);
      this.questions.set(TEST_SCENARIOS as AssessmentQuestion[]);
      this.phase.set('quiz');
      return;
    }

    // AI mode — check health first
    this.isAiMode.set(true);

    if (!this.aiAvailable()) {
      this.errorMessage.set(
        'AI 评估服务的 API Key 未配置。请在 backend/src/main/resources/application.properties 中设置 ai.assessment.api-key，然后重启后端。'
      );
      this.phase.set('error');
      return;
    }

    this.isGenerating.set(true);
    this.phase.set('generating');

    this.svc.generateAssessment(config).subscribe({
      next: (questions) => {
        this.questions.set(questions);
        this.isGenerating.set(false);
        this.phase.set('quiz');
      },
      error: (err) => {
        this.isGenerating.set(false);
        const msg = err?.error?.error || err?.message || 'AI 服务暂时不可用';
        this.errorMessage.set(`出题失败：${msg}`);
        this.phase.set('error');
      },
    });
  }

  fallbackToFixed(): void {
    this.isAiMode.set(false);
    this.questions.set(TEST_SCENARIOS as AssessmentQuestion[]);
    this.phase.set('quiz');
  }

  onRestart(): void {
    this.phase.set('settings');
    this.questions.set([]);
    this.isAiMode.set(false);
    this.errorMessage.set('');
    this.checkHealth(); // Re-check health on return to settings
  }
}
