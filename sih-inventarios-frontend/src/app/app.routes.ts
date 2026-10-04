import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

/**
 * Cada panel es una ruta padre que monta el marco con su menú lateral, y sus
 * módulos son rutas hijas. Así cada módulo tiene su propia URL: se puede
 * recargar, compartir el enlace y usar el botón de atrás del navegador.
 *
 * `canActivate` protege el marco y `canActivateChild` cada módulo, para que no
 * se pueda saltar el control escribiendo la URL del módulo directamente.
 * `roleGuard` además manda al usuario a su propio panel si el rol no es el
 * correcto, en vez de dejarlo viendo una pantalla vacía.
 *
 * Los `loadComponent` son perezosos: el código de un panel no se descarga hasta
 * que alguien entra a él.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage),
  },
  {
    path: 'dashboard-docente',
    canActivate: [authGuard, roleGuard(['DOCENTE'])],
    canActivateChild: [roleGuard(['DOCENTE'])],
    loadComponent: () =>
      import('./features/dashboard/docente/dashboard-docente').then((m) => m.DashboardDocente),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inventario' },
      {
        path: 'inventario',
        title: 'Aulas y equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/salas-equipos').then((m) => m.SalasEquipos),
        data: {
          // El docente también da de alta aulas y equipos: es quien está en el
          // sitio, recibe la máquina y la documenta. Hacerlo esperar al técnico
          // o a la administración convertía el inventario en trabajo a medias.
          // Editar no: una vez registrado, el registro se corrige entre técnico
          // y administración, así que aquí va en `false` y el backend rechaza
          // el PUT con 403. Tampoco puede cambiar el estado de un equipo ni
          // atender una falla.
          permiteCrear: true,
          permiteEditar: false,
          gradiente: 'bg-gradient-to-br from-aqua-500 to-brand-600',
          gradienteSolido:
            'bg-gradient-to-r from-aqua-500 to-brand-600 shadow-lg shadow-aqua-500/25 hover:shadow-aqua-500/40',
        },
      },
      {
        path: 'fallas',
        title: 'Reportar falla',
        loadComponent: () =>
          import('./features/dashboard/docente/fallas-docente').then((m) => m.FallasDocente),
      },
      {
        path: 'historial',
        title: 'Historial de equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/historial-equipos').then((m) => m.HistorialEquipos),
      },
      {
        path: 'planes',
        title: 'Planes',
        loadComponent: () => import('./features/dashboard/shared/planes').then((m) => m.Planes),
      },
    ],
  },
  {
    path: 'dashboard-tecnico',
    canActivate: [authGuard, roleGuard(['TECNICO', 'ADMINISTRADOR'])],
    canActivateChild: [roleGuard(['TECNICO', 'ADMINISTRADOR'])],
    loadComponent: () =>
      import('./features/dashboard/tecnico/dashboard-tecnico').then((m) => m.DashboardTecnico),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'fallas' },
      {
        path: 'fallas',
        title: 'Fallas y mantenimiento',
        loadComponent: () =>
          import('./features/dashboard/tecnico/fallas-tecnico').then((m) => m.FallasTecnico),
      },
      {
        path: 'verificacion',
        title: 'Verificación de aulas',
        loadComponent: () =>
          import('./features/dashboard/tecnico/verificacion').then((m) => m.VerificacionAulas),
      },
      {
        path: 'inventario',
        title: 'Aulas y equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/salas-equipos').then((m) => m.SalasEquipos),
        data: {
          // El técnico da de alta aulas y equipos igual que la administración:
          // es quien los instala y deja funcionando, además de mantener los que
          // ya existen. Los límites del plan los aplica el backend igual que a
          // cualquier otro rol.
          permiteCrear: true,
          permiteEditar: true,
          permiteCambiarEstado: true,
          gradiente: 'bg-gradient-to-br from-mint-500 to-aqua-500',
          gradienteSolido:
            'bg-gradient-to-r from-mint-500 to-aqua-500 shadow-lg shadow-mint-500/25 hover:shadow-mint-500/40',
        },
      },
      {
        path: 'historial',
        title: 'Historial de equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/historial-equipos').then((m) => m.HistorialEquipos),
      },
      {
        path: 'planes',
        title: 'Planes',
        loadComponent: () => import('./features/dashboard/shared/planes').then((m) => m.Planes),
      },
    ],
  },
  {
    path: 'dashboard-admin',
    canActivate: [authGuard, roleGuard(['ADMINISTRADOR'])],
    canActivateChild: [roleGuard(['ADMINISTRADOR'])],
    loadComponent: () =>
      import('./features/dashboard/admin/dashboard-admin').then((m) => m.DashboardAdmin),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'salas' },
      {
        path: 'salas',
        title: 'Aulas y equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/salas-equipos').then((m) => m.SalasEquipos),
        data: {
          // La administración da de alta aulas y equipos igual que el técnico.
          permiteCrear: true,
          permiteEditar: true,
          permiteCambiarEstado: true,
          gradiente: 'bg-gradient-to-br from-brand-600 to-pop-600',
          gradienteSolido:
            'bg-gradient-to-r from-brand-600 to-pop-600 shadow-lg shadow-brand-600/25 hover:shadow-brand-600/40',
        },
      },
      {
        // Foto del inventario de toda la institución, con el detalle de lo que
        // está fuera de servicio y de las fallas que siguen abiertas.
        path: 'inventario',
        title: 'Inventario general',
        loadComponent: () =>
          import('./features/dashboard/admin/inventario-general').then((m) => m.InventarioGeneral),
      },
      {
        path: 'usuarios',
        title: 'Cuentas y plan',
        loadComponent: () =>
          import('./features/dashboard/admin/usuarios-plan').then((m) => m.UsuariosPlan),
      },
      {
        path: 'historial',
        title: 'Historial de equipos',
        loadComponent: () =>
          import('./features/dashboard/shared/historial-equipos').then((m) => m.HistorialEquipos),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
