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
    path: 'catalogo',
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'catalogo/:id',
    loadComponent: () => import('./producto/producto').then((m) => m.Producto),
  },
  {
    path: 'carrito',
    canActivate: [MsalGuard],
    loadComponent: () => import('./carrito/carrito').then((m) => m.Carrito),
  },
  {
    path: 'compras',
    canActivate: [MsalGuard],
    loadComponent: () => import('./compras/compras').then((m) => m.Compras),
  },
  {
    path: 'notificaciones',
    canActivate: [MsalGuard],
    loadComponent: () => import('./notificaciones/notificaciones').then((m) => m.Notificaciones),
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
