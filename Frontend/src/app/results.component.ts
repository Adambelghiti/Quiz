import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameResults, LobbyService, RoundResponse } from './lobby.service';
import { CurrentGameService } from './current-game.service';
import { EffectsService } from './effects.service';
import { PlayerService } from './player.service';
import { WebSocketService } from './websocket.service';
import { FocusHeadingDirective } from './focus-heading.directive';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss'],
  imports: [CommonModule, FocusHeadingDirective],
  standalone: true,
})
export class ResultsComponent implements OnInit, OnDestroy {
  results: GameResults | null = null;
  loading = true;
  rematching = false;
  error: string | null = null;

  private gameId: number | null = null;
  private unsubscribe?: () => void;
  private leaving = false;

  constructor(
    private lobby: LobbyService,
    private route: ActivatedRoute,
    private router: Router,
    private currentGame: CurrentGameService,
    private ws: WebSocketService,
    private playerService: PlayerService,
    public effects: EffectsService
  ) {}

  ngOnInit(): void {
    const fromQuery = this.route.snapshot.queryParamMap.get('gameId');
    const gameId = fromQuery ? Number(fromQuery) : this.currentGame.getCurrentId();
    if (!gameId) {
      this.error = 'No game selected.';
      this.loading = false;
      return;
    }
    this.gameId = gameId;
    this.loadResults(gameId);

    // Follow the room so every player moves together when someone starts a
    // rematch (the same game id is reused).
    this.unsubscribe = this.ws.watch<RoundResponse>(`/topic/game/${gameId}`, (state) =>
      this.onRoomState(state)
    );
  }

  ngOnDestroy(): void {
    this.unsubscribe?.();
  }

  isWinner(playerId: number): boolean {
    return this.results?.winnerId === playerId;
  }

  /**
   * Restart the same room with the same players and settings. The server keeps
   * the game id, so the invite link stays valid; a multiplayer room returns to
   * the waiting room while a solo game starts immediately.
   */
  rematch(): void {
    if (this.gameId === null || this.rematching || this.leaving) {
      return;
    }
    if (this.results?.dailyChallenge) {
      this.error = 'Daily challenges cannot be replayed today.';
      return;
    }
    this.rematching = true;
    this.error = null;
    this.lobby.rematch(this.gameId).subscribe({
      next: (round) => this.followRoom(round),
      error: (err) => {
        this.rematching = false;
        this.error = err?.error?.message ?? 'Could not start the rematch.';
      },
    });
  }

  /** Leave this room's settings behind and configure a brand new game. */
  newGame(): void {
    this.router.navigate(['/setup']);
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }

  backToDaily(): void {
    this.router.navigate(['/daily']);
  }

  private loadResults(gameId: number): void {
    this.lobby.getResults(gameId).subscribe({
      next: (results) => {
        this.results = results;
        this.loading = false;
        this.celebrate(results);
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Could not load the results.';
        this.loading = false;
      },
    });
  }

  /** Confetti for the winner, with an extra fanfare when the local player won. */
  private celebrate(results: GameResults): void {
    if (results.winnerId == null || results.practice) {
      return;
    }
    const me = this.playerService.getPlayerId();
    if (me !== null && results.winnerId === me) {
      this.effects.celebrate();
    } else {
      this.effects.confetti();
    }
  }

  /**
   * A rematch resets the shared room, so the live state leaves {@code FINISHED}.
   * Everyone watching is sent to the room (multiplayer) or the quiz (solo).
   */
  private onRoomState(state: RoundResponse): void {
    if (!state || state.status === 'FINISHED') {
      return;
    }
    this.followRoom(state);
  }

  private followRoom(round: RoundResponse): void {
    if (this.leaving) {
      return;
    }
    this.leaving = true;
    if (round.status === 'WAITING') {
      this.router.navigate(['/room', round.gameId]);
    } else {
      this.router.navigate(['/quiz'], { queryParams: { gameId: round.gameId } });
    }
  }
}
