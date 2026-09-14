import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AccountInfo } from '@azure/msal-browser';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { StoreApi } from './core/store.api';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  protected isAuthenticated = signal(false);
  protected account = signal<AccountInfo | null>(null);
  protected cartCount = signal(0);
  protected unread = signal(0);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    private readonly storeApi: StoreApi,
  ) {}

  ngOnInit(): void {
    this.authService
      .trackAuthenticationStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.authService.refreshAuthenticationState();
        const loggedIn = this.authService.activeAccount !== undefined;
        this.isAuthenticated.set(loggedIn);
        this.account.set(this.authService.activeAccount ?? null);
        if (loggedIn) {
          this.refreshBadges();
        } else {
          this.cartCount.set(0);
          this.unread.set(0);
        }
      });

    this.storeApi.cartChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.isAuthenticated()) {
        this.refreshBadges();
      }
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

  protected refreshBadges(): void {
    this.storeApi.cart().subscribe({
      next: (cart) => this.cartCount.set(cart.totalItems ?? 0),
      error: () => this.cartCount.set(0),
    });
    this.storeApi.unreadCount().subscribe({
      next: (result) => this.unread.set(result.count ?? 0),
      error: () => this.unread.set(0),
    });
  }
}
