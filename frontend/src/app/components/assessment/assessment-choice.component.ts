import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssessmentQuestion } from '../../models/algorithm.models';

@Component({
  selector: 'app-assessment-choice',
  standalone: true,
  imports: [CommonModule],
  template: `
    <label class="block text-xs text-slate-400 mb-2">请选择一个选项：</label>
    <div class="space-y-2 mb-3">
      <span *ngFor="let opt of question.options" (click)="selectOption(opt)"
        class="w-full text-left px-4 py-2.5 rounded-lg border transition-all text-sm cursor-pointer select-none block"
        [class.bg-blue-600]="selected === opt"
        [class.border-blue-500]="selected === opt"
        [class.text-blue-200]="selected === opt"
        [class.bg-slate-900]="selected !== opt"
        [class.border-slate-700]="selected !== opt"
        [class.text-slate-300]="selected !== opt"
        [class.hover:border-slate-500]="selected !== opt"
        role="button">
        {{ opt }}
      </span>
    </div>
  `,
})
export class AssessmentChoiceComponent {
  @Input() question!: AssessmentQuestion;
  @Input() selected = '';

  @Output() selectedChange = new EventEmitter<string>();

  selectOption(opt: string): void {
    this.selected = opt;
    this.selectedChange.emit(opt);
  }
}
