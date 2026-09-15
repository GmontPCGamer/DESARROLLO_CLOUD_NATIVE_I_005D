import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { SessionStore } from './core/session.store';
import { StoreApi } from './core/store.api';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  protected readonly session = inject(SessionStore);
  private readonly authService = inject(AuthService);
  private readonly storeApi = inject(StoreApi);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Procesa un posible callback de Entra y limpia interacciones a medias.
    this.authService
      .handleRedirect()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          if (result?.account) {
            this.authService.refreshAuthenticationState();
            this.session.refresh();
          }
        },
        error: (error) => console.warn('[MSAL] Error procesando el redirect', error),
      });

    // Cada vez que MSAL termina una interacción (login, callback /auth, token silencioso)
    // se recalcula la sesión y se cargan claims y contadores.
    this.authService
      .trackAuthenticationStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.authService.refreshAuthenticationState();
        this.session.refresh();
      });

    this.storeApi.cartChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => this.session.refreshBadges());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected login(): void {
    this.authService.login();
  }

  protected logout(): void {
    this.session.clear();
    this.authService.logout();
  }
}
