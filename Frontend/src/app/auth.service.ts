import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface AuthUser {
  username: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiBaseUrl}/auth`;
  private readonly tokenKey = 'jwt';
  private readonly userKey = 'authUser';

  constructor(private http: HttpClient) {}

  /** Authenticate and store the returned session. */
  login(username: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, { username, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Create an account; the backend logs the user in straight away. */
  register(username: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, { username, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getUser(): AuthUser | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  /** True only when a non-expired token is stored. Expired sessions are cleared. */
  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    if (this.isExpired(token)) {
      this.logout();
      return false;
    }
    return true;
  }

  /** True when the stored session belongs to an administrator. */
  isAdmin(): boolean {
    return this.getUser()?.role === 'ADMIN';
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.token);
    localStorage.setItem(this.userKey, JSON.stringify({ username: res.username, role: res.role }));
  }

  private isExpired(token: string): boolean {
    const payload = this.decodePayload(token);
    if (!payload || typeof payload.exp !== 'number') {
      return false; // cannot read expiry → let the server decide
    }
    return Date.now() >= payload.exp * 1000;
  }

  private decodePayload(token: string): { exp?: number } | null {
    try {
      const part = token.split('.')[1];
      if (!part) {
        return null;
      }
      const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      return JSON.parse(atob(padded));
    } catch {
      return null;
    }
  }
}
