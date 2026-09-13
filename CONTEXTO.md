# 📑 CONTEXTO TÁCTICO Y ARQUITECTURA: @CLOUD_NATIVE (Pedidos360)

```yaml
context_version: "1.0"
project_name: "Pedidos360 - Cloud Native"
stack:
  frontend: "Angular (SPA) + MSAL v3 (@azure/msal-browser, @azure/msal-angular)"
  backend: "Java Spring Boot + Spring Security OAuth2 Resource Server"
  api_gateway: "AWS API Gateway (HTTP/REST APIs) + JWT Authorizer + CORS"
  idaas: "Microsoft Entra External ID (Azure AD B2C / External ID)"
evaluation_weights:
  frontend_msal: "60%"
  backend_api_gateway_bff: "40%"
```

---

## 1. REQUISITOS Y PAUTA DE EVALUACIÓN

### Frontend (60%)
- **Integración MSAL:** `@azure/msal-browser` y `@azure/msal-angular` operativos.
- **Flujos:** Inicio y cierre de sesión (`loginRedirect`, `logoutRedirect`).
- **Seguridad:** `MsalGuard` en rutas, `MsalInterceptor` inyecta token Bearer en peticiones HTTP automáticas.
- **Claims/Roles:** Obtención y lectura de tokens, roles y scopes desde los claims.

### Backend & API Gateway / BFF (40%)
- **JWT Authorizer:** API Gateway valida firma del JWT, `issuer` (`iss`) y `audience` (`aud`).
- **Spring Boot:** `oauth2ResourceServer` valida JWT entrante o pasa por API Gateway.
- **CORS:** Configurado en API Gateway para permitir solicitudes desde el Frontend (`http://localhost:4200`).
- **Versionamiento:** Rutas `/v1/...` y `/v2/...` coexistiendo en el API Gateway.

---

## 2. IDaaS: CONFIGURACIÓN EN AZURE ENTRA ID (EXTERNAL ID)

### A. API Backend (`Backend-API`)
1. Registrar App -> *Solo cuentas de este directorio organizativo*.
2. **Exponer una API:**
   - Application ID URI: `api://<backend-client-id>`
   - Scopes: `api://<backend-client-id>/read`, `api://<backend-client-id>/write`

### B. SPA Frontend (`Frontend-SPA`)
1. Registrar App -> Plataforma **Single-Page Application (SPA)**.
2. Redirect URIs: Login `http://localhost:4200` | Logout `http://localhost:4200/`.
3. **Permisos de API:** Añadir permisos de la API del Backend (`read`, `write`).
4. **Grant admin consent:** Conceder consentimiento de administrador para el tenant.

### C. External Identities (User Flow)
- Crear flujo de usuario *Sign up and Sign in* en Entra External ID.
- Configurar atributos de usuario (Given Name, Surname, Email, City, etc.).

---

## 3. FRONTEND: ANGULAR + MSAL CONFIGURACIÓN TÉCNICA

### Dependencias
`npm install @azure/msal-browser @azure/msal-angular`

### `src/environments/environment.ts`
```typescript
export const environment = {
  production: false,
  azure: {
    clientId: 'FRONTEND_CLIENT_ID',
    tenantId: 'TENANT_ID',
    authority: 'https://login.microsoftonline.com/TENANT_ID',
    redirectUri: 'http://localhost:4200',
    protectedResourceScopes: ['api://BACKEND_CLIENT_ID/read']
  },
  apiBaseUrl: 'http://localhost:8080' // O endpoint de AWS API Gateway
};
```

### Configuración MSAL (`src/app/msal-config.ts`)
```typescript
import { PublicClientApplication, IPublicClientApplication } from '@azure/msal-browser';
import { MsalInterceptorConfiguration, InteractionType } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: environment.azure.authority,
      redirectUri: environment.azure.redirectUri
    },
    cache: { cacheLocation: 'localStorage' }
  });
}

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>([
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes]
  ]);
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
```

### Main Standalone (`src/main.ts`)
```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MSAL_INSTANCE, MSAL_INTERCEPTOR_CONFIG, MsalInterceptor, MsalGuard, MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { provideRouter, Routes } from '@angular/router';
import { msalInstanceFactory, msalInterceptorConfigFactory } from './app/msal-config';

const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [MsalGuard] },
  { path: '**', redirectTo: '' }
];

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    MsalGuard, MsalService, MsalBroadcastService
  ]
});
```

### Componente Principal (`src/app/app.component.ts`)
```typescript
import { Component } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

@Component({ selector: 'app-root', templateUrl: './app.component.html' })
export class AppComponent {
  constructor(private msal: MsalService) {}
  isLoggedIn(): boolean { return this.msal.instance.getAllAccounts().length > 0; }
  login(): void { this.msal.loginRedirect(); }
  logout(): void { this.msal.logoutRedirect(); }
}
```

---

## 4. AWS API GATEWAY & CORS & VERSIONAMIENTO

### CORS
- **Allowed Origins:** `http://localhost:4200`
- **Allowed Headers:** `Authorization`, `Content-Type`, `X-Amz-Date`, `X-Api-Key`
- **Allowed Methods:** `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

### JWT Authorizer (Nativo HTTP API)
- **Identity Source:** `$request.header.Authorization`
- **Issuer:** `https://<tenant-name>.ciamlogin.com/<tenant-id>/v2.0/`
- **Audience:** `<BACKEND_CLIENT_ID>` o `<FRONTEND_CLIENT_ID>`

### Rutas y Versionamiento
- **`/v1/datos` (GET):** Integración HTTP a microservicio v1 (ej. `https://mindicador.cl/api`).
- **`/v2/datos` (GET):** Integración HTTP a microservicio v2 (ej. `https://jsonplaceholder.typicode.com/todos/`).

---

## 5. BACKEND: SPRING BOOT RESOURCE SERVER
- Dependencia: `spring-boot-starter-oauth2-resource-server`
- `application.yml`:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: https://login.microsoftonline.com/<TENANT_ID>/v2.0
  ```
- Configurar SecurityFilterChain para validar el Bearer JWT y autorizar solicitudes según claims.
