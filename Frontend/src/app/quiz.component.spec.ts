import { QuizComponent } from './quiz.component';
import { RoundResponse } from './lobby.service';

/**
 * Builds the component directly: these getters only depend on the round state,
 * so the injected services are not needed.
 */
function componentWith(questionLanguage?: string): QuizComponent {
  const component = new QuizComponent(null!, null!, null!, null!, null!, null!);
  component.round = {
    gameId: 1,
    status: 'IN_PROGRESS',
    round: 1,
    totalRounds: 3,
    players: [],
    answeredPlayerIds: [],
    finished: false,
    question: {
      id: 1,
      text: 'question',
      options: [],
      difficulty: 1,
      language: questionLanguage,
    },
  } as RoundResponse;
  return component;
}

describe('QuizComponent question direction', () => {
  it('reads Arabic questions right to left', () => {
    const component = componentWith('AR');

    expect(component.questionDirection).toBe('rtl');
    expect(component.questionLang).toBe('ar');
  });

  it('keeps English questions left to right', () => {
    const component = componentWith('EN');

    expect(component.questionDirection).toBe('ltr');
    expect(component.questionLang).toBe('en');
  });

  it('keeps French questions left to right', () => {
    const component = componentWith('FR');

    expect(component.questionDirection).toBe('ltr');
    expect(component.questionLang).toBe('fr');
  });

  it('falls back to left to right before a question arrives', () => {
    const component = componentWith();

    expect(component.questionDirection).toBe('ltr');
    expect(component.questionLang).toBe('en');
  });
});
