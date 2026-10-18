import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameResults, LobbyService } from './lobby.service';
import { CurrentGameService } from './current-game.service';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss'],
  imports: [CommonModule],
  standalone: true,
})
export class ResultsComponent implements OnInit {
  results: GameResults | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private lobby: LobbyService,
    private route: ActivatedRoute,
    private router: Router,
    private currentGame: CurrentGameService
  ) {}

  ngOnInit(): void {
    const fromQuery = this.route.snapshot.queryParamMap.get('gameId');
    const gameId = fromQuery ? Number(fromQuery) : this.currentGame.getCurrentId();
    if (!gameId) {
      this.error = 'No game selected.';
      this.loading = false;
      return;
    }
    this.lobby.getResults(gameId).subscribe({
      next: (results) => {
        this.results = results;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Could not load the results.';
        this.loading = false;
      },
    });
  }

  isWinner(playerId: number): boolean {
    return this.results?.winnerId === playerId;
  }

  playAgain(): void {
    this.router.navigate(['/setup']);
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }
}
