import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  // No external stylesheet – remove to avoid missing file errors.
})
export class App {
  protected readonly title = signal('FrontEnd');
}
