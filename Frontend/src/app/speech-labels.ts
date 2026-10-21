export type SpeechLanguage = 'EN' | 'FR' | 'AR';

const ARABIC_ORDINALS = ['الأولى', 'الثانية', 'الثالثة', 'الرابعة', 'الخامسة', 'السادسة'];

/** Normalize the backend's language enum to the three supported speech languages. */
export function speechLanguage(language?: string | null): SpeechLanguage {
  const key = (language ?? 'EN').trim().toUpperCase();
  return key === 'FR' || key === 'AR' ? key : 'EN';
}

/**
 * Prefix used for an answer choice, both in speech and in the accessible label.
 * Arabic uses ordinals because that is how choices are normally read aloud.
 */
export function answerChoicePrefix(index: number, language?: string | null): string {
  const number = index + 1;
  switch (speechLanguage(language)) {
    case 'FR':
      return `Réponse ${number} :`;
    case 'AR': {
      const ordinal = ARABIC_ORDINALS[index];
      return ordinal ? `الإجابة ${ordinal}:` : `الإجابة رقم ${number}:`;
    }
    default:
      return `Answer ${number}:`;
  }
}

/** How many choices follow the question, in the question's language. */
export function choiceCountLabel(count: number, language?: string | null): string {
  switch (speechLanguage(language)) {
    case 'FR':
      return `${count} choix.`;
    case 'AR':
      if (count === 1) {
        return 'خيار واحد.';
      }
      if (count === 2) {
        return 'خياران.';
      }
      return `${count} خيارات.`;
    default:
      return `${count} ${count === 1 ? 'answer choice' : 'answer choices'}.`;
  }
}

/** Arabic counts from 3 to 10 take the plural, larger counts take the singular. */
function arabicPoints(score: number): string {
  const count = Math.abs(score);
  if (count === 1) {
    return 'نقطة واحدة';
  }
  if (count === 2) {
    return 'نقطتين';
  }
  if (count >= 3 && count <= 10) {
    return `${count} نقاط`;
  }
  return `${count} نقطة`;
}

/** Spoken feedback for a correct answer, in the question's language. */
export function correctFeedback(score: number, language?: string | null): string {
  switch (speechLanguage(language)) {
    case 'FR':
      return `Correct ! Vous avez obtenu ${score} ${score === 1 ? 'point' : 'points'}.`;
    case 'AR':
      return `صحيح! حصلت على ${arabicPoints(score)}.`;
    default:
      return `Correct! You earned ${score} ${score === 1 ? 'point' : 'points'}.`;
  }
}

/** Spoken feedback for a wrong answer, in the question's language. */
export function incorrectFeedback(answer: string, language?: string | null): string {
  switch (speechLanguage(language)) {
    case 'FR':
      return `Pas tout à fait. La bonne réponse était ${answer}.`;
    case 'AR':
      return `ليس تماماً. الإجابة الصحيحة كانت ${answer}.`;
    default:
      return `Not quite. The correct answer was ${answer}.`;
  }
}

/** Spoken feedback when the round timed out, in the question's language. */
export function lateFeedback(answer: string, language?: string | null): string {
  switch (speechLanguage(language)) {
    case 'FR':
      return `Temps écoulé. La bonne réponse était ${answer}. Aucun point pour cette manche.`;
    case 'AR':
      return `انتهى الوقت. الإجابة الصحيحة كانت ${answer}. لا نقاط في هذه الجولة.`;
    default:
      return `Time ran out. The correct answer was ${answer}. No points this round.`;
  }
}

/** Short spoken announcement when the countdown reaches zero. */
export function timeUpFeedback(language?: string | null): string {
  switch (speechLanguage(language)) {
    case 'FR':
      return 'Temps écoulé.';
    case 'AR':
      return 'انتهى الوقت.';
    default:
      return "Time's up.";
  }
}
