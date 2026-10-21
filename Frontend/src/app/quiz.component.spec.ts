import { QuizComponent } from './quiz.component';
import { RoundResponse } from './lobby.service';
import { PreferencesService } from './preferences.service';
import { SpeechService } from './speech.service';

interface TestComponent {
  component: QuizComponent;
  preferences: PreferencesService;
  speech: jasmine.SpyObj<SpeechService>;
}

/**
 * Builds the component directly: these getters only depend on the round state,
 * so most injected services are not needed.
 */
function buildComponent(questionLanguage?: string, options: string[] = []): TestComponent {
  localStorage.removeItem('quizgame.speech');
  const preferences = new PreferencesService();
  const speech = jasmine.createSpyObj<SpeechService>(
    'SpeechService',
    ['readQuestion', 'readOption', 'readFeedback', 'speak', 'stop', 'hasVoiceFor'],
    { supported: true },
  );
  speech.hasVoiceFor.and.returnValue(true);
  const component = new QuizComponent(
    null!,
    null!,
    null!,
    null!,
    null!,
    null!,
    null!,
    speech,
    preferences,
  );
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
      options,
      difficulty: 1,
      language: questionLanguage,
    },
  } as RoundResponse;
  return { component, preferences, speech };
}

function componentWith(questionLanguage?: string): QuizComponent {
  return buildComponent(questionLanguage).component;
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

describe('QuizComponent practice timing', () => {
  it('never expires an untimed practice question', () => {
    const { component } = buildComponent('EN', ['Paris', 'Rome']);
    component.round!.practice = true;
    component.round!.roundTimeoutSeconds = 0;
    component.timeLeft = 0;

    expect(component.untimed).toBeTrue();
    expect(component.timeExpired).toBeFalse();
  });

  it('still expires a timed round when the clock reaches zero', () => {
    const { component } = buildComponent('EN', ['Paris', 'Rome']);
    component.round!.roundTimeoutSeconds = 30;
    component.timeLeft = 0;

    expect(component.untimed).toBeFalse();
    expect(component.timeExpired).toBeTrue();
  });
});

describe('QuizComponent voice reading', () => {
  it('labels answer choices in the question language for assistive technology', () => {
    const { component } = buildComponent('FR', ['Paris', 'Rome']);

    expect(component.optionAriaLabel(0, 'Paris')).toBe('Réponse 1 : Paris');
    expect(component.optionAriaLabel(1, 'Rome')).toBe('Réponse 2 : Rome');
  });

  it('labels Arabic answer choices with Arabic ordinals', () => {
    const { component } = buildComponent('AR', ['القاهرة', 'الرياض']);

    expect(component.optionAriaLabel(0, 'القاهرة')).toBe('الإجابة الأولى: القاهرة');
  });

  it('warns when voice reading is on but no voice matches the question language', () => {
    const { component, preferences, speech } = buildComponent('AR', ['القاهرة']);
    preferences.setSpeechEnabled(true);
    speech.hasVoiceFor.and.returnValue(false);

    expect(component.voiceMissingForQuestion).toBeTrue();
  });

  it('does not warn when a matching voice is installed', () => {
    const { component, preferences } = buildComponent('FR', ['Paris']);
    preferences.setSpeechEnabled(true);

    expect(component.voiceMissingForQuestion).toBeFalse();
  });

  it('does not read an answer choice unless voice reading is enabled', () => {
    const { component, speech } = buildComponent('EN', ['Paris', 'Rome']);
    component.timeLeft = 30;

    component.selectAnswer('Paris');

    expect(component.selectedAnswer).toBe('Paris');
    expect(speech.readOption).not.toHaveBeenCalled();
  });

  it('reads the selected answer choice in the question language', () => {
    const { component, preferences, speech } = buildComponent('FR', ['Paris', 'Rome']);
    preferences.setSpeechEnabled(true);
    component.timeLeft = 30;

    component.selectAnswer('Rome');

    expect(speech.readOption).toHaveBeenCalledWith('Rome', 1, 'FR');
  });

  it('can read the question and choices on demand', () => {
    const { component, preferences, speech } = buildComponent('AR', ['القاهرة', 'الرياض']);
    preferences.setSpeechEnabled(true);

    component.readQuestionAloud();

    expect(speech.readQuestion).toHaveBeenCalledWith(component.question!);
  });

  it('stops speaking when the quiz is destroyed', () => {
    const { component, speech } = buildComponent('EN');

    component.ngOnDestroy();

    expect(speech.stop).toHaveBeenCalled();
  });
});
