import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

export interface Player {
  id: number;
  name: string;
}

/** One finished game in the current player's history. */
export interface GameHistoryEntry {
  gameId: number;
  finalScore: number;
  rank: number;
  correctAnswers: number;
  totalRounds: number;
  playedAt: string;
}

/** Aggregate statistics for the current player. */
export interface PlayerStats {
  playerId: number;
  name: string;
  gamesPlayed: number;
  wins: number;
  totalScore: number;
  bestScore: number;
  correctAnswers: number;
  totalRounds: number;
  accuracy: number;
}

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private apiUrl = `${environment.apiBaseUrl}/players`;
  private readonly storageKey = 'playerId';

  constructor(private http: HttpClient) {}

  /**
   * The in-game player bound to the authenticated user. The server resolves
   * (and creates on first use) this player from the JWT, so it is always the
   * correct identity regardless of what is cached locally.
   */
  me(): Observable<Player> {
    return this.http
      .get<Player>(`${this.apiUrl}/me`)
      .pipe(tap((player) => this.setPlayerId(player.id)));
  }

  /** Finished games for the current user, newest first. */
  getHistory(): Observable<GameHistoryEntry[]> {
    return this.http.get<GameHistoryEntry[]>(`${this.apiUrl}/me/history`);
  }

  /** Aggregate statistics for the current user. */
  getStats(): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(`${this.apiUrl}/me/stats`);
  }

  /** Create a new player and return the created entity (admin/legacy use). */
  createPlayer(name: string): Observable<Player> {
    return this.http.post<Player>(this.apiUrl, { name });
  }

  /** The id of the locally remembered player, if any. Cache only. */
  getPlayerId(): number | null {
    const stored = localStorage.getItem(this.storageKey);
    return stored ? Number(stored) : null;
  }

  setPlayerId(id: number): void {
    localStorage.setItem(this.storageKey, String(id));
  }

  clearPlayerId(): void {
    localStorage.removeItem(this.storageKey);
  }

  /**
   * Resolve the current player from the server. Always authoritative: the local
   * id is only a cache and is never trusted, so a stale value after a database
   * reset self-heals on the next call.
   */
  resolveMe(): Observable<Player> {
    return this.me();
  }
}
