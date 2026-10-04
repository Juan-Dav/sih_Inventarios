import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';

import {
  ResumenInventario,
  formatearFecha,
  nombreEquipo,
} from '../../../core/models/inventario.model';
import { InventarioService } from '../../../core/services/inventario.service';
import { Kpi } from '../../../core/ui/kpi/kpi';

/**
 * Inventario general de la institución.
 *
 * Es la vista que responde "¿cómo está el inventario?" en una sola pantalla:
 * cuántas aulas hay, cómo se reparten los equipos por estado y qué está
 * pendiente. El administrador la usa para decidir, no para registrar; lo que se
 * registra está en el módulo de aulas y equipos.
 *
 * Los números no se cuentan en el navegador a partir de las listas: los pide
 * al backend con `GET /api/inventario/resumen`. Así el conteo por estado sale
 * de la base de datos y no de lo que se haya cargado en pantalla.
 */
@Component({
  selector: 'app-inventario-general',
  imports: [Kpi],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <section class="tarjeta">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="titulo-modulo">Inventario general</h2>
            <p class="subtitulo-modulo">
              El estado de todas las aulas y todos los equipos de la institución.
            </p>
          </div>

          <button
            type="button"
            class="botón boton-acento"
            [disabled]="cargando()"
            (click)="cargar()"
          >
            {{ cargando() ? 'Consultando…' : 'Actualizar' }}
          </button>
        </div>

        @if (error(); as mensaje) {
          <p class="aviso mt-4 bg-pop-500/15 txt-alerta">{{ mensaje }}</p>
        }
      </section>

      @if (resumen(); as datos) {
        <!-- ============ Indicadores ============ -->
        <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <app-kpi
            etiqueta="Aulas registradas"
            [valor]="datos.totalSalas"
            [detalle]="datos.totalSalas === 1 ? 'El plan gratuito permite una' : 'Sin límite en Premium'"
            icono="aula"
            gradiente="bg-gradient-to-r from-brand-500 to-brand-600"
            iconoClases="bg-brand-500/15 text-brand-300"
          />
          <app-kpi
            etiqueta="Equipos operativos"
            [valor]="datos.equiposOperativos"
            [detalle]="operativosDetalle()"
            icono="equipo"
            gradiente="bg-gradient-to-r from-mint-400 to-mint-500"
            iconoClases="bg-mint-400/15 txt-ok"
          />
          <app-kpi
            etiqueta="Fuera de servicio"
            [valor]="datos.equiposFueraDeServicio"
            [detalle]="datos.equiposEnMantenimiento + ' en mantenimiento'"
            icono="mantenimiento"
            gradiente="bg-gradient-to-r from-pop-400 to-pop-600"
            iconoClases="bg-pop-500/15 txt-alerta"
          />
          <app-kpi
            etiqueta="Fallas abiertas"
            [valor]="fallasAbiertas()"
            [detalle]="datos.totalFallas + ' reportadas en total'"
            icono="falla"
            gradiente="bg-gradient-to-r from-sun-400 to-sun-500"
            iconoClases="bg-sun-400/15 txt-espera"
          />
        </section>

        <!-- ============ Aulas ============ -->
        <section class="tarjeta">
          <h2 class="titulo-modulo">Aulas</h2>
          <p class="subtitulo-modulo">Cómo se reparten los equipos en cada aula.</p>

          @if (datos.salas.length === 0) {
            <div class="vacio">
              <h3>Todavía no hay aulas</h3>
              <p>Registra la primera aula para empezar a llevar el inventario.</p>
            </div>
          } @else {
            <div class="tabla-envoltura mt-5">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Aula</th>
                    <th>Equipos</th>
                    <th>Operativos</th>
                    <th>En mantenimiento</th>
                    <th>Fuera de servicio</th>
                    <th>Último conteo</th>
                    <th>Faltantes</th>
                  </tr>
                </thead>
                <tbody>
                  @for (sala of datos.salas; track sala.salaId) {
                    <tr>
                      <td class="font-semibold text-ink-900">{{ sala.sala }}</td>
                      <td>{{ sala.equiposRegistrados }}</td>
                      <td class="txt-ok">{{ sala.equiposOperativos }}</td>
                      <td class="txt-espera">{{ sala.equiposEnMantenimiento }}</td>
                      <td class="txt-alerta">{{ sala.equiposFueraDeServicio }}</td>
                      <td class="whitespace-nowrap">
                        @if (sala.ultimaVerificacion) {
                          {{ fecha(sala.ultimaVerificacion) }}
                        } @else {
                          <span class="text-ink-500">Sin recorrer</span>
                        }
                      </td>
                      <td>
                        @if (sala.equiposFaltantes === null) {
                          <span class="text-ink-500">—</span>
                        } @else if (sala.equiposFaltantes > 0) {
                          <span class="font-bold txt-alerta">{{ sala.equiposFaltantes }}</span>
                        } @else {
                          <span class="txt-ok">0</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <div class="grid gap-6 xl:grid-cols-2">
          <!-- ============ Fuera de servicio ============ -->
          <section class="tarjeta">
            <h2 class="titulo-modulo">Equipos fuera de servicio</h2>
            <p class="subtitulo-modulo">Los que el técnico marcó como no recuperables en aula.</p>

            @if (datos.listaEquiposFueraDeServicio.length === 0) {
              <div class="vacio">
                <h3>Ninguno por ahora</h3>
                <p>Todo el inventario registrado está en uso o en mantenimiento.</p>
              </div>
            } @else {
              <ul class="mt-5 flex flex-col gap-2">
                @for (equipo of datos.listaEquiposFueraDeServicio; track equipo.id) {
                  <li
                    class="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-ink-200 bg-white/5 px-3 py-2.5"
                  >
                    <span class="font-bold text-ink-900">{{ equipo.codigo }}</span>
                    <span class="text-xs text-ink-600">{{ nombreEquipo(equipo) }}</span>
                    <span class="text-xs text-ink-500">{{ equipo.sala.nombre }}</span>
                  </li>
                }
              </ul>
            }
          </section>

          <!-- ============ Fallas abiertas ============ -->
          <section class="tarjeta">
            <h2 class="titulo-modulo">Fallas sin resolver</h2>
            <p class="subtitulo-modulo">Las más antiguas primero: son las que más esperan.</p>

            @if (datos.fallasAbiertas.length === 0) {
              <div class="vacio">
                <h3>Sin fallas pendientes</h3>
                <p>Todo lo reportado ya tiene mantenimiento registrado.</p>
              </div>
            } @else {
              <ul class="mt-5 flex flex-col gap-2">
                @for (falla of datos.fallasAbiertas; track falla.id) {
                  <li class="rounded-xl border border-ink-200 bg-white/5 px-3 py-2.5">
                    <div class="flex flex-wrap items-center justify-between gap-2">
                      <span class="font-bold text-ink-900">{{ falla.equipo.codigo }}</span>
                      <span class="text-xs text-ink-500">{{ fecha(falla.fechaReporte) }}</span>
                    </div>
                    <p class="mt-1 text-xs leading-relaxed text-ink-600">{{ falla.descripcion }}</p>
                  </li>
                }
              </ul>
            }
          </section>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventarioGeneral implements OnInit {
  protected readonly inventario = inject(InventarioService);

  protected readonly resumen = signal<ResumenInventario | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly fecha = formatearFecha;
protected readonly nombreEquipo = nombreEquipo;

  /**
   * Fallas sin resolver.
   *
   * El backend manda las tres cuentas por estado y la lista de las abiertas,
   * pero no un total de "abiertas"; se calcula aquí como el total menos las
   * resueltas para que el número del indicador y el de la lista no puedan
   * contradecirse.
   */
  protected readonly fallasAbiertas = computed(() => {
    const datos = this.resumen();
    if (!datos) return 0;

    return datos.totalFallas - datos.fallasResueltas;
  });

  protected readonly operativosDetalle = computed(() => {
    const datos = this.resumen();
    if (!datos) return '';

    return `${datos.equiposOperativos} de ${datos.totalEquipos} equipos en servicio`;
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.inventario.obtenerResumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.inventario.leerError(error));
        this.cargando.set(false);
      },
    });
  }
}