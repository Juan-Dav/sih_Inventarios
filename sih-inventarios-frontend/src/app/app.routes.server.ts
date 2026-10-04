import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Los paneles dependen del token que vive en localStorage, y el prerender no
  // tiene navegador ni almacenamiento. Se renderizan en cliente.
  //
  // El patrón lleva `/**` a propósito: cada módulo es una ruta hija
  // (`dashboard-admin/salas`), y sin el comodín solo quedaría cubierta la ruta
  // padre, que además redirige y no se prerenderiza.
  {
    path: 'dashboard-docente/**',
    renderMode: RenderMode.Client,
  },
  {
    path: 'dashboard-tecnico/**',
    renderMode: RenderMode.Client,
  },
  {
    path: 'dashboard-admin/**',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
