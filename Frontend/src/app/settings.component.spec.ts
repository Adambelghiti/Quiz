import { PreferencesService } from './preferences.service';
import { SettingsComponent } from './settings.component';
import { SpeechService } from './speech.service';

describe('SettingsComponent', () => {
  let preferences: PreferencesService;
  let speech: jasmine.SpyObj<SpeechService>;
  let router: jasmine.SpyObj<import('@angular/router').Router>;
  let component: SettingsComponent;

  beforeEach(() => {
    localStorage.removeItem('quizgame.speech');
    preferences = new PreferencesService();
    speech = jasmine.createSpyObj<SpeechService>('SpeechService', ['speak', 'stop'], {
      supported: true,
    });
    router = jasmine.createSpyObj('Router', ['navigate']);
    component = new SettingsComponent(preferences, speech, router);
  });

  it('turns voice reading on only when the player opts in', () => {
    expect(component.speechEnabled).toBeFalse();

    component.onSpeechToggle({ target: { checked: true } } as unknown as Event);

    expect(preferences.speech.enabled).toBeTrue();
    expect(component.speechEnabled).toBeTrue();
  });

  it('stops speaking when voice reading is turned off', () => {
    preferences.setSpeechEnabled(true);

    component.onSpeechToggle({ target: { checked: false } } as unknown as Event);

    expect(preferences.speech.enabled).toBeFalse();
    expect(speech.stop).toHaveBeenCalled();
  });

  it('plays a sample phrase on request', () => {
    component.testVoice();

    expect(speech.speak).toHaveBeenCalledWith(jasmine.stringMatching(/voice reading is on/i), 'EN');
  });

  it('returns to the lobby', () => {
    component.back();

    expect(router.navigate).toHaveBeenCalledWith(['/lobby']);
  });
});
