import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LobbyService, RoundResponse } from './lobby.service';
import { PlayerService } from './player.service';
import { CurrentGameService } from './current-game.service';
import { WebSocketService } from './websocket.service';

@Component({
  selector: 'app-waiting-room',
  templateUrl: './waiting-room.component.html',
  styleUrls: ['./waiting-room.component.scss'],
  imports: [CommonModule],
  standalone: true,
})
export class WaitingRoomComponent implements OnInit, OnDestroy {
  room: RoundResponse | null = null;
  loading = true;
  starting = false;
  error: string | null = null;
  isHost = false;

  private gameId: number | null = null;
  private playerId: number | null = null;
  private unsubscribe?: () => void;
  private poll?: ReturnType<typeof setInterval>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private lobby: LobbyService,
    private playerService: PlayerService,
    private currentGame: CurrentGameService,
    private ws: WebSocketService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Invalid room.';
      this.loading = false;
      return;
    }
    this.gameId = id;
    this.playerId = this.playerService.getPlayerId();
    this.currentGame.set(id);

    // Live player list while we wait for the host to start.
    this.unsubscribe = this.ws.watch<RoundResponse>(`/topic/game/${id}`, (state) =>
      this.apply(state)
    );

    this.lobby.getRound(id, this.playerId ?? undefined).subscribe({
      next: (state) => {
        this.loading = false;
        this.apply(state);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Room not found.';
      },
    });

    // Fallback for when the WebSocket is unavailable: poll the room so we never
    // miss the host starting the game.
    this.poll = setInterval(() => this.pollRoom(), 3000);
  }

  ngOnDestroy(): void {
    this.unsubscribe?.();
    if (this.poll) {
      clearInterval(this.poll);
    }
  }

  private pollRoom(): void {
    if (this.gameId === null || this.room?.status === 'IN_PROGRESS') {
      return;
    }
    this.lobby.getRound(this.gameId, this.playerId ?? undefined).subscribe({
      next: (state) => this.apply(state),
      error: () => {
        /* transient; the WebSocket may still deliver the update */
      },
    });
  }

  get canStart(): boolean {
    return this.isHost && !this.starting;
  }

  start(): void {
    if (!this.gameId || this.playerId == null || this.starting) {
      return;
    }
    this.starting = true;
    this.lobby.startRoom(this.gameId, this.playerId).subscribe({
      next: () => this.router.navigate(['/quiz'], { queryParams: { gameId: this.gameId } }),
      error: (err) => {
        this.starting = false;
        this.error = err?.error?.message ?? 'Could not start the game.';
      },
    });
  }

  /** Leave the room; the server removes this player (and closes it if empty). */
  leave(): void {
    if (this.gameId === null) {
      this.router.navigate(['/lobby']);
      return;
    }
    this.lobby.leaveGame(this.gameId).subscribe({
      next: () => this.router.navigate(['/lobby']),
      error: () => this.router.navigate(['/lobby']),
    });
  }

  /** Host-only: cancel the room and send everyone back to the lobby. */
  cancel(): void {
    if (this.gameId === null || !this.isHost) {
      return;
    }
    if (!confirm('Cancel this room? Other players will be sent back to the lobby.')) {
      return;
    }
    this.lobby.cancelRoom(this.gameId).subscribe({
      next: () => this.router.navigate(['/lobby']),
      error: (err) => {
        this.error = err?.error?.message ?? 'Could not cancel the room.';
      },
    });
  }

  private apply(state: RoundResponse): void {
    this.room = state;
    // The server reports which player this response belongs to.
    if (state.playerId != null) {
      this.playerId = state.playerId;
      this.playerService.setPlayerId(state.playerId);
    }
    this.isHost = this.playerId != null && state.hostPlayerId === this.playerId;

    if (state.status === 'IN_PROGRESS' || state.round > 0) {
      this.router.navigate(['/quiz'], { queryParams: { gameId: state.gameId } });
    }
  }
}
