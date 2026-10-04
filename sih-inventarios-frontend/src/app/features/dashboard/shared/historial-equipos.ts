import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  HistorialEquipo,
  estiloDeEquipo,
  estiloDeFalla,
  formatearFecha,
  nombreEquipo,
} from '../../../core/models/inventario.model';
import { InventarioService } from '../../../core/services/inventario.service';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Historial de un equipo: sus fallas y sus mantenimientos.
 *
 * Es de consulta, así que lo comparten los tres paneles sin necesidad de
 * permisos: el docente revisa cómo va lo que reportó, el técnico mira el
 * antecedente antes de intervenir y la administración lo usa para supervisar.
 */
@Component({
  selector: 'app-historial-equipos',
  imports: [FormsModule],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <section class="tarjeta">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="titulo-modulo">Historial de un equipo</h2>
            <p class="subtitulo-modulo">
              Todo lo que se ha reportado y todas las intervenciones registradas.
            </p>
          </div>

          <button
            type="button"
            class="botón boton-acento"
            [disabled]="inventario.cargando() || cargando()"
            (click)="cargarHistorial()"
          >
            {{ cargando() ? 'Consultando…' : 'Actualizar' }}
          </button>
        </div>

        <div class="mt-5">
          <label class="rotulo" for="hist-equipo">Equipo</label>
          <select
            id="hist-equipo"
            name="equipo"
            class="campo max-w-md"
            [ngModel]="equipoSeleccionado()"
            (ngModelChange)="seleccionar($event)"
            [disabled]="inventario.cargando() || cargando()"
          >
            <option [ngValue]="null">Selecciona un equipo</option>
            @for (item of inventario.equipos(); track item.id) {
              <option [ngValue]="item.id">
                {{ item.codigo }} · {{ nombreEquipo(item) }} — {{ item.sala.nombre }}
              </option>
            }
          </select>
        </div>

        @if (equiposSinHistorial()) {
          <p class="aviso mt-4 bg-sun-400/15 txt-espera">
            Ese equipo no tiene fallas ni mantenimientos registrados.
          </p>
        }
      </section>

      @if (ficha(); as datos) {
        <!-- ============ Ficha ============ -->
        <section class="tarjeta">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 class="titulo-modulo">{{ datos.equipo.codigo }}</h2>
              <p class="subtitulo-modulo">
                {{ nombreEquipo(datos.equipo) }} · {{ datos.equipo.sala.nombre }}
              </p>
              <p class="mt-1 text-xs text-ink-500">
                {{ datos.equipo.caracteristicas || 'Sin características registradas' }}
              </p>
            </div>

            <span class="etiqueta-estado" [class]="estiloEquipo(datos.equipo.estado).clases">
              {{ estiloEquipo(datos.equipo.estado).texto }}
            </span>
          </div>

          <div class="mt-5 grid gap-4 sm:grid-cols-3">
            <div class="rounded-2xl border border-ink-200 bg-white/5 p-4">
              <p class="text-[0.65rem] font-bold uppercase tracking-[0.14em] text-ink-500">
                Fallas reportadas
              </p>
              <p class="mt-1 font-display text-2xl font-extrabold text-ink-900">
                {{ datos.fallas.length }}
              </p>
            </div>
            <div class="rounded-2xl border border-ink-200 bg-white/5 p-4">
              <p class="text-[0.65rem] font-bold uppercase tracking-[0.14em] text-ink-500">
                Mantenimientos
              </p>
              <p class="mt-1 font-display text-2xl font-extrabold text-ink-900">
                {{ datos.mantenimientos.length }}
              </p>
            </div>
            <div class="rounded-2xl border border-ink-200 bg-white/5 p-4">
              <p class="text-[0.65rem] font-bold uppercase tracking-[0.14em] text-ink-500">
                Fallas sin resolver
              </p>
              <p class="mt-1 font-display text-2xl font-extrabold text-ink-900">
                {{ sinResolver(datos) }}
              </p>
            </div>
          </div>
        </section>

        <!-- ============ Fallas ============ -->
        <section class="tarjeta">
          <h2 class="titulo-modulo">Fallas reportadas</h2>
          <p class="subtitulo-modulo">Lo que reportaste o el técnico detectó en esta máquina.</p>

          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Descripción</th>
                  <th class="w-32">Estado</th>
                </tr>
              </thead>
              <tbody>
                @for (falla of datos.fallas; track falla.id) {
                  <tr>
                    <td class="whitespace-nowrap text-ink-600">{{ fecha(falla.fechaReporte) }}</td>
                    <td class="text-ink-700">{{ falla.descripcion }}</td>
                    <td>
                      <span class="etiqueta-estado" [class]="estiloFalla(falla.estado).clases">
                        {{ estiloFalla(falla.estado).texto }}
                      </span>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="3">
                      <div class="vacio">
                        <h3>Sin fallas registradas</h3>
                        <p>Esta máquina no ha tenido novedades reportadas.</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <!-- ============ Mantenimientos ============ -->
        <section class="tarjeta">
          <h2 class="titulo-modulo">Mantenimientos</h2>
          <p class="subtitulo-modulo">Diagnóstico y solución aplicados en cada intervención.</p>

          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Diagnóstico</th>
                  <th>Solución</th>
                </tr>
              </thead>
              <tbody>
                @for (mantenimiento of datos.mantenimientos; track mantenimiento.id) {
                  <tr>
                    <td class="whitespace-nowrap text-ink-600">
                      {{ fecha(mantenimiento.fechaMantenimiento) }}
                    </td>
                    <td class="text-ink-700">{{ mantenimiento.diagnostico }}</td>
                    <td class="text-ink-700">{{ mantenimiento.solucion }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="3">
                      <div class="vacio">
                        <h3>Sin mantenimientos registrados</h3>
                        <p>Todavía no se le ha hecho ninguna intervención.</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }

      @if (aviso(); as mensaje) {
        <p
          class="aviso"
          [class.bg-mint-400/15]="mensaje.tipo === 'exito'"
          [class.txt-ok]="mensaje.tipo === 'exito'"
          [class.bg-pop-500/15]="mensaje.tipo === 'error'"
          [class.txt-alerta]="mensaje.tipo === 'error'"
        >
          {{ mensaje.texto }}
        </p>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialEquipos implements OnInit {
  protected readonly inventario = inject(InventarioService);

  protected readonly estiloEquipo = estiloDeEquipo;
  protected readonly estiloFalla = estiloDeFalla;
  protected readonly fecha = formatearFecha;
protected readonly nombreEquipo = nombreEquipo;

  protected readonly cargando = signal(false);
  protected readonly aviso = signal<Aviso | null>(null);

  protected readonly equipoSeleccionado = signal<number | null>(null);

  /** Ficha abierta. Es `null` hasta que se elige un equipo. */
  protected readonly ficha = signal<HistorialEquipo | null>(null);

  protected readonly equiposSinHistorial = computed(() => {
    const ficha = this.ficha();
    if (!ficha) return false;

    return ficha.fallas.length === 0 && ficha.mantenimientos.length === 0;
  });

  ngOnInit(): void {
    this.inventario.cargarTodo();
  }

  protected seleccionar(id: number | null): void {
    this.equipoSeleccionado.set(id);

    if (id === null) {
      this.ficha.set(null);
      return;
    }

    this.cargarHistorial();
  }

  protected cargarHistorial(): void {
    const id = this.equipoSeleccionado();

    if (id === null) {
      this.aviso.set({ tipo: 'error', texto: 'Selecciona un equipo para ver su historial.' });
      return;
    }

    this.cargando.set(true);
    this.aviso.set(null);

    this.inventario.obtenerHistorial(id).subscribe({
      next: (historial) => {
        this.ficha.set(historial);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }

  protected sinResolver(datos: HistorialEquipo): number {
    return datos.fallas.filter((falla) => falla.estado !== 'RESUELTO').length;
  }
}