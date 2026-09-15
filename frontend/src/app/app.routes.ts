import { Routes } from '@angular/router';
import { MsalGuard, MsalRedirectComponent } from '@azure/msal-angular';

export const routes: Routes = [
  {
    // Callback de Microsoft Entra ID (redirectUri). MSAL procesa el código y vuelve a la página original.
    path: 'auth',
    component: MsalRedirectComponent,
  },
  {
    path: '',
    title: 'NexoTech · Tecnología',
    loadComponent: () => import('./home/home').then((m) => m.Home),
  },
  {
    path: 'catalogo',
    title: 'Tienda · NexoTech',
    loadComponent: () => import('./catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'catalogo/:id',
    title: 'Producto · NexoTech',
    loadComponent: () => import('./producto/producto').then((m) => m.Producto),
  },
  {
    path: 'carrito',
    title: 'Carrito · NexoTech',
    canActivate: [MsalGuard],
    loadComponent: () => import('./carrito/carrito').then((m) => m.Carrito),
  },
  {
    path: 'compras',
    title: 'Mis compras · NexoTech',
    canActivate: [MsalGuard],
    loadComponent: () => import('./compras/compras').then((m) => m.Compras),
  },
  {
    path: 'notificaciones',
    title: 'Avisos · NexoTech',
    canActivate: [MsalGuard],
    loadComponent: () => import('./notificaciones/notificaciones').then((m) => m.Notificaciones),
  },
  {
    path: 'perfil',
    title: 'Perfil · NexoTech',
    canActivate: [MsalGuard],
    loadComponent: () => import('./perfil/perfil').then((m) => m.Perfil),
  },
  {
    path: 'admin',
    title: 'Administración · NexoTech',
    canActivate: [MsalGuard],
    loadComponent: () => import('./admin/admin').then((m) => m.Admin),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado · NexoTech',
    loadComponent: () => import('./acceso-denegado/acceso-denegado').then((m) => m.AccesoDenegado),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
