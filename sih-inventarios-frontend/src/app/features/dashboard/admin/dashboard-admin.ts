import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DashboardShell, MenuItem } from '../../../core/layout/dashboard-shell/dashboard-shell';

/**
 * Marco del panel del administrador.
 *
 * Solo vive el menú: el módulo que se dibuja dentro lo decide el router, así
 * que añadir otra pantalla después es agregar una ruta hija y una entrada más.
 */
@Component({
  selector: 'app-dashboard-admin',
  imports: [DashboardShell],
  template: `
    <app-dashboard-shell
      titulo="Panel de administración"
      subtitulo="Controla las aulas, los equipos y el estado general de la plataforma."
      rol="ADMINISTRADOR"
      [menu]="menu"
      [proximos]="proximos"
      gradiente="bg-gradient-to-br from-brand-600 to-pop-600"
      gradienteSolido="bg-gradient-to-r from-brand-600 to-pop-600 shadow-lg shadow-brand-600/25 hover:shadow-brand-600/40"
      acento="txt-acento"
      halo="bg-brand-500/10 txt-acento ring-brand-400/40"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardAdmin {
  protected readonly menu: readonly MenuItem[] = [
    {
      label: 'Aulas y equipos',
      descripcion: 'Registra las aulas, da de alta sus equipos y consulta el conteo.',
      ruta: '/dashboard-admin/salas',
      icon: 'aula',
    },
    {
      label: 'Inventario general',
      descripcion: 'El estado de todas las aulas: qué está operativo y qué no.',
      ruta: '/dashboard-admin/inventario',
      icon: 'equipo',
    },
    {
      label: 'Cuentas y plan',
      descripcion: 'Crea cuentas, asigna roles y decide hasta dónde llega el plan gratuito.',
      ruta: '/dashboard-admin/usuarios',
      icon: 'panel',
    },
    {
      label: 'Historial',
      descripcion: 'Revisa las fallas y los mantenimientos de cada equipo.',
      ruta: '/dashboard-admin/historial',
      icon: 'reporte',
    },
  ];

  protected readonly proximos: readonly string[] = ['Reportes consolidados'];
}