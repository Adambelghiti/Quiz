import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

/** Allows only authenticated administrators through; everyone else goes to the lobby. */
@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn() && this.auth.isAdmin()) {
      return true;
    }
    return this.router.createUrlTree(['/lobby']);
  }
}
