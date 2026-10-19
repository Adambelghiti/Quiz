import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LeaderboardEntry, LobbyService } from './lobby.service';
import { FocusHeadingDirective } from './focus-heading.directive';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss'],
  imports: [CommonModule, FormsModule, FocusHeadingDirective],
  standalone: true,
})
export class LeaderboardComponent implements OnInit {
  entries: LeaderboardEntry[] = [];
  limit = 20;
  loading = true;
  error: string | null = null;

  constructor(private lobby: LobbyService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.lobby.getLeaderboard(this.limit).subscribe({
      next: (entries) => {
        this.entries = entries;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Could not load the leaderboard.';
      },
    });
  }

  accuracy(entry: LeaderboardEntry): string {
    return `${Math.round((entry.accuracy ?? 0) * 100)}%`;
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }
}
