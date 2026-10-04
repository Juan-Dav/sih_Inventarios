import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ETIQUETA_ROL, Rol } from '../../models/sesion.model';
import { AuthService } from '../../services/auth.service';

/** Entrada del menú lateral del panel. */
export interface MenuItem {
  label: string;
  descripcion: string;
  /** Ruta absoluta dentro del panel, tal y como se la pasa a `routerLink`. */
  ruta: string;
  /** Clave de `ICONOS`. */
  icon: string;
  /** Cuántas entradas del módulo hay, para pintarlo como contador. */
  insignia?: string;
}

/**
 * Iconos de Heroicons en trazo, guardados como `path` para poder cambiar de
 * icono sin cambiar de componente.
 */
export const ICONOS: Record<string, string> = {
  panel: 'M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z',
  aula:
    'M3.75 21h16.5M4.5 3h15M5.25 3v18m13.5-18v18M9 6.75h1.5m-1.5 3h1.5m-1.5 3h1.5m3-6H15m-1.5 3H15m-1.5 3H15M9 21v-3.375c0-.621.504-1.125 1.125-1.125h3.75c.621 0 1.125.504 1.125 1.125V21',
  equipo:
    'M8.25 3v1.5M4.5 8.25H3m18 0h-1.5M4.5 12H3m18 0h-1.5m-15 3.75H3m18 0h-1.5M8.25 19.5V21M12 3v1.5m0 15V21m3.75-18v1.5m0 15V21m-9-1.5h7.5a2.25 2.25 0 002.25-2.25V6.75a2.25 2.25 0 00-2.25-2.25H6.75A2.25 2.25 0 004.5 6.75v8.25a2.25 2.25 0 002.25 2.25z',
  falla: 'M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z',
  mantenimiento:
    'M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 11-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 004.486-6.336l-3.276 3.277a3.004 3.004 0 01-2.25-2.25l3.276-3.276a4.5 4.5 0 00-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085m-1.745 1.437L5.909 7.5H4.5L2.25 3h1.5l1.5 4.5 2.25 4.5 1.5 4.5h1.5',
  usuario:
    'M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z',
  reporte:
    'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z',
  ajustes:
    'M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.28zM15 12a3 3 0 11-6 0 3 3 0 016 0z',
  /* Tarjeta con escudo: el del plan de pago. */
  plan: 'M9 12.75L11.25 15 15 9.75M9 3.75l-6 2.25v6c0 3.75 2.5 7.125 6 8.25 3.5-1.125 6-4.5 6-8.25V6L9 3.75z',
  salir: 'M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75',
  volver: 'M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18',
};

/** Clases de tema oscuro que se aplican al contenedor raíz de cada panel. */
const TEMA_POR_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'tema-admin',
  TECNICO: 'tema-tecnico',
  DOCENTE: 'tema-docente',
};

/**
 * Marco común de los tres paneles.
 *
 * Aporta la barra superior fija con la sesión, la barra lateral con el menú del
 * rol y el `router-outlet` donde cada módulo se dibuja.
 *
 * El tema se deriva del rol en vez de recibirse como entrada: los tres paneles
 * comparten este componente y no hacía falta que los tres volvieran a declarar
 * la misma paleta.
 */
@Component({
  selector: 'app-dashboard-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './dashboard-shell.html',
  styleUrl: './dashboard-shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardShell {
  protected readonly auth = inject(AuthService);
  protected readonly iconos = ICONOS;

  readonly titulo = input.required<string>();
  readonly subtitulo = input.required<string>();
  readonly rol = input.required<Rol>();
  readonly menu = input.required<readonly MenuItem[]>();

  /** Tema oscuro del rol: base, halos de fondo y color de acento. */
  protected readonly tema = computed(() => TEMA_POR_ROL[this.rol()]);

  /** Clases del degradado del rol, aplicado en el encabezado y al ítem activo. */
  readonly gradiente = input.required<string>();
  /**
   * Degradado sólido del rol para los botones de acción.
   *
   * Va aparte del `gradiente` porque ese se usa en superficies grandes, donde
   * un degradado fuerte satura el texto, y en los botones hace falta que el
   * color sea plano para que la etiqueta se lea.
   */
  readonly gradienteSolido = input.required<string>();
  /** Clases del texto/borde de acento del rol. */
  readonly acento = input.required<string>();
  /** Clases del halo de color de fondo; va más saturado que el acento. */
  readonly halo = input.required<string>();
  /**
   * Módulos que todavía no existen.
   *
   * Se listan como texto sin botón a propósito: un enlace que no lleva a
   * ninguna parte promete una función que no tiene.
   */
  readonly proximos = input<readonly string[]>([]);

  protected readonly etiquetaRol = ETIQUETA_ROL;

  /** Iniciales para el avatar de la cabecera. */
  protected readonly iniciales = computed(() => {
    const nombre = this.auth.usuario()?.nombre?.trim() ?? '';
    if (!nombre) return '?';

    return nombre
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase() ?? '')
      .join('');
  });

  protected cerrarSesion(): void {
    this.auth.logout();
  }
}
