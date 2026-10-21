import { correctFeedback, timeUpFeedback } from './speech-labels';

describe('speech labels', () => {
  it('localizes the time-up announcement', () => {
    expect(timeUpFeedback('EN')).toBe("Time's up.");
    expect(timeUpFeedback('FR')).toBe('Temps écoulé.');
    expect(timeUpFeedback('AR')).toBe('انتهى الوقت.');
  });

  it('uses natural Arabic plurals for points', () => {
    expect(correctFeedback(1, 'AR')).toContain('نقطة واحدة');
    expect(correctFeedback(2, 'AR')).toContain('نقطتين');
    expect(correctFeedback(3, 'AR')).toContain('3 نقاط');
    expect(correctFeedback(75, 'AR')).toContain('75 نقطة');
    expect(correctFeedback(75, 'AR')).not.toContain('75 نقاط');
  });
});
