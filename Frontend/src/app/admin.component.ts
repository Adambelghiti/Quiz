import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminQuestion, QuestionAdminService } from './question-admin.service';
import { FocusHeadingDirective } from './focus-heading.directive';

/** Fixed enums mirrored from the backend (see Category / Language). */
const CATEGORIES = [
  'MOROCCAN_HISTORY',
  'MOROCCAN_CULTURE',
  'ARABIC_CULTURE',
  'ISLAMIC_CULTURE',
  'GEOGRAPHY',
  'SPORTS',
  'MUSIC',
  'SCIENCE',
  'GENERAL_KNOWLEDGE',
];
const LANGUAGES = ['EN', 'FR', 'AR'];

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
  imports: [CommonModule, FormsModule, FocusHeadingDirective],
  standalone: true,
})
export class AdminComponent implements OnInit {
  questions: AdminQuestion[] = [];
  categories = CATEGORIES;
  languages = LANGUAGES;

  loading = true;
  saving = false;
  error: string | null = null;
  notice: string | null = null;

  /** Question currently being edited (or a blank one for a new question). */
  draft: AdminQuestion = this.blankQuestion();
  editingId: number | null = null;

  constructor(private admin: QuestionAdminService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.admin.list().subscribe({
      next: (questions) => {
        this.questions = questions;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Could not load questions.';
      },
    });
  }

  edit(question: AdminQuestion): void {
    this.editingId = question.id ?? null;
    this.draft = { ...question, options: [...question.options] };
    this.notice = null;
    this.error = null;
  }

  newQuestion(): void {
    this.editingId = null;
    this.draft = this.blankQuestion();
    this.notice = null;
    this.error = null;
  }

  save(): void {
    if (this.saving) {
      return;
    }
    const validation = this.validate(this.draft);
    if (validation) {
      this.error = validation;
      return;
    }
    this.saving = true;
    this.error = null;
    this.notice = null;

    const request$ =
      this.editingId != null
        ? this.admin.update(this.editingId, this.draft)
        : this.admin.create(this.draft);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.notice = this.editingId != null ? 'Question updated.' : 'Question created.';
        this.newQuestion();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message ?? 'Could not save the question.';
      },
    });
  }

  remove(question: AdminQuestion): void {
    if (question.id == null) {
      return;
    }
    if (!confirm('Delete this question?')) {
      return;
    }
    this.admin.remove(question.id).subscribe({
      next: () => {
        this.notice = 'Question deleted.';
        if (this.editingId === question.id) {
          this.newQuestion();
        }
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Could not delete the question.';
      },
    });
  }

  trackOption(index: number): number {
    return index;
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }

  private validate(question: AdminQuestion): string | null {
    if (!question.text?.trim()) {
      return 'Question text is required.';
    }
    const options = (question.options ?? []).map((o) => o?.trim()).filter((o) => !!o);
    if (options.length < 2) {
      return 'At least two answer options are required.';
    }
    if (!question.correctAnswer?.trim()) {
      return 'A correct answer is required.';
    }
    if (!options.includes(question.correctAnswer.trim())) {
      return 'The correct answer must match one of the options.';
    }
    return null;
  }

  private blankQuestion(): AdminQuestion {
    return {
      text: '',
      category: CATEGORIES[0],
      difficulty: 1,
      language: 'EN',
      correctAnswer: '',
      options: ['', '', '', ''],
      hint: '',
    };
  }
}
