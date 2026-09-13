import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AccountInfo, InteractionStatus } from '@azure/msal-browser';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from './auth/auth.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');
  protected isAuthenticated = signal(false);
  protected account = signal<AccountInfo | null>(null);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.trackAuthenticationStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.authService.refreshAuthenticationState();
        this.isAuthenticated.set(this.authService.activeAccount !== undefined);
        this.account.set(this.authService.activeAccount);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected login(): void {
    this.authService.login();
  }

  protected logout(): void {
    this.authService.logout();
  }
}