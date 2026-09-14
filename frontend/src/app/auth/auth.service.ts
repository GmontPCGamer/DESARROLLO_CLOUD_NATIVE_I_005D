import { Inject, Injectable, Optional } from '@angular/core';
import { MsalBroadcastService, MsalGuardConfiguration, MsalService, MSAL_GUARD_CONFIG, MSAL_INSTANCE } from '@azure/msal-angular';
import { InteractionStatus, IPublicClientApplication, PopupRequest, RedirectRequest } from '@azure/msal-browser';
import { BehaviorSubject, filter, Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _isAuthenticated = new BehaviorSubject<boolean>(false);
  readonly isAuthenticated$: Observable<boolean> = this._isAuthenticated.asObservable();

  constructor(
    @Inject(MSAL_INSTANCE) private readonly msalInstance: IPublicClientApplication,
    @Optional() @Inject(MSAL_GUARD_CONFIG) private readonly guardConfig: MsalGuardConfiguration,
    private readonly msalService: MsalService,
    private readonly broadcastService: MsalBroadcastService,
  ) {}

  login(): void {
    if (this.guardConfig?.authRequest) {
      this.msalService.loginRedirect({ ...this.guardConfig.authRequest } as RedirectRequest).subscribe();
    } else {
      this.msalService.loginRedirect().subscribe();
    }
  }

  loginPopup(): void {
    if (this.guardConfig?.authRequest) {
      this.msalService.loginPopup({ ...this.guardConfig.authRequest } as PopupRequest).subscribe();
    } else {
      this.msalService.loginPopup().subscribe();
    }
  }

  logout(): void {
    this.msalService.logoutRedirect().subscribe();
  }

  trackAuthenticationStatus(): Observable<InteractionStatus> {
    return this.broadcastService.inProgress$.pipe(
      filter((status: InteractionStatus) => status === InteractionStatus.None),
    );
  }

  get activeAccount() {
    return this.msalInstance.getActiveAccount() ?? this.msalInstance.getAllAccounts()[0];
  }

  refreshAuthenticationState(): void {
    this._isAuthenticated.next(this.msalInstance.getAllAccounts().length > 0);
  }

  getAccessToken(): Promise<string | null> {
    const account = this.activeAccount;
    if (!account) {
      return Promise.resolve(null);
    }
    return this.msalInstance
      .acquireTokenSilent({ scopes: ['openid', 'profile', 'offline_access', environment.msal.apiScope], account })
      .then((result) => result.accessToken)
      .catch(() => null);
  }
}