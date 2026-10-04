import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ICONOS } from '../../layout/dashboard-shell/dashboard-shell';

/**
 * Tarjeta de indicador para la cabecera de los módulos.
 *
 * Existe como componente y no como markup repetido porque las cuatro páginas
 * de los paneles muestran las mismas cifras con el mismo formato, y el color
 * lo decide quien la usa.
 */
@Component({
  selector: 'app-kpi',
  template: `
    <div
      class="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-5 shadow-sm backdrop-blur-md transition-shadow duration-300 hover:shadow-lg"
    >
      <!-- Franja de color en el canto superior -->
      <span
        class="absolute inset-x-0 top-0 h-1 opacity-90 transition-opacity duration-300 group-hover:opacity-100"
        [class]="gradiente()"
      ></span>

      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <p class="text-[0.6rem] font-bold uppercase tracking-[0.18em] text-ink-500">
            {{ etiqueta() }}
          </p>
          <p class="mt-2 font-display text-3xl font-black leading-none text-ink-900">
            {{ valor() }}
          </p>
          @if (detalle()) {
            <p class="mt-1.5 text-xs font-medium text-ink-500">{{ detalle() }}</p>
          }
        </div>

        <span
          class="grid h-11 w-11 shrink-0 place-items-center rounded-2xl transition-transform duration-300 group-hover:scale-110"
          [class]="iconoClases()"
        >
          <svg
            class="h-5 w-5"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" [attr.d]="iconos[icono()]" />
          </svg>
        </span>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Kpi {
  protected readonly iconos = ICONOS;

  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly detalle = input<string>('');
  readonly icono = input.required<string>();
  /** Clases del degradado de la franja superior. */
  readonly gradiente = input.required<string>();
  /** Clases del contenedor del icono. */
  readonly iconoClases = input.required<string>();
}
