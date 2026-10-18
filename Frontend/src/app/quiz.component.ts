import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import {
  AnswerResult,
  LobbyService,
  QuestionView,
  RoundResponse,
} from './lobby.service';
import { CurrentGameService } from './current-game.service';
import { PlayerService } from './player.service';
import { WebSocketService } from './websocket.service';

@Component({
  selector: 'app-quiz',
  templateUrl: './quiz.component.html',
  styleUrls: ['./quiz.component.scss'],
  imports: [CommonModule],
  standalone: true,
})
export class QuizComponent implements OnInit, OnDestroy {
  round: RoundResponse | null = null;
  result: AnswerResult | null = null;
  selectedAnswer = '';

  loading = true;
  submitting = false;
  advancing = false;
  error: string | null = null;

  /** Seconds left in the current round, derived from the server clock. */
  timeLeft = 0;

  private gameId: number | null = null;
  private playerId: number | null = null;
  private unsubscribe?: () => void;
  private timer?: ReturnType<typeof setInterval>;
  private lastTick = 0;

  constructor(
    private lobby: LobbyService,
    private route: ActivatedRoute,
    private router: Router,
    private currentGame: CurrentGameService,
    private playerService: PlayerService,
    private ws: WebSocketService
  ) {}

  ngOnInit(): void {
    const fromQuery = Number(this.route.snapshot.queryParamMap.get('gameId'));
    if (fromQuery) {
      this.enterGame(fromQuery);
      return;
    }

    this.currentGame.get().subscribe((id) => {
      if (id === null) {
        this.error = 'No active game. Please start a new one from the lobby.';
        this.loading = false;
        return;
      }
      this.enterGame(id);
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe?.();
    this.clearTimer();
  }

  get question(): QuestionView | null {
    return this.round?.question ?? null;
  }

  get myScore(): number {
    if (!this.round || this.playerId === null) {
      return 0;
    }
    const me = this.round.players.find((p) => p.playerId === this.playerId);
    return me ? me.score : 0;
  }

  get isLastRound(): boolean {
    return !!this.round && this.round.round >= this.round.totalRounds;
  }

  /**
   * Text direction of the question's own content. The application shell stays
   * in English, so only the question card is flipped (Arabic is read right to
   * left).
   */
  get questionDirection(): 'rtl' | 'ltr' {
    return this.question?.language === 'AR' ? 'rtl' : 'ltr';
  }

  /** BCP-47 tag of the question's language, so assistive tech reads it correctly. */
  get questionLang(): string {
    return (this.question?.language ?? 'EN').toLowerCase();
  }

  get iAmHost(): boolean {
    return this.playerId !== null && this.round?.hostPlayerId === this.playerId;
  }

  /** True once the round's time is up on the server clock. */
  get timeExpired(): boolean {
    return this.timeLeft <= 0;
  }

  /** True once every player has answered the current round. */
  get canAdvance(): boolean {
    if (!this.round) {
      return false;
    }
    return this.round.answeredPlayerIds.length >= this.round.players.length;
  }

  hasAnswered(playerId: number): boolean {
    return !!this.round?.answeredPlayerIds.includes(playerId);
  }

  selectAnswer(option: string): void {
    if (!this.result && !this.timeExpired) {
      this.selectedAnswer = option;
    }
  }

  submit(): void {
    if (!this.round?.question || this.gameId === null || this.playerId === null) {
      return;
    }
    if (this.submitting || this.result) {
      return;
    }
    if (!this.selectedAnswer) {
      this.error = 'Please choose an answer first.';
      return;
    }

    this.error = null;
    this.submitting = true;
    this.clearTimer();

    // Elapsed time is measured on the server; the client sends 0.
    this.lobby
      .submitAnswer(this.gameId, this.playerId, this.round.question.id, this.selectedAnswer)
      .subscribe({
        next: (result) => {
          this.result = result;
          this.submitting = false;
          this.markSelfAnswered();
          this.refreshScores();
        },
        error: (err) => {
          this.submitting = false;
          this.error = this.messageFor(err);
          this.startTimer();
        },
      });
  }

  next(): void {
    if (this.gameId === null || this.advancing) {
      return;
    }
    this.advancing = true;
    this.lobby.nextRound(this.gameId, this.playerId ?? undefined).subscribe({
      next: (round) => {
        this.advancing = false;
        if (round.finished) {
          this.goToResults();
          return;
        }
        this.round = round;
        this.resetForQuestion();
      },
      error: (err) => {
        this.advancing = false;
        this.error = this.messageFor(err);
      },
    });
  }

  /** Leave the game and return to the lobby. */
  quit(): void {
    if (this.gameId === null) {
      this.router.navigate(['/lobby']);
      return;
    }
    if (!confirm('Leave the game? Other players will keep playing.')) {
      return;
    }
    this.clearTimer();
    this.lobby.leaveGame(this.gameId).subscribe({
      next: () => this.router.navigate(['/lobby']),
      error: () => this.router.navigate(['/lobby']),
    });
  }

  private enterGame(gameId: number): void {
    if (this.gameId === gameId) {
      return;
    }
    this.gameId = gameId;
    this.currentGame.set(gameId);
    this.playerId = this.playerService.getPlayerId();

    // All game state (new rounds, incoming answers, scores, finish) arrives here.
    this.unsubscribe?.();
    this.unsubscribe = this.ws.watch<RoundResponse>(
      `/topic/game/${gameId}`,
      (state) => this.applyServerState(state)
    );

    this.loadRound();
  }

  private loadRound(): void {
    if (this.gameId === null) {
      return;
    }
    this.loading = true;
    this.lobby.getRound(this.gameId, this.playerId ?? undefined).subscribe({
      next: (round) => {
        this.loading = false;
        // The server tells us which player this response is for; trust it over
        // any locally cached id.
        if (round.playerId != null) {
          this.playerId = round.playerId;
          this.playerService.setPlayerId(round.playerId);
        }
        if (round.status === 'WAITING') {
          this.router.navigate(['/room', round.gameId]);
          return;
        }
        if (round.finished) {
          this.goToResults();
          return;
        }
        this.round = round;
        this.resetForQuestion();
      },
      error: (err) => {
        this.loading = false;
        this.error = this.messageFor(err);
      },
    });
  }

  /**
   * Merge a pushed game state. A changed round number means a brand new
   * question, anything else is just a scoreboard / presence update.
   */
  private applyServerState(state: RoundResponse): void {
    if (state.status === 'WAITING') {
      this.router.navigate(['/room', state.gameId]);
      return;
    }
    if (state.finished) {
      this.goToResults();
      return;
    }
    if (!state.question) {
      return;
    }

    if (!this.round || state.round !== this.round.round) {
      this.round = state;
      this.resetForQuestion();
      return;
    }

    this.round.players = state.players;
    this.round.answeredPlayerIds = state.answeredPlayerIds;
    this.round.status = state.status;
  }

  /** Pull the latest scores so the header stays in sync even without WebSocket. */
  private refreshScores(): void {
    if (this.gameId === null) {
      return;
    }
    this.lobby.getRound(this.gameId, this.playerId ?? undefined).subscribe({
      next: (round) => {
        if (this.round && this.round.round === round.round) {
          this.round.players = round.players;
          this.round.answeredPlayerIds = round.answeredPlayerIds;
        }
      },
      error: () => {
        /* non-critical */
      },
    });
  }

  private markSelfAnswered(): void {
    if (!this.round || this.playerId === null) {
      return;
    }
    if (!this.round.answeredPlayerIds.includes(this.playerId)) {
      this.round.answeredPlayerIds = [...this.round.answeredPlayerIds, this.playerId];
    }
  }

  private resetForQuestion(): void {
    this.result = null;
    this.selectedAnswer = '';
    this.error = null;
    this.startTimer();
  }

  /** Start the per-round countdown using the server-provided start time. */
  private startTimer(): void {
    this.clearTimer();
    this.lastTick = 0;
    this.tick();
    this.timer = setInterval(() => this.tick(), 250);
  }

  private tick(): void {
    const round = this.round;
    if (!round || !round.roundStartedAt || !round.roundTimeoutSeconds) {
      this.timeLeft = 0;
      return;
    }
    const endsAt = round.roundStartedAt + round.roundTimeoutSeconds * 1000;
    const remaining = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000));
    const wasPositive = this.lastTick > 0;
    this.lastTick = remaining;
    this.timeLeft = remaining;

    if (remaining <= 0) {
      this.clearTimer();
      if (wasPositive) {
        this.onTimeUp();
      }
    }
  }

  private onTimeUp(): void {
    if (this.result) {
      return;
    }
    if (this.selectedAnswer) {
      // Auto-submit whatever the player had selected before time ran out.
      this.submit();
    } else {
      this.error = "Time's up! The round will move on automatically.";
    }
  }

  private clearTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  private goToResults(): void {
    if (this.gameId === null) {
      return;
    }
    this.clearTimer();
    this.router.navigate(['/results'], { queryParams: { gameId: this.gameId } });
  }

  private messageFor(err: unknown): string {
    const httpError = err as { error?: { message?: string }; status?: number };
    if (httpError?.error?.message) {
      return httpError.error.message;
    }
    return 'Something went wrong. Please try again.';
  }
}
