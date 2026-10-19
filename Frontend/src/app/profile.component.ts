import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { GameHistoryEntry, PlayerService, PlayerStats } from './player.service';
import { FocusHeadingDirective } from './focus-heading.directive';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  imports: [CommonModule, FocusHeadingDirective],
  standalone: true,
})
export class ProfileComponent implements OnInit {
  stats: PlayerStats | null = null;
  history: GameHistoryEntry[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private player: PlayerService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.player.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Could not load your stats.';
      },
    });

    this.player.getHistory().subscribe({
      next: (history) => (this.history = history),
      error: () => {
        /* history is optional; the stats block already reports errors */
      },
    });
  }

  accuracyPercent(): string {
    if (!this.stats) {
      return '0%';
    }
    return `${Math.round(this.stats.accuracy * 100)}%`;
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }
}
