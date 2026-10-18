import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Service that keeps track of the current game id across the app.
 * The id is persisted so a page refresh can resume the game.
 */
@Injectable({ providedIn: 'root' })
export class CurrentGameService {
  private readonly storageKey = 'currentGameId';
  private current$ = new BehaviorSubject<number | null>(this.restore());

  /** Set the active game ID. */
  set(id: number): void {
    localStorage.setItem(this.storageKey, String(id));
    this.current$.next(id);
  }

  /** Forget the active game ID. */
  clear(): void {
    localStorage.removeItem(this.storageKey);
    this.current$.next(null);
  }

  /** Observable of the current game id (or null if none). */
  get(): Observable<number | null> {
    return this.current$;
  }

  /** Synchronous snapshot of the current game id, if any. */
  getCurrentId(): number | null {
    return this.current$.value;
  }

  private restore(): number | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }
    const id = Number(raw);
    return Number.isFinite(id) && id > 0 ? id : null;
  }
}
