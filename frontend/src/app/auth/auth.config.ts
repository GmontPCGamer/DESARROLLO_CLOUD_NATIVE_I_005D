import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { BrowserCacheLocation, Configuration, InteractionType, IPublicClientApplication, PublicClientApplication } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msal.clientId,
      authority: environment.msal.authority,
      redirectUri: environment.msal.redirectUri,
      postLogoutRedirectUri: environment.msal.postLogoutRedirectUri,
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
  } satisfies Configuration);
}

const apiScopes = [environment.msal.apiScope];

export const protectedResourceMap = new Map<string, Array<string>>([
  [`${environment.backendApiUrl}/cart`, apiScopes],
  [`${environment.backendApiUrl}/cart/`, apiScopes],
  [`${environment.backendApiUrl}/orders`, apiScopes],
  [`${environment.backendApiUrl}/orders/`, apiScopes],
  [`${environment.backendApiUrl}/notifications`, apiScopes],
  [`${environment.backendApiUrl}/notifications/`, apiScopes],
  [`${environment.backendApiUrl}/me`, apiScopes],
  [`${environment.backendApiUrl}/products/`, apiScopes],
  [`${environment.backendApiUrl}/payments/`, apiScopes],
  [`${environment.backendApiUrl}/shipping/`, apiScopes],
]);

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: ['openid', 'profile', 'offline_access', environment.msal.apiScope],
    },
    loginFailedRoute: '/acceso-denegado',
  };
}