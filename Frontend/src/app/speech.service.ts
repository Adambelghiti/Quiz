import { Inject, Injectable, InjectionToken } from '@angular/core';
import {
  answerChoicePrefix,
  choiceCountLabel,
  correctFeedback,
  incorrectFeedback,
  lateFeedback,
  speechLanguage,
} from './speech-labels';

/**
 * Wrappers around the browser's Web Speech API. Injecting the engine and the
 * utterance constructor keeps the service unit-testable without a real voice.
 */
export const SPEECH_SYNTHESIS = new InjectionToken<SpeechSynthesis | null>('SPEECH_SYNTHESIS', {
  providedIn: 'root',
  factory: () =>
    typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : null,
});

export const SPEECH_UTTERANCE = new InjectionToken<typeof SpeechSynthesisUtterance | null>(
  'SPEECH_UTTERANCE',
  {
    providedIn: 'root',
    factory: () =>
      typeof SpeechSynthesisUtterance !== 'undefined' ? SpeechSynthesisUtterance : null,
  },
);

/** The subset of a question that can be read aloud. */
export interface ReadableQuestion {
  readonly text: string;
  readonly options?: readonly string[];
  readonly language?: string | null;
}

/** The subset of an answer result that can be read aloud. */
export interface ReadableFeedback {
  readonly correct: boolean;
  readonly correctAnswer: string;
  readonly scoreDelta: number;
  readonly late?: boolean;
  readonly explanation?: string | null;
}

const LANGUAGE_TAGS: Record<string, string> = {
  EN: 'en-US',
  FR: 'fr-FR',
  AR: 'ar-SA',
};

/**
 * On-device text-to-speech using the Web Speech API. No network request and no
 * extra dependency: the browser's installed voices do the work. If a browser
 * or voice pack is missing, {@link supported} is false and every method becomes
 * a harmless no-op.
 */
@Injectable({ providedIn: 'root' })
export class SpeechService {
  constructor(
    @Inject(SPEECH_SYNTHESIS) private readonly synth: SpeechSynthesis | null,
    @Inject(SPEECH_UTTERANCE) private readonly Utterance: typeof SpeechSynthesisUtterance | null,
  ) {}

  get supported(): boolean {
    return this.synth !== null && this.Utterance !== null;
  }

  /**
   * Whether an installed voice matches the language. Before the browser has
   * listed its voices, return true so the UI does not show a false warning.
   */
  hasVoiceFor(language?: string | null): boolean {
    if (!this.synth) {
      return false;
    }
    const voices = this.synth.getVoices?.() ?? [];
    if (voices.length === 0) {
      return true;
    }
    const primary = this.languageTag(language).split('-')[0].toLowerCase();
    return voices.some((voice) => {
      const tag = voice.lang.toLowerCase();
      return tag === primary || tag.startsWith(`${primary}-`);
    });
  }

  /** BCP-47 tag used by the speech engine, derived from the question language. */
  languageTag(language?: string | null): string {
    return LANGUAGE_TAGS[speechLanguage(language)];
  }

  /** Read a question followed by every answer choice, numbered in display order. */
  readQuestion(question: ReadableQuestion): void {
    const parts = [question.text.trim()];
    const options = question.options ?? [];

    if (options.length > 0) {
      parts.push(choiceCountLabel(options.length, question.language));
      options.forEach((option, index) => {
        parts.push(`${answerChoicePrefix(index, question.language)} ${option.trim()}.`);
      });
    }

    this.speak(parts.filter(Boolean).join(' '), question.language);
  }

  /** Read one answer choice, using the same numbering as the question. */
  readOption(option: string, index: number, language?: string | null): void {
    this.speak(`${answerChoicePrefix(index, language)} ${option.trim()}.`, language);
  }

  /** Read the outcome of an answer, including the no-points late case. */
  readFeedback(result: ReadableFeedback, language?: string | null): void {
    let text: string;
    if (result.late) {
      text = lateFeedback(result.correctAnswer, language);
    } else if (result.correct) {
      text = correctFeedback(result.scoreDelta, language);
    } else {
      text = incorrectFeedback(result.correctAnswer, language);
    }
    const explanation = result.explanation?.trim();
    if (explanation) {
      text += ` ${explanation}`;
    }
    this.speak(text, language);
  }

  /** Speak arbitrary text, cancelling anything already in progress. */
  speak(text: string, language?: string | null): void {
    if (!this.supported || !text.trim()) {
      return;
    }

    this.synth!.cancel();
    const utterance = new this.Utterance!(text);
    utterance.lang = this.languageTag(language);
    utterance.rate = 0.95;
    utterance.pitch = 1;

    const voice = this.pickVoice(utterance.lang);
    if (voice) {
      utterance.voice = voice;
    }

    try {
      this.synth!.speak(utterance);
    } catch {
      /* Some browsers refuse speech before a user gesture; the next click retries. */
    }
  }

  /** Stop the current utterance, e.g. when leaving the quiz. */
  stop(): void {
    if (!this.synth) {
      return;
    }
    try {
      this.synth.cancel();
    } catch {
      /* Nothing to cancel. */
    }
  }

  /** Prefer an exact locale match, then any voice speaking the same language. */
  private pickVoice(tag: string): SpeechSynthesisVoice | null {
    if (!this.synth) {
      return null;
    }
    const voices = this.synth.getVoices?.() ?? [];
    const wanted = tag.toLowerCase();
    const primary = wanted.split('-')[0];
    return (
      voices.find((voice) => voice.lang.toLowerCase() === wanted) ??
      voices.find((voice) => voice.lang.toLowerCase() === primary) ??
      voices.find((voice) => voice.lang.toLowerCase().startsWith(`${primary}-`)) ??
      voices.find((voice) => voice.default) ??
      voices[0] ??
      null
    );
  }
}
