import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlgorithmStore } from '../../store/algorithm.store';
import { AlgorithmService } from '../../services/algorithm.service';
import { AssessmentQuestion } from '../../models/algorithm.models';
import { AssessmentChoiceComponent } from './assessment-choice.component';

interface QuestionResult {
  scenarioId: number;
  correct: boolean;
  userAnswer: string;
  explanation: string;
}

@Component({
  selector: 'app-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule, AssessmentChoiceComponent],
  templateUrl: './assessment.component.html',
})
export class AssessmentComponent {
  @Input() questions: AssessmentQuestion[] = [];
  @Input() isAiMode = false;
  @Input() isGenerating = false;
  @Output() restart = new EventEmitter<void>();

  currentIndex = signal(0);
  userInput = signal('');
  results = signal<QuestionResult[]>([]);
  isSubmitted = signal(false);
  isVerifying = signal(false);
  isEvaluating = signal(false);

  currentScenario = computed(() => this.questions[this.currentIndex()] ?? null);
  get totalQuestions(): number { return this.questions.length; }
  answeredCount = computed(() => this.results().length);
  correctCount = computed(() => this.results().filter(r => r.correct).length);
  isComplete = computed(() => this.results().length >= this.totalQuestions && this.totalQuestions > 0);

  currentResult = computed(() => {
    const s = this.currentScenario();
    if (!s) return null;
    return this.results().find(r => r.scenarioId === s.id) ?? null;
  });

  constructor(private store: AlgorithmStore, private svc: AlgorithmService) {}

  prevQuestion(): void {
    if (this.currentIndex() > 0) {
      this.currentIndex.update(i => i - 1);
      this.isSubmitted.set(false);
      this.userInput.set('');
      const existing = this.results().find(r => r.scenarioId === this.currentScenario()?.id);
      if (existing) {
        this.isSubmitted.set(true);
        this.userInput.set(existing.userAnswer);
      }
    }
  }

  nextQuestion(): void {
    if (this.currentIndex() < this.totalQuestions - 1) {
      this.currentIndex.update(i => i + 1);
      this.isSubmitted.set(false);
      this.userInput.set('');
      const existing = this.results().find(r => r.scenarioId === this.currentScenario()?.id);
      if (existing) {
        this.isSubmitted.set(true);
        this.userInput.set(existing.userAnswer);
      }
    }
  }

  submitAnswer(): void {
    const scenario = this.currentScenario();
    if (!scenario || this.isSubmitted()) return;

    const userAnswer = this.userInput().trim();
    if (!userAnswer) return;

    this.isSubmitted.set(true);

    if (this.isAiMode) {
      this.gradeViaAiEval(scenario, userAnswer);
    } else {
      this.gradeLegacy(scenario, userAnswer);
    }
  }

  /** AI evaluation: send question + answer to LLM for semantic grading */
  private gradeViaAiEval(scenario: AssessmentQuestion, userAnswer: string): void {
    this.isEvaluating.set(true);
    this.svc.evaluateAnswer(scenario, userAnswer).subscribe({
      next: (result) => {
        this.isEvaluating.set(false);
        this.addResult(scenario, userAnswer, result.correct, result.feedback);
      },
      error: () => {
        this.isEvaluating.set(false);
        // Fallback to legacy grading on API error
        this.gradeLegacy(scenario, userAnswer);
      },
    });
  }

  /** Legacy grading for fixed questions (fallback as well) */
  private gradeLegacy(scenario: AssessmentQuestion, userAnswer: string): void {
    const questionType = scenario.questionType;

    if (questionType === 'value-fill') {
      this.gradeValueFill(scenario, userAnswer);
    } else if (questionType === 'state-fill' || questionType === 'table-fill') {
      this.gradeViaApi(scenario, userAnswer);
    } else if (questionType === 'path-fill') {
      this.gradePathFill(scenario, userAnswer);
    } else {
      this.gradeChoice(scenario, userAnswer);
    }
  }

  private gradeValueFill(scenario: AssessmentQuestion, userAnswer: string): void {
    const answer = scenario.answer as Record<string, unknown>;
    const userLower = userAnswer.toLowerCase().replace(/\s+/g, '');
    const expectedComparisons = String(answer['comparisons'] ?? '');

    let correct = false;
    if (scenario.id === 3) {
      const mids = (answer['mids'] as number[]).map(String);
      const hasComparisonCount = userLower.includes(expectedComparisons) ||
        userLower.includes(String(expectedComparisons));
      const hasMids = mids.every(m => userLower.includes(m));
      correct = hasComparisonCount && hasMids;
    } else {
      correct = userLower.includes(expectedComparisons);
    }

    this.addResult(scenario, userAnswer, correct);
  }

  private gradePathFill(scenario: AssessmentQuestion, userAnswer: string): void {
    const answer = scenario.answer as Record<string, unknown>;
    const expectedPath = String(answer['path']).replace(/\s+/g, '').toLowerCase();
    const expectedDist = String(answer['distance'] ?? '');
    const userClean = userAnswer.replace(/\s+/g, '').toLowerCase();

    const pathCorrect = userClean.includes(expectedPath) ||
      userClean.includes(expectedPath.replace(/→/g, ''));
    const distCorrect = userClean.includes(expectedDist);

    this.addResult(scenario, userAnswer, pathCorrect && distCorrect);
  }

  private gradeChoice(scenario: AssessmentQuestion, userAnswer: string): void {
    const userLetter = this.extractOptionLetter(userAnswer);
    const correctLetter = this.extractOptionLetter(this.answerAsString(scenario.answer));
    const correct = userLetter !== '' && userLetter === correctLetter;
    this.addResult(scenario, userAnswer, correct);
  }

  /** Extract option letter (A/B/C/D) from various formats: "A", "A. xxx", "A) xxx" */
  private extractOptionLetter(text: string): string {
    const match = text.trim().match(/^([A-D])[.)]\s/);
    if (match) return match[1]!;
    if (/^[A-D]$/.test(text.trim())) return text.trim();
    return '';
  }

  /** Convert unknown answer type to string for comparison */
  private answerAsString(answer: unknown): string {
    if (typeof answer === 'string') return answer;
    if (answer && typeof answer === 'object') {
      const obj = answer as Record<string, unknown>;
      return String(obj['option'] ?? obj['answer'] ?? obj['text'] ?? '');
    }
    return String(answer ?? '');
  }

  private gradeViaApi(scenario: AssessmentQuestion, userAnswer: string): void {
    if (scenario.targetStepIndex == null || !scenario.verifyField) {
      const answerStr = JSON.stringify(scenario.answer);
      const correct = userAnswer.replace(/\s+/g, '') === answerStr.replace(/\s+/g, '');
      this.addResult(scenario, userAnswer, correct);
      return;
    }

    this.isVerifying.set(true);
    this.svc.verifyStep(scenario.algorithm, scenario.inputParams, scenario.targetStepIndex).subscribe({
      next: (res) => {
        this.isVerifying.set(false);
        if (!res?.stepData) {
          const answerStr = JSON.stringify(scenario.answer);
          const correct = userAnswer.replace(/\s+/g, '').includes(
            answerStr.replace(/\s+/g, '').substring(0, 20));
          this.addResult(scenario, userAnswer, correct);
          return;
        }

        const stepData = res.stepData as Record<string, unknown>;
        const verifyField = scenario.verifyField!;
        const expectedValue = stepData[verifyField];
        const expectedStr = JSON.stringify(expectedValue).replace(/\s+/g, '');
        const userClean = userAnswer.replace(/\s+/g, '');
        let correct = userClean.includes(expectedStr.substring(0, Math.min(expectedStr.length, 30)));

        if (scenario.questionType === 'table-fill') {
          const answer = scenario.answer as Record<string, unknown>;
          const dpValue = String(answer['dpValue'] ?? '');
          correct = userClean.includes(dpValue);
        }

        this.addResult(scenario, userAnswer, correct);
      },
      error: () => {
        this.isVerifying.set(false);
        const answerStr = JSON.stringify(scenario.answer);
        const correct = userAnswer.replace(/\s+/g, '').includes(
          answerStr.replace(/\s+/g, '').substring(0, 20));
        this.addResult(scenario, userAnswer, correct);
      },
    });
  }

  private addResult(scenario: AssessmentQuestion, userAnswer: string, correct: boolean, explanation?: string): void {
    const expl = explanation ?? scenario.explanation ?? '';
    const existing = this.results().find(r => r.scenarioId === scenario.id);
    if (existing) {
      this.results.update(list => list.map(r =>
        r.scenarioId === scenario.id
          ? { ...r, correct, userAnswer, explanation: expl }
          : r
      ));
    } else {
      this.results.update(list => [...list, {
        scenarioId: scenario.id,
        correct,
        userAnswer,
        explanation: expl,
      }]);
    }
  }

  restartTest(): void {
    this.restart.emit();
  }

  isChoiceType(s: AssessmentQuestion): boolean {
    return s.questionType === 'choice' && !!s.options?.length;
  }

  isShortAnswerType(s: AssessmentQuestion): boolean {
    return s.questionType === 'value-fill' && s.options == null;
  }

  /** Select an option for choice-type questions */
  selectOption(opt: string): void {
    this.userInput.set(opt);
  }

  placeholderText(): string {
    const s = this.currentScenario();
    if (!s) return '输入你的答案...';
    switch (s.questionType) {
      case 'value-fill': return '输入数值或说明...';
      case 'state-fill': return '输入数组状态（如 [3,1,2,5,8,6]）或描述...';
      case 'path-fill': return '输入路径和距离（如 A→D→E→F, 距离10）...';
      case 'table-fill': return '输入 DP 表格中指定位置的值...';
      case 'choice': return '请从下方选项中选择...';
      default: return '输入你的答案...';
    }
  }

  getResultClass(scenarioId: number): string {
    const r = this.results().find(x => x.scenarioId === scenarioId);
    if (!r) return 'bg-slate-700 text-slate-500';
    if (scenarioId === this.currentScenario()?.id) {
      return r.correct ? 'bg-green-600 text-white ring-2 ring-green-400' : 'bg-red-600 text-white ring-2 ring-red-400';
    }
    return r.correct ? 'bg-green-600 text-white' : 'bg-red-600 text-white';
  }

  getDotLabel(scenarioId: number): string {
    const r = this.results().find(x => x.scenarioId === scenarioId);
    if (!r) return String(scenarioId);
    return r.correct ? '✓' : '✗';
  }

  formatParams(params: Record<string, unknown>): string {
    if (params['array']) {
      return `array: [${(params['array'] as number[]).join(', ')}]${params['target'] != null ? ', target: ' + params['target'] : ''}`;
    }
    if (params['items']) {
      const items = params['items'] as Array<Record<string, unknown>>;
      return `items: ${items.map(i => `${i['name']}(w:${i['weight']},v:${i['value']})`).join(', ')}, capacity: ${params['capacity']}`;
    }
    if (params['graph']) return '图数据（见题目描述）';
    return JSON.stringify(params);
  }

  formatAnswer(answer: unknown): string {
    if (typeof answer === 'string') {
      // Try to resolve choice answer against current scenario's options
      const s = this.currentScenario();
      if (s?.options) {
        const matched = s.options.find(o => o.startsWith(answer) || o === answer);
        if (matched) return matched;
      }
      return answer;
    }
    if (answer && typeof answer === 'object') {
      const obj = answer as Record<string, unknown>;
      const s = this.currentScenario();
      const letter = String(obj['option'] ?? obj['answer'] ?? '');
      if (s?.options && letter) {
        const matched = s.options.find(o => this.extractOptionLetter(o) === letter);
        if (matched) return matched;
      }
      return JSON.stringify(answer, null, 2);
    }
    return JSON.stringify(answer);
  }
}
