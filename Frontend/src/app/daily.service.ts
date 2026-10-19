import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoundResponse } from './lobby.service';
import { environment } from '../environments/environment';

/** One row of today's daily-challenge leaderboard. */
export interface DailyChallengeEntry {
  rank: number;
  playerId: number;
  name: string;
  score: number;
  correctAnswers: number;
  totalRounds: number;
}

/** Today's challenge, the caller's attempt, and the leaderboard. */
export interface DailyChallengeStatus {
  date: string;
  playerId: number;
  totalRounds: number;
  roundTimeoutSeconds: number;
  language: string;
  inProgress: boolean;
  completed: boolean;
  gameId?: number;
  score?: number;
  correctAnswers?: number;
  rank?: number;
  leaderboard: DailyChallengeEntry[];
}

/** The once-per-day shared quiz. */
@Injectable({ providedIn: 'root' })
export class DailyChallengeService {
  private apiUrl = `${environment.apiBaseUrl}/daily`;

  constructor(private http: HttpClient) {}

  /** Today's status, including the caller's rank and the daily leaderboard. */
  getStatus(): Observable<DailyChallengeStatus> {
    return this.http.get<DailyChallengeStatus>(this.apiUrl);
  }

  /** Start a fresh attempt, or resume the unfinished one. */
  start(): Observable<RoundResponse> {
    return this.http.post<RoundResponse>(`${this.apiUrl}/start`, {});
  }
}
