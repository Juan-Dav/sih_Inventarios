import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DashboardShell, MenuItem } from '../../../core/layout/dashboard-shell/dashboard-shell';

/**
 * Marco del panel del técnico.
 *
 * El técnico atiende lo que reportan los docentes y además da de alta y
 * mantiene el inventario, así que aquí el módulo de aulas y equipos va con
 * formularios y edición habilitadas.
 */
@Component({
  selector: 'app-dashboard-tecnico',
  imports: [DashboardShell],
  template: `
    <app-dashboard-shell
      titulo="Mantenimiento"
      subtitulo="Atiende las fallas reportadas y deja registrado cada mantenimiento."
      rol="TECNICO"
      [menu]="menu"
      [proximos]="proximos"
      gradiente="bg-gradient-to-br from-mint-500 to-aqua-500"
      gradienteSolido="bg-gradient-to-r from-mint-500 to-aqua-500 shadow-lg shadow-mint-500/25 hover:shadow-mint-500/40"
      acento="txt-ok"
      halo="bg-mint-500/10 txt-ok ring-mint-500/30"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardTecnico {
  protected readonly menu: readonly MenuItem[] = [
    {
      label: 'Fallas y mantenimiento',
      descripcion: 'Fallas reportadas por los docentes y registro de la solución.',
      ruta: '/dashboard-tecnico/fallas',
      icon: 'mantenimiento',
    },
    {
      label: 'Verificación de aulas',
      descripcion: 'Cuenta los equipos de cada aula y detecta los que faltan.',
      ruta: '/dashboard-tecnico/verificacion',
      icon: 'ajustes',
    },
    {
      label: 'Aulas y equipos',
      descripcion: 'Registra aulas nuevas y da de alta o edita sus equipos.',
      ruta: '/dashboard-tecnico/inventario',
      icon: 'aula',
    },
    {
      label: 'Historial',
      descripcion: 'Antecedente de fallas y mantenimientos de cada equipo.',
      ruta: '/dashboard-tecnico/historial',
      icon: 'reporte',
    },
    {
      label: 'Planes',
      descripcion: 'Contrata o cancela el Premium del sistema.',
      ruta: '/dashboard-tecnico/planes',
      icon: 'plan',
    },
  ];

  protected readonly proximos: readonly string[] = ['Reportes de mantenimiento'];
}