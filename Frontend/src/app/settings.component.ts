import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PreferencesService } from './preferences.service';
import { SpeechService } from './speech.service';

/**
 * Opt-in accessibility settings. Voice reading is off by default; players who
 * need it enable it here once and the choice is remembered on this device.
 */
@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  imports: [CommonModule],
  standalone: true,
})
export class SettingsComponent {
  constructor(
    public preferences: PreferencesService,
    public speech: SpeechService,
    private router: Router
  ) {}

  get supported(): boolean {
    return this.speech.supported;
  }

  get speechEnabled(): boolean {
    return this.preferences.speech.enabled;
  }

  get autoReadQuestion(): boolean {
    return this.preferences.speech.autoReadQuestion;
  }

  get readSelectedOption(): boolean {
    return this.preferences.speech.readSelectedOption;
  }

  onSpeechToggle(event: Event): void {
    const enabled = (event.target as HTMLInputElement).checked;
    this.preferences.setSpeechEnabled(enabled);
    if (!enabled) {
      this.speech.stop();
    }
  }

  onAutoReadToggle(event: Event): void {
    this.preferences.setAutoReadQuestion((event.target as HTMLInputElement).checked);
  }

  onReadOptionToggle(event: Event): void {
    this.preferences.setReadSelectedOption((event.target as HTMLInputElement).checked);
  }

  /** Play a short sample so the player can check the volume and voice. */
  testVoice(): void {
    this.speech.speak(
      'Voice reading is on. Questions and answer choices will be read aloud.',
      'EN'
    );
  }

  back(): void {
    this.router.navigate(['/lobby']);
  }
}
