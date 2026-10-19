import { PreferencesService } from './preferences.service';

describe('PreferencesService', () => {
  const storageKey = 'quizgame.speech';

  beforeEach(() => localStorage.removeItem(storageKey));

  it('leaves voice reading off until the player opts in', () => {
    const service = new PreferencesService();

    expect(service.speech.enabled).toBeFalse();
    expect(service.speech.autoReadQuestion).toBeTrue();
    expect(service.speech.readSelectedOption).toBeTrue();
  });

  it('persists the opt-in across reloads', () => {
    const first = new PreferencesService();
    first.setSpeechEnabled(true);
    first.setAutoReadQuestion(false);
    first.setReadSelectedOption(false);

    const reloaded = new PreferencesService();

    expect(reloaded.speech).toEqual({
      enabled: true,
      autoReadQuestion: false,
      readSelectedOption: false,
    });
  });

  it('survives malformed stored preferences', () => {
    localStorage.setItem(storageKey, '{not json');

    const service = new PreferencesService();

    expect(service.speech.enabled).toBeFalse();
    expect(service.speech.autoReadQuestion).toBeTrue();
  });

  it('fills in defaults when stored preferences are partial', () => {
    localStorage.setItem(storageKey, JSON.stringify({ enabled: true }));

    const service = new PreferencesService();

    expect(service.speech.enabled).toBeTrue();
    expect(service.speech.autoReadQuestion).toBeTrue();
    expect(service.speech.readSelectedOption).toBeTrue();
  });
});
