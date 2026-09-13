import { Routes } from '@angular/router';
import { MsalGuard, MsalRedirectComponent } from '@azure/msal-angular';

export const routes: Routes = [
  {
    path: 'auth',
    component: MsalRedirectComponent,
  },
  {
    path: '',
    loadComponent: () => import('./home/home').then((m) => m.Home),
  },
  {
    path: 'perfil',
    canActivate: [MsalGuard],
    loadComponent: () => import('./perfil/perfil').then((m) => m.Perfil),
  },
  {
    path: '**',
    redirectTo: '',
  },
];