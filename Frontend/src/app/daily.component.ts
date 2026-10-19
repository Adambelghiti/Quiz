import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DailyChallengeEntry, DailyChallengeService, DailyChallengeStatus } from './daily.service';
import { CurrentGameService } from './current-game.service';
import { PlayerService } from './player.service';
import { FocusHeadingDirective } from './focus-heading.directive';

@Component({
  selector: 'app-daily-challenge',
  templateUrl: './daily.component.html',
  styleUrls: ['./daily.component.scss'],
  imports: [CommonModule, FocusHeadingDirective],
  standalone: true,
})
export class DailyChallengeComponent implements OnInit {
  status: DailyChallengeStatus | null = null;
  loading = true;
  starting = false;
  error: string | null = null;

  constructor(
    private daily: DailyChallengeService,
    private currentGame: CurrentGameService,
    private playerService: PlayerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get dateLabel(): string {
    if (!this.status) {
      return '';
    }
    return new Date(`${this.status.date}T00:00:00Z`).toLocaleDateString(undefined, {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      timeZone: 'UTC',
    });
  }

  get languageLabel(): string {
    const labels: Record<string, string> = {
      EN: 'English',
      FR: 'Français',
      AR: 'العربية',
    };
    return labels[this.status?.language ?? 'EN'] ?? this.status?.language ?? 'English';
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.daily.getStatus().subscribe({
      next: (status) => {
        this.status = status;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Could not load the daily challenge.';
      },
    });
  }

  /** Start a fresh attempt or resume an unfinished one. */
  play(): void {
    if (!this.status || this.starting) {
      return;
    }
    if (this.status.inProgress && this.status.gameId != null) {
      this.resume();
      return;
    }

    this.starting = true;
    this.error = null;
    this.daily.start().subscribe({
      next: (round) => {
        this.starting = false;
        this.currentGame.set(round.gameId);
        if (round.playerId != null) {
          this.playerService.setPlayerId(round.playerId);
        }
        this.router.navigate(['/quiz'], { queryParams: { gameId: round.gameId } });
      },
      error: (err) => {
        this.starting = false;
        this.error = err?.error?.message ?? 'Could not start the daily challenge.';
      },
    });
  }

  resume(): void {
    if (this.status?.gameId == null) {
      return;
    }
    this.currentGame.set(this.status.gameId);
    this.router.navigate(['/quiz'], { queryParams: { gameId: this.status.gameId } });
  }

  isMe(entry: DailyChallengeEntry): boolean {
    return this.status?.playerId === entry.playerId;
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }
}
