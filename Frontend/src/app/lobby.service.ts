import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/** A question as sent to the client during a game (no correct answer). */
export interface QuestionView {
  id: number;
  text: string;
  hint?: string;
  options: string[];
  category?: string;
  difficulty: number;
  language?: string;
}

/** Lightweight game info shown in the lobby. */
export interface GameSummary {
  id: number;
  status: string;
  currentRound: number;
  totalRounds: number;
  playerCount: number;
}

export interface PlayerScore {
  playerId: number;
  name: string;
  score: number;
  correctAnswers: number;
}

/** State of a game plus the question for the current round. */
export interface RoundResponse {
  gameId: number;
  status: string;
  round: number;
  totalRounds: number;
  playerId?: number;
  hostPlayerId?: number;
  question?: QuestionView;
  players: PlayerScore[];
  answeredPlayerIds: number[];
  finished: boolean;
  /** Epoch millis when the round started (server clock); 0 when no round is active. */
  roundStartedAt?: number;
  /** Seconds allowed for the current round before it auto-advances. */
  roundTimeoutSeconds?: number;
}

/** Outcome of a single answer. */
export interface AnswerResult {
  correct: boolean;
  correctAnswer: string;
  scoreDelta: number;
  totalScore: number;
  correctAnswers: number;
  round: number;
  totalRounds: number;
  roundComplete: boolean;
}

/** Final standings of a game. */
export interface GameResults {
  gameId: number;
  status: string;
  winnerId?: number | null;
  winnerName?: string | null;
  totalRounds: number;
  scores: PlayerScore[];
}

export interface LeaderboardEntry {
  playerId: number;
  name: string;
  totalScore: number;
  gamesPlayed?: number;
  wins?: number;
  correctAnswers?: number;
  totalRounds?: number;
  accuracy?: number;
}

export interface StartGameRequest {
  category?: string | null;
  difficulty?: number | null;
  language?: string | null;
  rounds?: number;
  playerId?: number | null;
  playerName?: string;
}

@Injectable({ providedIn: 'root' })
export class LobbyService {
  private apiUrl = `${environment.apiBaseUrl}/games`;
  private leaderboardUrl = `${environment.apiBaseUrl}/leaderboard`;

  constructor(private http: HttpClient) {}

  /** List games for the lobby. */
  getGames(): Observable<GameSummary[]> {
    return this.http.get<GameSummary[]>(this.apiUrl);
  }

  /** Create a configured game and receive its first round. */
  startGame(request: StartGameRequest): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/start`, request);
  }

  /** Create a multiplayer room that waits for the host to start it. */
  createRoom(request: StartGameRequest): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/create`, request);
  }

  /** Host-only: start a waiting room. */
  startRoom(gameId: number, playerId: number): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/${gameId}/start`, {
      playerId,
    });
  }

  /** Join an existing game; the server identifies the player from the JWT. */
  joinGame(gameId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${gameId}/join`, {});
  }

  /** Leave the current game; closes the room if it becomes empty. */
  leaveGame(gameId: number): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/${gameId}/leave`, {});
  }

  /** Host-only: remove another player from the game. */
  kickPlayer(gameId: number, playerId: number): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/${gameId}/kick/${playerId}`, {});
  }

  /** Host-only: cancel (delete) a room that has not started yet. */
  cancelRoom(gameId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${gameId}`);
  }

  /** Fetch the current round (question + scores). */
  getRound(gameId: number, playerId?: number): Observable<RoundResponse> {
    const url = `${this.apiUrl}/${gameId}/round`;
    if (playerId != null) {
      return this.http.get<RoundResponse>(url, { params: { playerId: String(playerId) } });
    }
    return this.http.get<RoundResponse>(url);
  }

  /** Submit an answer for the current round. */
  submitAnswer(
    gameId: number,
    playerId: number,
    questionId: number,
    answer: string
  ): Observable<AnswerResult> {
    return this.http.post<AnswerResult>(`${this.apiUrl}/${gameId}/answer`, {
      playerId,
      questionId,
      answer,
    });
  }

  /** Advance to the next round (or finish the game after the last one). */
  nextRound(gameId: number, playerId?: number): Observable<RoundResponse> {
    const url = `${this.apiUrl}/${gameId}/next-round`;
    if (playerId != null) {
      return this.http.post<RoundResponse>(url, {}, { params: { playerId: String(playerId) } });
    }
    return this.http.post<RoundResponse>(url, {});
  }

  /** Finish a game now. */
  finishGame(gameId: number): Observable<GameResults> {
    return this.http.post<GameResults>(`${this.apiUrl}/${gameId}/finish`, {});
  }

  /** Final standings of a game. */
  getResults(gameId: number): Observable<GameResults> {
    return this.http.get<GameResults>(`${this.apiUrl}/${gameId}/results`);
  }

  /** Top players across finished games. */
  getLeaderboard(limit = 10): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(this.leaderboardUrl, {
      params: { limit: String(limit) },
    });
  }
}
