import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DailyChallengeService } from './daily.service';
import { environment } from '../environments/environment';

describe('DailyChallengeService', () => {
  let service: DailyChallengeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DailyChallengeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads today’s status and daily leaderboard', () => {
    let date: string | undefined;
    service.getStatus().subscribe((status) => (date = status.date));

    http.expectOne(`${environment.apiBaseUrl}/daily`).flush({
      date: '2026-10-19',
      totalRounds: 5,
      roundTimeoutSeconds: 30,
      language: 'EN',
      inProgress: false,
      completed: true,
      score: 420,
      correctAnswers: 4,
      rank: 2,
      leaderboard: [
        { rank: 1, playerId: 1, name: 'Ada', score: 500, correctAnswers: 5, totalRounds: 5 },
      ],
    });

    expect(date).toBe('2026-10-19');
  });

  it('starts or resumes the attempt through the daily endpoint', () => {
    service.start().subscribe((round) => expect(round.dailyChallenge).toBeTrue());

    http.expectOne(`${environment.apiBaseUrl}/daily/start`).flush({
      gameId: 9,
      status: 'IN_PROGRESS',
      round: 1,
      totalRounds: 5,
      players: [],
      answeredPlayerIds: [],
      finished: false,
      dailyChallenge: true,
    });
  });
});
