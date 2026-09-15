import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import {
  BrowserCacheLocation,
  Configuration,
  InteractionType,
  IPublicClientApplication,
  LogLevel,
  PublicClientApplication,
} from '@azure/msal-browser';
import { environment } from '../../environments/environment';

/** Scopes que pide la SPA: OIDC básicos + el scope delegado de la API NexoTech. */
export const loginScopes = ['openid', 'profile', 'offline_access', environment.msal.apiScope];

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msal.clientId,
      authority: environment.msal.authority,
      redirectUri: environment.msal.redirectUri,
      postLogoutRedirectUri: environment.msal.postLogoutRedirectUri,
      // Tras procesar el callback en /auth, MSAL vuelve por defecto a la página donde se inició el login.
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
    system: {
      loggerOptions: {
        logLevel: environment.production ? LogLevel.Warning : LogLevel.Info,
        piiLoggingEnabled: false,
        loggerCallback: (level, message) => {
          if (level <= LogLevel.Warning) {
            console.warn('[MSAL]', message);
          }
        },
      },
    },
  } satisfies Configuration);
}

const apiScopes = [environment.msal.apiScope];

/**
 * Recursos a los que el MsalInterceptor adjunta el token.
 *
 * msal-angular 6 usa coincidencia estricta (patrón anclado al inicio y al fin),
 * por lo que se requieren comodines `*` para cubrir sub-rutas como
 * /api/cart/items o /api/notifications/unread-count.
 * Las rutas /api/public/** no están listadas: son anónimas.
 */
export const protectedResourceMap = new Map<string, Array<string>>([
  [`${environment.backendApiUrl}/me`, apiScopes],
  [`${environment.backendApiUrl}/cart*`, apiScopes],
  [`${environment.backendApiUrl}/orders*`, apiScopes],
  [`${environment.backendApiUrl}/notifications*`, apiScopes],
  [`${environment.backendApiUrl}/products/*`, apiScopes],
  [`${environment.backendApiUrl}/payments/*`, apiScopes],
  [`${environment.backendApiUrl}/shipping/*`, apiScopes],
  [`${environment.backendApiUrl}/admin/*`, apiScopes],
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
      scopes: loginScopes,
    },
    loginFailedRoute: '/acceso-denegado',
  };
}
