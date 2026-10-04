import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  estiloDeEquipo,
  estiloDeFalla,
  formatearFecha,
  nombreEquipo,
} from '../../../core/models/inventario.model';
import { InventarioService } from '../../../core/services/inventario.service';
import { Kpi } from '../../../core/ui/kpi/kpi';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Módulo "Reportar falla" del docente.
 *
 * El alta necesita un equipo, y para elegirlo hay que elegir antes el aula que
 * lo contiene, por eso los dos selectores van en cascada. Al cambiar de aula se
 * limpia el equipo: si no, se podría mandar el id de un equipo que pertenece a
 * otra aula.
 *
 * El formulario es un signal y no un objeto suelto a propósito: el listado de
 * equipos del aula elegida sale de un `computed`, y los `computed` solo se
 * recalculan cuando cambia una señal de la que dependen. Con el aula en una
 * propiedad normal, el `computed` se resolvía una vez con el aula vacía y se
 * quedaba con esa lista vacía para siempre: al elegir el aula la plantilla se
 * volvía a pintar, pero el `@for` ya no tenía equipos que dibujar y el selector
 * se quedaba en "Selecciona un equipo" sin opciones.
 */
@Component({
  selector: 'app-fallas-docente',
  imports: [FormsModule, Kpi],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Indicadores ============ -->
      <div class="grid gap-4 sm:grid-cols-3">
        <app-kpi
          etiqueta="Fallas reportadas"
          [valor]="inventario.fallas().length"
          detalle="en total entre todas las aulas"
          icono="falla"
          gradiente="bg-gradient-to-r from-aqua-400 to-aqua-500"
          iconoClases="bg-aqua-400/15 txt-info"
        />
        <app-kpi
          etiqueta="Pendientes de atención"
          [valor]="abiertas().length"
          detalle="aún sin resolver"
          icono="mantenimiento"
          gradiente="bg-gradient-to-r from-sun-400 to-sun-500"
          iconoClases="bg-sun-400/15 txt-espera"
        />
        <app-kpi
          etiqueta="Aulas con equipos"
          [valor]="salasConEquipos().length"
          [detalle]="salasConEquipos().length === 1 ? '1 aula disponible' : 'aulas disponibles'"
          icono="aula"
          gradiente="bg-gradient-to-r from-brand-500 to-brand-600"
          iconoClases="bg-brand-500/15 text-brand-300"
        />
      </div>

      @if (sinEquipos()) {
        <p class="aviso bg-sun-400/15 txt-espera">
          Todavía no hay equipos registrados en ninguna aula, así que aún no se pueden reportar
          fallas. Pídele al administrador que registre los equipos primero.
        </p>
      }

      <div class="grid gap-6 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)]">
        <!-- ============ Alta de falla ============ -->
        <section class="tarjeta lg:sticky lg:top-8 lg:self-start">
          <h2 class="titulo-modulo">Reportar falla</h2>
          <p class="subtitulo-modulo">
            Elige el aula, el equipo y describe qué pasa. La llega al técnico en estado pendiente.
          </p>

          <form class="mt-5 flex flex-col gap-4" (ngSubmit)="reportar()">
            <div>
              <label class="rotulo" for="falla-aula">Aula</label>
              <select
                id="falla-aula"
                name="aula"
                class="campo"
                [ngModel]="formulario().aulaId"
                (ngModelChange)="elegirAula($event)"
                [disabled]="enviando()"
              >
                <option [ngValue]="null">Selecciona un aula</option>
                @for (sala of inventario.salas(); track sala.id) {
                  <option [ngValue]="sala.id">{{ sala.nombre }}</option>
                }
              </select>
            </div>

            <div>
              <label class="rotulo" for="falla-equipo">Equipo</label>
              <select
                id="falla-equipo"
                name="equipo"
                class="campo"
                [ngModel]="formulario().equipoId"
                (ngModelChange)="elegirEquipo($event)"
                [disabled]="enviando() || formulario().aulaId === null"
              >
                <option [ngValue]="null">
                  {{ formulario().aulaId === null ? 'Primero elige un aula' : 'Selecciona un equipo' }}
                </option>
                @for (equipo of equiposDisponibles(); track equipo.id) {
                  <option [ngValue]="equipo.id">
                    {{ equipo.codigo }} · {{ nombreEquipo(equipo) }} ·
                    {{ estiloEquipo(equipo.estado).texto }}
                  </option>
                }
              </select>
            </div>

            <div>
              <label class="rotulo" for="falla-descripcion">¿Qué ocurre?</label>
              <textarea
                id="falla-descripcion"
                name="descripcion"
                class="campo min-h-28 resize-y"
                rows="4"
                maxlength="500"
                placeholder="Ej: el equipo no enciende y el ventilador no gira"
                [ngModel]="formulario().descripcion"
                (ngModelChange)="escribirDescripcion($event)"
                [disabled]="enviando()"
              ></textarea>
              <p class="mt-1 text-right text-xs text-ink-400">
                {{ formulario().descripcion.length }}/500
              </p>
            </div>

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

            <button
              type="submit"
              class="botón bg-gradient-to-r from-aqua-500 to-brand-600 text-white shadow-lg shadow-aqua-500/25 hover:shadow-aqua-500/40"
              [disabled]="enviando()"
            >
              {{ enviando() ? 'Enviando…' : 'Reportar falla' }}
            </button>
          </form>
        </section>

        <!-- ============ Historial ============ -->
        <section class="tarjeta">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="titulo-modulo">Fallas registradas</h2>
              <p class="subtitulo-modulo">Todo lo que se ha reportado en el sistema.</p>
            </div>

            <button
              type="button"
              class="botón boton-acento"
              [disabled]="inventario.cargando()"
              (click)="recargar()"
            >
              {{ inventario.cargando() ? 'Actualizando…' : 'Actualizar' }}
            </button>
          </div>

          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Equipo</th>
                  <th>Aula</th>
                  <th>Falla</th>
                  <th>Reportada</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                @for (falla of inventario.fallas(); track falla.id) {
                  <tr>
                    <td>
                      <span class="font-bold text-ink-900">{{ falla.equipo.codigo }}</span>
                      <span class="mt-0.5 block text-xs text-ink-500">
                        {{ nombreEquipo(falla.equipo) }}
                      </span>
                      <span class="etiqueta-estado mt-1" [class]="estiloFalla(falla.estado).clases">
                        {{ estiloEquipo(falla.equipo.estado).texto }}
                      </span>
                    </td>
                    <td class="text-ink-600">{{ falla.equipo.sala.nombre }}</td>
                    <td class="max-w-sm text-ink-600">{{ falla.descripcion }}</td>
                    <td class="whitespace-nowrap text-xs text-ink-500">
                      {{ fecha(falla.fechaReporte) }}
                    </td>
                    <td>
                      <span class="etiqueta-estado" [class]="estiloFalla(falla.estado).clases">
                        {{ estiloFalla(falla.estado).texto }}
                      </span>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5">
                      <div class="vacio">
                        <h3>No hay fallas registradas</h3>
                        <p>Cuando reportes la primera aparecerá en esta tabla.</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FallasDocente implements OnInit {
  protected readonly inventario = inject(InventarioService);

  protected readonly estiloFalla = estiloDeFalla;
protected readonly estiloEquipo = estiloDeEquipo;
protected readonly nombreEquipo = nombreEquipo;
protected readonly fecha = formatearFecha;

  protected readonly enviando = signal(false);
  protected readonly aviso = signal<Aviso | null>(null);

  protected formulario = signal<{
    aulaId: number | null;
    equipoId: number | null;
    descripcion: string;
  }>({
    aulaId: null,
    equipoId: null,
    descripcion: '',
  });

  /** Fallas que todavía no están resueltas. */
  protected readonly abiertas = computed(() =>
    this.inventario.fallas().filter((falla) => falla.estado !== 'RESUELTO'),
  );

  /** Equipos del aula elegida, para el segundo selector. */
  protected readonly equiposDisponibles = computed(() => {
    const aulaId = this.formulario().aulaId;
    if (aulaId === null) return [];
    return this.inventario
      .equipos()
      .filter((equipo) => Number(equipo.sala?.id) === Number(aulaId));
  });

  /** Salas que ya tienen al menos un equipo; si no hay ninguna, no se puede reportar. */
  protected readonly salasConEquipos = computed(() => {
    const conEquipos = new Set(
      this.inventario
        .equipos()
        .map((equipo) => equipo.sala?.id)
        .filter((id): id is number => id !== undefined && id !== null),
    );
    return this.inventario.salas().filter((sala) => conEquipos.has(sala.id));
  });

  protected readonly sinEquipos = computed(
    () => this.inventario.cargando() || this.inventario.equipos().length === 0,
  );

  ngOnInit(): void {
    this.inventario.cargarTodo();
  }

  protected recargar(): void {
    this.inventario.cargarTodo();
  }

  /**
   * Al cambiar de aula se descarta el equipo, que ya no corresponde.
   *
   * El aula y el equipo se escriben en el mismo signal porque el `computed` de
   * los equipos necesita ver el aula nueva y el equipo limpio en la misma
   * pasada; si se escribieran por separado, el filtro se recalcularía dos veces.
   */
  protected elegirAula(aulaId: number | null): void {
    this.formulario.update((formulario) => ({ ...formulario, aulaId, equipoId: null }));
  }

  protected elegirEquipo(equipoId: number | null): void {
    this.formulario.update((formulario) => ({ ...formulario, equipoId }));
  }

  protected escribirDescripcion(descripcion: string): void {
    this.formulario.update((formulario) => ({ ...formulario, descripcion }));
  }

  protected reportar(): void {
    this.aviso.set(null);

    const formulario = this.formulario();
    const descripcion = formulario.descripcion.trim();
    const equipoId = formulario.equipoId;

    if (formulario.aulaId === null) {
      this.aviso.set({ tipo: 'error', texto: 'Elige el aula donde está el equipo.' });
      return;
    }
    if (equipoId === null) {
      this.aviso.set({ tipo: 'error', texto: 'Elige el equipo que tiene la falla.' });
      return;
    }
    if (descripcion.length < 10) {
      this.aviso.set({
        tipo: 'error',
        texto: 'Describe la falla con al menos 10 caracteres para que el técnico entienda.',
      });
      return;
    }

    this.enviando.set(true);

    this.inventario.reportarFalla({ descripcion, estado: 'PENDIENTE' }, equipoId).subscribe({
      next: (falla) => {
        this.enviando.set(false);
        this.formulario.set({ aulaId: null, equipoId: null, descripcion: '' });
        this.aviso.set({
          tipo: 'exito',
          texto: `Falla reportada en ${falla.equipo.codigo ?? 'el equipo'}. Ya está en la cola del técnico.`,
        });
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }
}
