import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LobbyService, StartGameRequest } from './lobby.service';
import { CurrentGameService } from './current-game.service';
import { PlayerService } from './player.service';
import { QuizService } from './quiz.service';
import { FocusHeadingDirective } from './focus-heading.directive';

interface DifficultyOption {
  value: number | null;
  label: string;
}

interface LanguageOption {
  value: string | null;
  label: string;
}

/**
 * "Any" language means no filter at all, so a single game can deal questions
 * from several languages. The label says so, because a mixed-language game
 * otherwise looks like a bug.
 */
const ANY_LANGUAGE_LABEL = 'Any language (questions may mix languages)';

@Component({
  selector: 'app-game-setup',
  templateUrl: './game-setup.component.html',
  styleUrls: ['./game-setup.component.scss'],
  imports: [CommonModule, FormsModule, FocusHeadingDirective],
  standalone: true,
})
export class GameSetupComponent implements OnInit {
  categories: string[] = [];
  difficulties: DifficultyOption[] = [{ value: null, label: 'Any difficulty' }];
  languages: LanguageOption[] = [{ value: null, label: ANY_LANGUAGE_LABEL }];

  category: string | null = null;
  difficulty: number | null = null;
  language: string | null = null;
  rounds = 5;
  roundSeconds = 30;

  readonly roundTimeOptions = [15, 30, 45, 60, 90, 120];

  loading = false;
  errorMsg: string | null = null;

  private static readonly difficultyLabels: Record<number, string> = {
    1: 'Easy',
    2: 'Medium',
    3: 'Hard',
  };

  private static readonly languageLabels: Record<string, string> = {
    EN: 'English',
    FR: 'Français',
    AR: 'العربية',
  };

  constructor(
    private lobby: LobbyService,
    private currentGame: CurrentGameService,
    private router: Router,
    private quiz: QuizService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    this.quiz.getFacets().subscribe({
      next: (facets) => {
        this.categories = Array.from(new Set(facets.categories ?? []));
        this.difficulties = [
          { value: null, label: 'Any difficulty' },
          ...(facets.difficulties ?? []).map((d) => ({
            value: d,
            label: GameSetupComponent.difficultyLabels[d] ?? `Level ${d}`,
          })),
        ];
        this.languages = [
          { value: null, label: ANY_LANGUAGE_LABEL },
          ...(facets.languages ?? []).map((l) => ({
            value: l,
            label: GameSetupComponent.languageLabels[l] ?? l,
          })),
        ];
      },
      error: () => {
        this.errorMsg = 'Could not load the available options – you can still start a game.';
      },
    });
  }

  /** Create a multiplayer room and wait for other players. */
  createRoom(): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.errorMsg = null;

    this.lobby.createRoom(this.request()).subscribe({
      next: (room) => {
        this.currentGame.set(room.gameId);
        if (room.playerId != null) {
          this.playerService.setPlayerId(room.playerId);
        }
        this.router.navigate(['/room', room.gameId]);
      },
      error: (err) => this.fail(err),
    });
  }

  /** Start a game immediately with just this player. */
  playSolo(): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.errorMsg = null;

    this.lobby.startGame(this.request()).subscribe({
      next: (round) => {
        this.currentGame.set(round.gameId);
        if (round.playerId != null) {
          this.playerService.setPlayerId(round.playerId);
        }
        this.router.navigate(['/quiz'], { queryParams: { gameId: round.gameId } });
      },
      error: (err) => this.fail(err),
    });
  }

  cancel(): void {
    this.router.navigate(['/lobby']);
  }

  private request(): StartGameRequest {
    // Identity is resolved server-side from the JWT; only game options are sent.
    return {
      category: this.category,
      difficulty: this.difficulty,
      language: this.language,
      rounds: Number(this.rounds) || 5,
      roundTimeoutSeconds: Number(this.roundSeconds) || 30,
    };
  }

  private fail(err: unknown): void {
    this.loading = false;
    const httpError = err as { error?: { message?: string } };
    this.errorMsg = httpError?.error?.message ?? 'Could not create the game. Please try again.';
  }
}
