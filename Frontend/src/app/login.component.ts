import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from './auth.service';

type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule],
  standalone: true,
})
export class LoginComponent {
  mode: AuthMode = 'login';
  username = '';
  password = '';
  error: string | null = null;
  loading = false;

  constructor(private auth: AuthService, private router: Router, private route: ActivatedRoute) {}

  get isRegister(): boolean {
    return this.mode === 'register';
  }

  toggleMode(): void {
    this.mode = this.isRegister ? 'login' : 'register';
    this.error = null;
  }

  openSettings(): void {
    this.router.navigate(['/settings']);
  }

  submit(): void {
    if (this.loading) {
      return;
    }
    this.error = null;

    const username = this.username.trim();
    if (!username || !this.password) {
      this.error = 'Please enter a username and a password.';
      return;
    }
    if (this.isRegister && this.password.length < 4) {
      this.error = 'Password must be at least 4 characters long.';
      return;
    }

    this.loading = true;
    const request$ = this.isRegister
      ? this.auth.register(username, this.password)
      : this.auth.login(username, this.password);

    request$.subscribe({
      next: () => this.router.navigateByUrl(this.redirectTarget()),
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      },
    });
  }

  /**
   * Where to go after a successful login. The auth guard stores the attempted
   * URL (for example an invite link like {@code /room/7}) so invited players
   * land straight in the room. Only same-app paths are accepted.
   */
  private redirectTarget(): string {
    const target = this.route.snapshot.queryParamMap.get('redirectTo');
    if (target && target.startsWith('/') && !target.startsWith('//')) {
      return target;
    }
    return '/lobby';
  }

  private messageFor(err: HttpErrorResponse): string {
    const backendMessage =
      err.error && typeof err.error === 'object' ? (err.error.message as string) : null;
    if (backendMessage) {
      return backendMessage;
    }
    if (err.status === 401) {
      return 'Invalid username or password.';
    }
    if (err.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    return 'Something went wrong. Please try again.';
  }
}
