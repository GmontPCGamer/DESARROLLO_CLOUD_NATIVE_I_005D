import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AccountInfo } from '@azure/msal-browser';
import { AccessTokenInfo, AuthService } from '../auth/auth.service';
import { Me } from '../core/models';
import { StoreApi, errorMessage } from '../core/store.api';

/**
 * Vista protegida con MsalGuard. Muestra:
 * - Claims del ID token (lado cliente, MSAL).
 * - Resumen del access token pedido para la API (scopes, aud, iss, exp).
 * - Lo que el BFF ve tras validar el token (GET /api/me): scopes, roles y authorities.
 */
@Component({
  imports: [DatePipe],
  selector: 'app-perfil',
  styleUrl: './perfil.css',
  templateUrl: './perfil.html',
})
export class Perfil implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(StoreApi);

  protected readonly account = signal<AccountInfo | null>(null);
  protected readonly token = signal<AccessTokenInfo | null>(null);
  protected readonly me = signal<Me | null>(null);
  protected readonly loadingMe = signal(true);
  protected readonly meError = signal<string | null>(null);

  ngOnInit(): void {
    this.account.set(this.auth.activeAccount ?? null);
    this.auth.getAccessTokenInfo().then((info) => this.token.set(info));
    this.api.me().subscribe({
      next: (me) => {
        this.me.set(me);
        this.loadingMe.set(false);
      },
      error: (err) => {
        this.loadingMe.set(false);
        this.meError.set(errorMessage(err, 'El BFF no pudo validar el token'));
      },
    });
  }

  protected claim(name: string): string {
    const claims = this.account()?.idTokenClaims as Record<string, unknown> | undefined;
    const value = claims?.[name];
    if (value === undefined || value === null) return '—';
    return Array.isArray(value) ? value.join(', ') : String(value);
  }

  protected idTokenRoles(): string[] {
    const claims = this.account()?.idTokenClaims as Record<string, unknown> | undefined;
    return (claims?.['roles'] as string[]) ?? [];
  }
}
