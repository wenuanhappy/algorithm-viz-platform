import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompetitionStore } from '../../store/competition.store';

@Component({
  selector: 'app-competition',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './competition.component.html',
})
export class CompetitionComponent {
  constructor(public competition: CompetitionStore) {}
}
