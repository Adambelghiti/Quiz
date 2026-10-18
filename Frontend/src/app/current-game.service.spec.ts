import { TestBed } from '@angular/core/testing';
import { CurrentGameService } from './current-game.service';

describe('CurrentGameService', () => {
  let service: CurrentGameService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [CurrentGameService] });
    service = TestBed.inject(CurrentGameService);
  });

  it('starts with no current game', () => {
    expect(service.getCurrentId()).toBeNull();
  });

  it('persists the current game id and clears it', () => {
    service.set(42);
    expect(service.getCurrentId()).toBe(42);
    expect(localStorage.getItem('currentGameId')).toBe('42');

    service.clear();
    expect(service.getCurrentId()).toBeNull();
    expect(localStorage.getItem('currentGameId')).toBeNull();
  });
});
