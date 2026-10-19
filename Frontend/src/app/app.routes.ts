import { Routes } from '@angular/router';
import { QuizComponent } from './quiz.component';
import { ResultsComponent } from './results.component';
import { LoginComponent } from './login.component';
import { LobbyComponent } from './lobby.component';
import { GameSetupComponent } from './game-setup.component';
import { WaitingRoomComponent } from './waiting-room.component';
import { AdminComponent } from './admin.component';
import { ProfileComponent } from './profile.component';
import { LeaderboardComponent } from './leaderboard.component';
import { SettingsComponent } from './settings.component';
import { DailyChallengeComponent } from './daily.component';
import { AuthGuard } from './auth.guard';
import { AdminGuard } from './admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  // Public: players can turn on voice reading before they sign in.
  { path: 'settings', component: SettingsComponent },
  { path: 'lobby', component: LobbyComponent, canActivate: [AuthGuard] },
  { path: 'daily', component: DailyChallengeComponent, canActivate: [AuthGuard] },
  { path: 'setup', component: GameSetupComponent, canActivate: [AuthGuard] },
  { path: 'room/:id', component: WaitingRoomComponent, canActivate: [AuthGuard] },
  { path: 'quiz', component: QuizComponent, canActivate: [AuthGuard] },
  { path: 'results', component: ResultsComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'leaderboard', component: LeaderboardComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [AdminGuard] },
  { path: '**', redirectTo: '/lobby' }
];
