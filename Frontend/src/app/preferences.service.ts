import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/** Player-controlled voice reading preferences. */
export interface SpeechPreferences {
  /** Master switch. Voice reading stays off until the player opts in. */
  readonly enabled: boolean;
  /** Read a new question (and its answer choices) automatically. */
  readonly autoReadQuestion: boolean;
  /** Read an answer choice when the player selects it. */
  readonly readSelectedOption: boolean;
}

const STORAGE_KEY = 'quizgame.speech';

const DEFAULTS: SpeechPreferences = {
  enabled: false,
  autoReadQuestion: true,
  readSelectedOption: true,
};

/**
 * Remembers accessibility choices in {@code localStorage}, mirroring the
 * existing sound-mute preference. Voice reading is deliberately opt-in: games
 * are often played with a screen reader already speaking, and two voices at
 * once are worse than none.
 */
@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private current = this.restore();
  private readonly changes = new BehaviorSubject<SpeechPreferences>(this.current);

  /** Emits whenever a preference changes, so open screens can react. */
  readonly speech$: Observable<SpeechPreferences> = this.changes.asObservable();

  get speech(): SpeechPreferences {
    return this.current;
  }

  setSpeechEnabled(enabled: boolean): void {
    this.update({ enabled });
  }

  setAutoReadQuestion(autoReadQuestion: boolean): void {
    this.update({ autoReadQuestion });
  }

  setReadSelectedOption(readSelectedOption: boolean): void {
    this.update({ readSelectedOption });
  }

  private update(patch: Partial<SpeechPreferences>): void {
    this.current = { ...this.current, ...patch };
    this.changes.next(this.current);
    this.persist();
  }

  private persist(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.current));
    } catch {
      /* Private browsing may refuse storage; preferences still apply this session. */
    }
  }

  private restore(): SpeechPreferences {
    if (typeof localStorage === 'undefined') {
      return DEFAULTS;
    }
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (!stored) {
        return DEFAULTS;
      }
      const parsed = JSON.parse(stored) as Partial<Record<keyof SpeechPreferences, unknown>>;
      return {
        enabled: typeof parsed['enabled'] === 'boolean' ? parsed['enabled'] : DEFAULTS.enabled,
        autoReadQuestion:
          typeof parsed['autoReadQuestion'] === 'boolean'
            ? parsed['autoReadQuestion']
            : DEFAULTS.autoReadQuestion,
        readSelectedOption:
          typeof parsed['readSelectedOption'] === 'boolean'
            ? parsed['readSelectedOption']
            : DEFAULTS.readSelectedOption,
      };
    } catch {
      return DEFAULTS;
    }
  }
}
