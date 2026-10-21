import { SpeechService } from './speech.service';

class FakeUtterance {
  text: string;
  lang = '';
  rate = 1;
  pitch = 1;
  voice: SpeechSynthesisVoice | null = null;

  constructor(text = '') {
    this.text = text;
  }
}

class FakeSynthesis {
  spoken: FakeUtterance[] = [];
  cancelled = 0;
  voices: SpeechSynthesisVoice[] = [];

  speak(utterance: SpeechSynthesisUtterance): void {
    this.spoken.push(utterance as unknown as FakeUtterance);
  }

  cancel(): void {
    this.cancelled++;
  }

  getVoices(): SpeechSynthesisVoice[] {
    return this.voices;
  }
}

function serviceWith(synth: FakeSynthesis): SpeechService {
  return new SpeechService(
    synth as unknown as SpeechSynthesis,
    FakeUtterance as unknown as typeof SpeechSynthesisUtterance,
  );
}

describe('SpeechService', () => {
  it('maps question languages to BCP-47 tags', () => {
    const service = serviceWith(new FakeSynthesis());

    expect(service.languageTag('EN')).toBe('en-US');
    expect(service.languageTag('FR')).toBe('fr-FR');
    expect(service.languageTag('AR')).toBe('ar-SA');
    expect(service.languageTag(null)).toBe('en-US');
    expect(service.languageTag('de')).toBe('en-US');
  });

  it('reads the question and every numbered answer choice', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readQuestion({
      text: 'What is the capital of France?',
      options: ['Paris', 'Rome', 'Madrid'],
      language: 'EN',
    });

    expect(synth.cancelled).toBe(1);
    expect(synth.spoken.length).toBe(1);
    expect(synth.spoken[0].text).toContain('What is the capital of France?');
    expect(synth.spoken[0].text).toContain('Answer 1: Paris');
    expect(synth.spoken[0].text).toContain('Answer 2: Rome');
    expect(synth.spoken[0].text).toContain('Answer 3: Madrid');
    expect(synth.spoken[0].lang).toBe('en-US');
  });

  it('uses French answer labels for a French question', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readQuestion({
      text: 'Quelle est la capitale de la France ?',
      options: ['Paris', 'Rome', 'Madrid'],
      language: 'FR',
    });

    expect(synth.spoken[0].text).toContain('3 choix');
    expect(synth.spoken[0].text).toContain('Réponse 1 : Paris');
    expect(synth.spoken[0].text).toContain('Réponse 2 : Rome');
    expect(synth.spoken[0].text).not.toContain('Answer');
  });

  it('uses Arabic answer labels for an Arabic question', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readQuestion({
      text: 'ما هي عاصمة فرنسا؟',
      options: ['باريس', 'روما', 'مدريد'],
      language: 'AR',
    });

    expect(synth.spoken[0].text).toContain('ما هي عاصمة فرنسا؟');
    expect(synth.spoken[0].text).toContain('الإجابة الأولى');
    expect(synth.spoken[0].text).toContain('الإجابة الثانية');
    expect(synth.spoken[0].text).not.toContain('Answer');
  });

  it('uses a localized label when reading one answer choice', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readOption('Rome', 1, 'FR');
    service.readOption('الرياض', 1, 'AR');

    expect(synth.spoken[0].text).toContain('Réponse 2 : Rome');
    expect(synth.spoken[1].text).toContain('الإجابة الثانية');
  });

  it('localizes feedback for French and Arabic questions', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readFeedback({ correct: false, correctAnswer: 'Paris', scoreDelta: 0 }, 'FR');
    service.readFeedback({ correct: true, correctAnswer: 'باريس', scoreDelta: 75 }, 'AR');

    expect(synth.spoken[0].text).toContain('Pas tout à fait');
    expect(synth.spoken[0].text).toContain('La bonne réponse était Paris');
    expect(synth.spoken[0].text).not.toContain('Not quite');
    expect(synth.spoken[1].text).toContain('صحيح');
    expect(synth.spoken[1].text).toContain('75');
    expect(synth.spoken[1].text).not.toContain('Correct');
  });

  it('uses a matching installed voice when one is available', () => {
    const synth = new FakeSynthesis();
    synth.voices = [
      { lang: 'en-US', name: 'English' } as SpeechSynthesisVoice,
      { lang: 'fr-FR', name: 'French' } as SpeechSynthesisVoice,
    ];
    const service = serviceWith(synth);

    service.speak('Bonjour', 'FR');

    expect(synth.spoken[0].lang).toBe('fr-FR');
    expect(synth.spoken[0].voice?.name).toBe('French');
  });

  it('falls back to the default voice instead of staying silent', () => {
    const synth = new FakeSynthesis();
    synth.voices = [{ lang: 'en-US', name: 'English', default: true } as SpeechSynthesisVoice];
    const service = serviceWith(synth);

    service.speak('مرحبا', 'AR');

    expect(synth.spoken[0].lang).toBe('ar-SA');
    expect(synth.spoken[0].voice?.name).toBe('English');
  });

  it('matches a voice tagged with only the language code', () => {
    const synth = new FakeSynthesis();
    synth.voices = [
      { lang: 'en-US', name: 'English', default: true } as SpeechSynthesisVoice,
      { lang: 'ar', name: 'Arabic' } as SpeechSynthesisVoice,
    ];
    const service = serviceWith(synth);

    service.speak('مرحبا', 'AR');

    expect(synth.spoken[0].voice?.name).toBe('Arabic');
  });

  it('reports whether a matching voice is installed', () => {
    const synth = new FakeSynthesis();
    synth.voices = [{ lang: 'en-US', name: 'English' } as SpeechSynthesisVoice];
    const service = serviceWith(synth);

    expect(service.hasVoiceFor('EN')).toBeTrue();
    expect(service.hasVoiceFor('FR')).toBeFalse();
    expect(service.hasVoiceFor('AR')).toBeFalse();
  });

  it('does not report a missing voice before the browser has listed them', () => {
    const service = serviceWith(new FakeSynthesis());

    expect(service.hasVoiceFor('AR')).toBeTrue();
  });

  it('explains a late answer without awarding points', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readFeedback(
      {
        correct: true,
        correctAnswer: 'Paris',
        scoreDelta: 0,
        late: true,
      },
      'EN',
    );

    expect(synth.spoken[0].text).toContain('Time ran out');
    expect(synth.spoken[0].text).toContain('Paris');
    expect(synth.spoken[0].text).toContain('No points');
  });

  it('reports a correct answer and its score', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readFeedback(
      {
        correct: true,
        correctAnswer: 'Paris',
        scoreDelta: 75,
      },
      'EN',
    );

    expect(synth.spoken[0].text).toContain('Correct');
    expect(synth.spoken[0].text).toContain('75 points');
  });

  it('reads the explanation after the answer feedback', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.readFeedback(
      {
        correct: false,
        correctAnswer: 'Paris',
        scoreDelta: 0,
        explanation: 'Paris is the capital and largest city of France.',
      },
      'EN',
    );

    expect(synth.spoken[0].text).toContain('Paris is the capital');
    expect(synth.spoken[0].text.indexOf('Not quite')).toBeLessThan(
      synth.spoken[0].text.indexOf('Paris is the capital'),
    );
  });

  it('stops any speech when asked', () => {
    const synth = new FakeSynthesis();
    const service = serviceWith(synth);

    service.stop();

    expect(synth.cancelled).toBe(1);
  });

  it('reports that speech is unavailable without a synthesis engine', () => {
    const service = new SpeechService(null, null);

    expect(service.supported).toBeFalse();
    expect(() => service.speak('hello')).not.toThrow();
  });
});
