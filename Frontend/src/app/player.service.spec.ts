import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlayerService } from './player.service';
import { environment } from '../environments/environment';

describe('PlayerService', () => {
  let service: PlayerService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PlayerService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('resolves the current player from the server and caches the id', () => {
    let resolvedId: number | undefined;
    service.me().subscribe((player) => (resolvedId = player.id));

    http.expectOne(`${environment.apiBaseUrl}/players/me`).flush({ id: 7, name: 'demo' });

    expect(resolvedId).toBe(7);
    expect(service.getPlayerId()).toBe(7);
  });

  it('fetches history and stats for the current user', () => {
    service.getHistory().subscribe((history) => expect(history.length).toBe(1));
    http.expectOne(`${environment.apiBaseUrl}/players/me/history`).flush([
      {
        gameId: 1,
        finalScore: 320,
        rank: 1,
        correctAnswers: 4,
        totalRounds: 5,
        playedAt: '2026-01-01T10:00:00',
      },
    ]);

    service.getStats().subscribe((stats) => expect(stats.wins).toBe(1));
    http.expectOne(`${environment.apiBaseUrl}/players/me/stats`).flush({
      playerId: 7,
      name: 'demo',
      gamesPlayed: 1,
      wins: 1,
      totalScore: 320,
      bestScore: 320,
      correctAnswers: 4,
      totalRounds: 5,
      accuracy: 0.8,
    });
  });
});
