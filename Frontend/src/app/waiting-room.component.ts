import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { QRCodeModule } from 'angularx-qrcode';
import { LobbyService, RoundResponse } from './lobby.service';
import { PlayerService } from './player.service';
import { CurrentGameService } from './current-game.service';
import { WebSocketService } from './websocket.service';

@Component({
  selector: 'app-waiting-room',
  templateUrl: './waiting-room.component.html',
  styleUrls: ['./waiting-room.component.scss'],
  imports: [CommonModule, QRCodeModule],
  standalone: true,
})
export class WaitingRoomComponent implements OnInit, OnDestroy {
  room: RoundResponse | null = null;
  loading = true;
  starting = false;
  error: string | null = null;
  isHost = false;

  /** Absolute link other players can open (or scan) to join this room. */
  inviteUrl = '';
  copied = false;

  private gameId: number | null = null;
  private playerId: number | null = null;
  private unsubscribe?: () => void;
  private poll?: ReturnType<typeof setInterval>;
  private copiedTimer?: ReturnType<typeof setTimeout>;

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
    this.inviteUrl = `${location.origin}/room/${id}`;
    this.playerId = this.playerService.getPlayerId();
    this.currentGame.set(id);

    // Opening an invite link should join the room seamlessly. joinGame is
    // idempotent for players already in the room; if it fails (for example the
    // room has finished) we still load the room so the real error is shown.
    this.lobby.joinGame(id).subscribe({
      next: () => this.enterRoom(id),
      error: () => this.enterRoom(id),
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe?.();
    if (this.poll) {
      clearInterval(this.poll);
    }
    if (this.copiedTimer) {
      clearTimeout(this.copiedTimer);
    }
  }

  /** Subscribe to live updates and fetch the room once we are a participant. */
  private enterRoom(id: number): void {
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

  /** Select the whole link when the field is focused, for easy manual copying. */
  selectInvite(event: Event): void {
    (event.target as HTMLInputElement | null)?.select();
  }

  /** Copy the invite link, falling back to a legacy copy for older browsers. */
  copyInvite(): void {
    if (!this.inviteUrl) {
      return;
    }
    const done = (): void => {
      this.copied = true;
      if (this.copiedTimer) {
        clearTimeout(this.copiedTimer);
      }
      this.copiedTimer = setTimeout(() => (this.copied = false), 2000);
    };

    if (navigator.clipboard?.writeText) {
      navigator.clipboard
        .writeText(this.inviteUrl)
        .then(done)
        .catch(() => this.fallbackCopy(done));
    } else {
      this.fallbackCopy(done);
    }
  }

  private fallbackCopy(done: () => void): void {
    const helper = document.createElement('textarea');
    helper.value = this.inviteUrl;
    helper.setAttribute('readonly', '');
    helper.style.position = 'fixed';
    helper.style.opacity = '0';
    document.body.appendChild(helper);
    helper.select();
    try {
      document.execCommand('copy');
      done();
    } catch {
      /* clipboard access can be blocked; the link stays selectable */
    } finally {
      document.body.removeChild(helper);
    }
  }

  private apply(state: RoundResponse): void {
    this.room = state;
    // The server reports which player this response belongs to.
    if (state.playerId != null) {
      this.playerId = state.playerId;
      this.playerService.setPlayerId(state.playerId);
    }
    this.isHost = this.playerId != null && state.hostPlayerId === this.playerId;

    if (state.status === 'FINISHED') {
      this.router.navigate(['/results'], { queryParams: { gameId: state.gameId } });
      return;
    }
    if (state.status === 'IN_PROGRESS' || state.round > 0) {
      this.router.navigate(['/quiz'], { queryParams: { gameId: state.gameId } });
    }
  }
}
