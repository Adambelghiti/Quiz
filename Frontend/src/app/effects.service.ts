import { Injectable } from '@angular/core';
import confetti from 'canvas-confetti';

type SoundName = 'correct' | 'wrong' | 'tick' | 'win';

/** Files live in public/sounds and are served at the app root by the assets pipeline. */
const SOUND_SOURCES: Record<SoundName, string> = {
  correct: 'sounds/correct.wav',
  wrong: 'sounds/wrong.wav',
  tick: 'sounds/tick.wav',
  win: 'sounds/win.wav',
};

/**
 * The game's "juice": short sound effects, mobile haptics and the victory
 * confetti.
 *
 * Sounds are preloaded {@link HTMLAudioElement}s (standard HTML5 Audio). A mute
 * toggle is persisted in {@code localStorage} so the choice survives reloads,
 * and confetti is skipped for players who prefer reduced motion.
 */
@Injectable({ providedIn: 'root' })
export class EffectsService {
  private readonly storageKey = 'soundMuted';
  private readonly sounds = new Map<SoundName, HTMLAudioElement>();
  private mutedState = this.restoreMuted();

  get muted(): boolean {
    return this.mutedState;
  }

  toggleMuted(): void {
    this.mutedState = !this.mutedState;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.storageKey, String(this.mutedState));
    }
  }

  /** Bright chime and a short tap for a correct answer. */
  answerCorrect(): void {
    this.play('correct');
    this.vibrate(30);
  }

  /** Low buzz and a double tap for a wrong answer. */
  answerWrong(): void {
    this.play('wrong');
    this.vibrate([50, 40, 50]);
  }

  /** One tick per second during the last five seconds of a round. */
  tick(): void {
    this.play('tick');
  }

  /** Full victory: fanfare, a celebratory vibration and confetti. */
  celebrate(): void {
    this.play('win');
    this.vibrate([40, 30, 40, 30, 80]);
    this.confetti();
  }

  /** A two-sided confetti burst, skipped when reduced motion is requested. */
  confetti(): void {
    if (typeof window === 'undefined' || this.prefersReducedMotion()) {
      return;
    }

    const base = {
      spread: 70,
      startVelocity: 55,
      ticks: 220,
      zIndex: 1000,
      colors: ['#4f46e5', '#22c55e', '#f59e0b', '#ec4899', '#38bdf8'],
    };

    void confetti({ ...base, particleCount: 90, angle: 60, origin: { x: 0, y: 0.9 } });
    void confetti({ ...base, particleCount: 90, angle: 120, origin: { x: 1, y: 0.9 } });
    window.setTimeout(() => {
      void confetti({ ...base, particleCount: 60, spread: 100, origin: { x: 0.5, y: 0.6 } });
    }, 180);
  }

  private play(name: SoundName): void {
    if (this.muted) {
      return;
    }
    const audio = this.audio(name);
    try {
      audio.currentTime = 0;
    } catch {
      /* Not seekable before metadata loads; playing from the start is fine. */
    }
    void audio.play().catch(() => {
      /* Browsers may block audio before the first user gesture; ignore. */
    });
  }

  private audio(name: SoundName): HTMLAudioElement {
    let audio = this.sounds.get(name);
    if (!audio) {
      audio = new Audio(SOUND_SOURCES[name]);
      audio.preload = 'auto';
      this.sounds.set(name, audio);
    }
    return audio;
  }

  private vibrate(pattern: number | number[]): void {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern);
    }
  }

  private prefersReducedMotion(): boolean {
    return (
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    );
  }

  private restoreMuted(): boolean {
    return typeof localStorage !== 'undefined' && localStorage.getItem(this.storageKey) === 'true';
  }
}
