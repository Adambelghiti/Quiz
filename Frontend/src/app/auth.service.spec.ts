import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../environments/environment';

/** A JWT-shaped token whose payload decodes to {"exp": 9999999999}. */
const VALID_TOKEN = 'header.eyJleHAiOjk5OTk5OTk5OTl9.signature';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the session on login and reports logged in', () => {
    service.login('demo', 'demo123').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/auth/login`).flush({
      token: VALID_TOKEN,
      username: 'demo',
      role: 'USER',
    });

    expect(service.getToken()).toBe(VALID_TOKEN);
    expect(service.isLoggedIn()).toBe(true);
    expect(service.isAdmin()).toBe(false);
    expect(service.getUser()?.username).toBe('demo');
  });

  it('reports the admin role', () => {
    service.login('admin', 'admin123').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/auth/login`).flush({
      token: VALID_TOKEN,
      username: 'admin',
      role: 'ADMIN',
    });

    expect(service.isAdmin()).toBe(true);
  });

  it('clears the session on logout', () => {
    service.register('demo', 'demo123').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/auth/register`).flush({
      token: VALID_TOKEN,
      username: 'demo',
      role: 'USER',
    });

    service.logout();
    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
  });
});
