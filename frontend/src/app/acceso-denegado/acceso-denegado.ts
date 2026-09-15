import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Destino de MsalGuard cuando el login falla o se cancela (loginFailedRoute). */
@Component({
  imports: [RouterLink],
  selector: 'app-acceso-denegado',
  template: `
    <div class="empty panel" style="max-width: 36rem; margin: 2rem auto; text-align: left">
      <p class="kicker">Acceso denegado</p>
      <h1 style="margin-top: 0">No pudimos iniciar tu sesión</h1>
      <p class="muted">
        El inicio de sesión con Microsoft Entra ID fue cancelado o rechazado. Solo cuentas del
        directorio institucional pueden acceder a las secciones privadas.
      </p>
      <div class="actions">
        <button type="button" class="btn btn-primary" (click)="auth.login()">Intentar de nuevo</button>
        <a class="btn btn-ghost" routerLink="/">Volver al inicio</a>
      </div>
    </div>
  `,
})
export class AccesoDenegado {
  protected readonly auth = inject(AuthService);
}
