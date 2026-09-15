import { Inject, Injectable, signal } from '@angular/core';
import { MsalBroadcastService, MsalService, MSAL_INSTANCE } from '@azure/msal-angular';
import {
  AccountInfo,
  AuthenticationResult,
  EventMessage,
  EventType,
  InteractionStatus,
  IPublicClientApplication,
  RedirectRequest,
} from '@azure/msal-browser';
import { filter, Observable } from 'rxjs';
import { loginScopes } from './auth.config';

/** Resumen del access token obtenido para la API (sin exponer el JWT completo en la UI). */
export interface AccessTokenInfo {
  scopes: string[];
  expiresOn: Date | null;
  audience: string | null;
  issuer: string | null;
  roles: string[];
  tokenVersion: string | null;
  preview: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** true cuando existe una cuenta MSAL en caché. */
  readonly isAuthenticated = signal(false);
  readonly account = signal<AccountInfo | null>(null);

  constructor(
    @Inject(MSAL_INSTANCE) private readonly msalInstance: IPublicClientApplication,
    private readonly msalService: MsalService,
    private readonly broadcastService: MsalBroadcastService,
  ) {
    // Al completar un login o una adquisición de token, fija la cuenta activa.
    this.broadcastService.msalSubject$
      .pipe(
        filter(
          (event: EventMessage) =>
            event.eventType === EventType.LOGIN_SUCCESS ||
            event.eventType === EventType.ACQUIRE_TOKEN_SUCCESS,
        ),
      )
      .subscribe((event) => {
        const result = event.payload as AuthenticationResult | null;
        if (result?.account) {
          this.msalInstance.setActiveAccount(result.account);
        }
        this.refreshAuthenticationState();
      });
  }

  /**
   * Procesa la respuesta del redirect (si la hay) en cada carga de la app.
   * Cuando no hay respuesta, MSAL limpia el estado temporal (evita el error
   * `interaction_in_progress` si el usuario volvió atrás desde la pantalla de Microsoft).
   */
  handleRedirect(): Observable<AuthenticationResult | null> {
    return this.msalService.handleRedirectObservable();
  }

  login(): void {
    this.msalService
      .loginRedirect({ scopes: loginScopes, prompt: 'select_account' } as RedirectRequest)
      .subscribe({
        error: (error) => console.warn('[MSAL] No se pudo iniciar el login', error),
      });
  }

  logout(): void {
    const account = this.activeAccount ?? undefined;
    this.msalService.logoutRedirect({ account }).subscribe();
  }

  /** Emite cuando MSAL termina cualquier interacción (login, redirect, adquisición de token). */
  trackAuthenticationStatus(): Observable<InteractionStatus> {
    return this.broadcastService.inProgress$.pipe(
      filter((status: InteractionStatus) => status === InteractionStatus.None),
    );
  }

  get activeAccount(): AccountInfo | undefined {
    return this.msalInstance.getActiveAccount() ?? this.msalInstance.getAllAccounts()[0];
  }

  refreshAuthenticationState(): void {
    const account = this.activeAccount ?? null;
    if (account && !this.msalInstance.getActiveAccount()) {
      this.msalInstance.setActiveAccount(account);
    }
    this.account.set(account);
    this.isAuthenticated.set(account !== null);
  }

  /**
   * Obtiene (en silencio) el access token para la API y devuelve un resumen
   * legible de sus claims. Usado por la vista Perfil para la demostración.
   */
  async getAccessTokenInfo(): Promise<AccessTokenInfo | null> {
    const account = this.activeAccount;
    if (!account) {
      return null;
    }
    try {
      const result = await this.msalInstance.acquireTokenSilent({ scopes: loginScopes, account });
      const claims = decodeJwtPayload(result.accessToken);
      return {
        scopes: result.scopes,
        expiresOn: result.expiresOn,
        audience: (claims['aud'] as string) ?? null,
        issuer: (claims['iss'] as string) ?? null,
        roles: (claims['roles'] as string[]) ?? [],
        tokenVersion: (claims['ver'] as string) ?? null,
        preview: `${result.accessToken.slice(0, 12)}…${result.accessToken.slice(-6)}`,
      };
    } catch {
      return null;
    }
  }
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  try {
    const payload = token.split('.')[1] ?? '';
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return {};
  }
}
