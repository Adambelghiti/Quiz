import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { GameSummary, LeaderboardEntry, LobbyService } from './lobby.service';
import { CurrentGameService } from './current-game.service';
import { AuthService } from './auth.service';
import { WebSocketService } from './websocket.service';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss'],
  imports: [CommonModule],
  standalone: true,
})
export class LobbyComponent implements OnInit, OnDestroy {
  games: GameSummary[] = [];
  leaderboard: LeaderboardEntry[] = [];
  loading = true;
  live = false;
  error?: string;
  username = '';
  isAdmin = false;

  private unsubscribe?: () => void;
  private connectedSub?: Subscription;

  constructor(
    private lobbyService: LobbyService,
    private router: Router,
    private currentGame: CurrentGameService,
    private auth: AuthService,
    private ws: WebSocketService
  ) {
    this.username = this.auth.getUser()?.username ?? '';
    this.isAdmin = this.auth.isAdmin();
  }

  openAdmin(): void {
    this.router.navigate(['/admin']);
  }

  openProfile(): void {
    this.router.navigate(['/profile']);
  }

  openLeaderboard(): void {
    this.router.navigate(['/leaderboard']);
  }

  ngOnInit(): void {
    this.loadGames();
    this.loadLeaderboard();

    // The server pushes the open-game list whenever a game is created,
    // joined, started or finished.
    this.unsubscribe = this.ws.watch<GameSummary[]>('/topic/lobby', (games) => {
      this.games = games;
      this.loading = false;
    });
    this.connectedSub = this.ws.connected.subscribe((connected) => (this.live = connected));
  }

  ngOnDestroy(): void {
    this.unsubscribe?.();
    this.connectedSub?.unsubscribe();
  }

  loadGames(): void {
    this.lobbyService.getGames().subscribe({
      next: (games) => {
        this.games = games;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Failed to load games';
        this.loading = false;
      },
    });
  }

  loadLeaderboard(): void {
    this.lobbyService.getLeaderboard(5).subscribe({
      next: (entries) => (this.leaderboard = entries),
      error: () => {
        /* the leaderboard is optional; ignore failures */
      },
    });
  }

  refresh(): void {
    this.loadGames();
    this.loadLeaderboard();
  }

  newGame(): void {
    this.router.navigate(['/setup']);
  }

  canJoin(game: GameSummary): boolean {
    return game.status !== 'FINISHED';
  }

  joinGame(game: GameSummary): void {
    // The server resolves the player from the JWT, so no id needs to be sent.
    this.lobbyService.joinGame(game.id).subscribe({
      next: () => {
        this.currentGame.set(game.id);
        // Waiting rooms show who has joined; running games go straight in.
        if (game.status === 'WAITING') {
          this.router.navigate(['/room', game.id]);
        } else {
          this.router.navigate(['/quiz'], { queryParams: { gameId: game.id } });
        }
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Could not join the game.';
      },
    });
  }

  logout(): void {
    this.ws.disconnect();
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
