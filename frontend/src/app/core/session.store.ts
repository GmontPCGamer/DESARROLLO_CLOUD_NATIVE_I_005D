import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { Me } from './models';
import { StoreApi } from './store.api';

/**
 * Estado de sesión compartido por toda la app: cuenta MSAL, claims validados
 * por el BFF (/api/me) y contadores del header.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly auth = inject(AuthService);
  private readonly api = inject(StoreApi);

  readonly me = signal<Me | null>(null);
  readonly cartCount = signal(0);
  readonly unread = signal(0);

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly account = this.auth.account;
  readonly isAdmin = computed(() => this.me()?.isAdmin ?? false);
  readonly displayName = computed(() => {
    const account = this.account();
    return account?.name || account?.username || '';
  });

  /** Carga /api/me y contadores tras detectar una sesión. */
  refresh(): void {
    if (!this.isAuthenticated()) {
      this.clear();
      return;
    }
    this.api.me().subscribe({
      next: (me) => this.me.set(me),
      error: () => this.me.set(null),
    });
    this.refreshBadges();
  }

  refreshBadges(): void {
    if (!this.isAuthenticated()) {
      return;
    }
    this.api.cart().subscribe({
      next: (cart) => this.cartCount.set(cart.totalItems ?? 0),
      error: () => this.cartCount.set(0),
    });
    this.api.unreadCount().subscribe({
      next: (result) => this.unread.set(result.count ?? 0),
      error: () => this.unread.set(0),
    });
  }

  clear(): void {
    this.me.set(null);
    this.cartCount.set(0);
    this.unread.set(0);
  }
}
