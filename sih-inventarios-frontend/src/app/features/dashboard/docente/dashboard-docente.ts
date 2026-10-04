import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DashboardShell, MenuItem } from '../../../core/layout/dashboard-shell/dashboard-shell';

/**
 * Marco del panel del docente.
 *
 * El docente consulta, registra lo que ve en el aula y reporta fallas. Da de
 * alta aulas y equipos porque es quien llega primero y ya los tiene delante: lo
 * que no hace es cambiar el estado de una máquina ni atender una falla, que es
 * decisión de mantenimiento del técnico y de la administración, y el backend
 * también se lo rechaza.
 */
@Component({
  selector: 'app-dashboard-docente',
  imports: [DashboardShell],
  template: `
    <app-dashboard-shell
      titulo="Panel del docente"
      subtitulo="Consulta dónde está cada equipo y reporta las fallas que encuentres."
      rol="DOCENTE"
      [menu]="menu"
      [proximos]="proximos"
      gradiente="bg-gradient-to-br from-aqua-500 to-brand-600"
      gradienteSolido="bg-gradient-to-r from-aqua-500 to-brand-600 shadow-lg shadow-aqua-500/25 hover:shadow-aqua-500/40"
      acento="txt-acento"
      halo="bg-aqua-500/10 txt-acento ring-aqua-500/30"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardDocente {
  protected readonly menu: readonly MenuItem[] = [
    {
      label: 'Aulas y equipos',
      descripcion: 'Registra las aulas y los equipos que hay en cada una.',
      ruta: '/dashboard-docente/inventario',
      icon: 'aula',
    },
    {
      label: 'Reportar falla',
      descripcion: 'Da de alta una falla y revisa las que ya has reportado.',
      ruta: '/dashboard-docente/fallas',
      icon: 'falla',
    },
    {
      label: 'Seguimiento',
      descripcion: 'Las fallas y los mantenimientos de cada equipo.',
      ruta: '/dashboard-docente/historial',
      icon: 'reporte',
    },
    {
      label: 'Planes',
      descripcion: 'Contrata o cancela el Premium del sistema.',
      ruta: '/dashboard-docente/planes',
      icon: 'plan',
    },
  ];

  protected readonly proximos: readonly string[] = ['Mis reportes'];
}