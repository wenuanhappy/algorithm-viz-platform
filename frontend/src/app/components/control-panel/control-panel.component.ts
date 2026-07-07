import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlgorithmStore } from '../../store/algorithm.store';

@Component({
  selector: 'app-control-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './control-panel.component.html',
})
export class ControlPanelComponent {
  speedOptions = [
    { label: '0.5×', value: 1000 },
    { label: '1×',   value: 500 },
    { label: '2×',   value: 250 },
    { label: '4×',   value: 125 },
  ];

  constructor(public store: AlgorithmStore) {}

  onSpeedChange(value: number): void {
    this.store.setSpeed(value);
  }

  onSliderChange(event: Event): void {
    const val = +(event.target as HTMLInputElement).value;
    this.store.setCurrentStep(val);
  }
}
