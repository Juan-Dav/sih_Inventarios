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
 * Módulo "Fallas y mantenimiento" del técnico.
 *
 * Recibe lo que reportan los docentes y deja por escrito qué se hizo. Al
 * guardar un mantenimiento se cierra la falla y el equipo vuelve a estar
 * operativo, porque de lo contrario el técnico repetiría el mismo trabajo la
 * próxima vez que la falla saliera en la cola.
 */
@Component({
  selector: 'app-fallas-tecnico',
  imports: [FormsModule, Kpi],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Indicadores ============ -->
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <app-kpi
          etiqueta="Fallas pendientes"
          [valor]="pendientes().length"
          detalle="sin atender todavía"
          icono="falla"
          gradiente="bg-gradient-to-r from-sun-400 to-sun-500"
          iconoClases="bg-sun-400/15 txt-espera"
        />
        <app-kpi
          etiqueta="En revisión"
          [valor]="enRevision().length"
          detalle="atendiéndose en este momento"
          icono="mantenimiento"
          gradiente="bg-gradient-to-r from-aqua-400 to-aqua-500"
          iconoClases="bg-aqua-400/15 txt-info"
        />
        <app-kpi
          etiqueta="Fallas resueltas"
          [valor]="resueltas().length"
          detalle="ya quedaron operativas"
          icono="panel"
          gradiente="bg-gradient-to-r from-mint-400 to-mint-500"
          iconoClases="bg-mint-400/15 txt-ok"
        />
        <app-kpi
          etiqueta="Mantenimientos"
          [valor]="inventario.mantenimientos().length"
          detalle="intervenciones registradas"
          icono="reporte"
          gradiente="bg-gradient-to-r from-brand-500 to-brand-600"
          iconoClases="bg-brand-500/15 text-brand-300"
        />
      </div>

      <div class="grid gap-6 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)]">
        <!-- ============ Registro de mantenimiento ============ -->
        <section class="tarjeta lg:sticky lg:top-8 lg:self-start">
          <h2 class="titulo-modulo">Registrar mantenimiento</h2>
          <p class="subtitulo-modulo">
            Elige la falla, escribe qué encontraste y qué hiciste. La falla se cerrará y el equipo
            volverá a estar operativo.
          </p>

          @if (sinFallas()) {
            <p class="aviso mt-5 bg-mint-400/15 txt-ok">
              No hay fallas pendientes de atender. Todo está al día.
            </p>
          } @else {
            <form class="mt-5 flex flex-col gap-4" (ngSubmit)="guardar()">
              <div>
                <label class="rotulo" for="mt-falla">Falla que vas a atender</label>
                <select
                  id="mt-falla"
                  name="falla"
                  class="campo"
                  [(ngModel)]="formulario.fallaId"
                  [disabled]="guardando()"
                >
                  <option [ngValue]="null">Selecciona una falla</option>
                  @for (falla of cola(); track falla.id) {
                    <option [ngValue]="falla.id">
                      {{ falla.equipo.codigo }} · {{ nombreEquipo(falla.equipo) }} —
                      {{ falla.equipo.sala.nombre }}
                    </option>
                  }
                </select>
              </div>

              <div>
                <label class="rotulo" for="mt-diagnostico">Diagnóstico</label>
                <textarea
                  id="mt-diagnostico"
                  name="diagnostico"
                  class="campo min-h-24 resize-y"
                  rows="3"
                  maxlength="500"
                  placeholder="Ej: la fuente de poder está fundida"
                  [(ngModel)]="formulario.diagnostico"
                  [disabled]="guardando()"
                ></textarea>
              </div>

              <div>
                <label class="rotulo" for="mt-solucion">Solución aplicada</label>
                <textarea
                  id="mt-solucion"
                  name="solucion"
                  class="campo min-h-24 resize-y"
                  rows="3"
                  maxlength="500"
                  placeholder="Ej: se reemplazó la fuente por una nueva de 500W"
                  [(ngModel)]="formulario.solucion"
                  [disabled]="guardando()"
                ></textarea>
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
                class="botón bg-gradient-to-r from-mint-500 to-aqua-500 text-white shadow-lg shadow-mint-500/25 hover:shadow-mint-500/40"
                [disabled]="guardando()"
              >
                {{ guardando() ? 'Guardando…' : 'Guardar mantenimiento' }}
              </button>
            </form>
          }
        </section>

        <div class="flex flex-col gap-6">
          <!-- ============ Cola de trabajo ============ -->
          <section class="tarjeta">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 class="titulo-modulo">Fallas por atender</h2>
                <p class="subtitulo-modulo">
                  Lo que|reportan los docentes y todavía no tiene solución.
                </p>
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
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  @for (falla of cola(); track falla.id) {
                    <tr>
                      <td>
                        <span class="font-bold text-ink-900">{{ falla.equipo.codigo }}</span>
                        <span class="mt-0.5 block text-xs text-ink-500">
                          {{ nombreEquipo(falla.equipo) }}
                        </span>
                        <span
                          class="etiqueta-estado mt-1"
                          [class]="estiloEquipo(falla.equipo.estado).clases"
                        >
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
                      <td class="text-right">
                        <button
                          type="button"
                          class="botón boton-acento boton-fila"
                          (click)="elegir(falla.id)"
                        >
                          Atender
                        </button>
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="6">
                        <div class="vacio">
                          <h3>No hay fallas en la cola</h3>
                          <p>Cuando un docente reporte algo aparecerá aquí.</p>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

          <!-- ============ Historial ============ -->
          <section class="tarjeta">
            <h2 class="titulo-modulo">Mantenimientos registrados</h2>
            <p class="subtitulo-modulo">Historial de las intervenciones hechas en los equipos.</p>

            <div class="tabla-envoltura mt-5">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Equipo</th>
                    <th>Diagnóstico</th>
                    <th>Solución</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  @for (mantenimiento of inventario.mantenimientos(); track mantenimiento.id) {
                    <tr>
                      <td>
                        <span class="font-bold text-ink-900">
                          {{ mantenimiento.equipo.codigo }}
                        </span>
                        <span class="mt-0.5 block text-xs text-ink-500">
                          {{ nombreEquipo(mantenimiento.equipo) }} ·
                          {{ mantenimiento.equipo.sala.nombre }}
                        </span>
                      </td>
                      <td class="max-w-xs text-ink-600">{{ mantenimiento.diagnostico }}</td>
                      <td class="max-w-xs text-ink-600">{{ mantenimiento.solucion }}</td>
                      <td class="whitespace-nowrap text-xs text-ink-500">
                        {{ fecha(mantenimiento.fechaMantenimiento) }}
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="4">
                        <div class="vacio">
                          <h3>Sin mantenimientos</h3>
                          <p>Los que registres aparecerán aquí.</p>
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
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FallasTecnico implements OnInit {
  protected readonly inventario = inject(InventarioService);

  protected readonly estiloFalla = estiloDeFalla;
protected readonly estiloEquipo = estiloDeEquipo;
protected readonly nombreEquipo = nombreEquipo;
protected readonly fecha = formatearFecha;

  protected readonly guardando = signal(false);
  protected readonly aviso = signal<Aviso | null>(null);

  protected formulario: { fallaId: number | null; diagnostico: string; solucion: string } = {
    fallaId: null,
    diagnostico: '',
    solucion: '',
  };

  /** Cola de trabajo: todo lo que aún no está resuelto. */
  protected readonly cola = computed(() =>
    this.inventario.fallas().filter((falla) => falla.estado !== 'RESUELTO'),
  );

  protected readonly pendientes = computed(() =>
    this.inventario.fallas().filter((falla) => falla.estado === 'PENDIENTE'),
  );

  protected readonly enRevision = computed(() =>
    this.inventario.fallas().filter((falla) => falla.estado === 'EN_REVISION'),
  );

  protected readonly resueltas = computed(() =>
    this.inventario.fallas().filter((falla) => falla.estado === 'RESUELTO'),
  );

  protected readonly sinFallas = computed(
    () => !this.inventario.cargando() && this.cola().length === 0,
  );

  ngOnInit(): void {
    this.inventario.cargarTodo();
  }

  protected recargar(): void {
    this.inventario.cargarTodo();
  }

  /** "Atender" en una fila deja esa falla cargada en el formulario. */
  protected elegir(fallaId: number): void {
    this.formulario.fallaId = fallaId;
    this.aviso.set(null);
  }

  protected guardar(): void {
    this.aviso.set(null);

    const fallaId = this.formulario.fallaId;
    const diagnostico = this.formulario.diagnostico.trim();
    const solucion = this.formulario.solucion.trim();

    if (fallaId === null) {
      this.aviso.set({ tipo: 'error', texto: 'Elige la falla que vas a atender.' });
      return;
    }
    if (diagnostico.length < 10) {
      this.aviso.set({
        tipo: 'error',
        texto: 'Escribe el diagnóstico con al menos 10 caracteres.',
      });
      return;
    }
    if (solucion.length < 10) {
      this.aviso.set({
        tipo: 'error',
        texto: 'Escribe la solución aplicada con al menos 10 caracteres.',
      });
      return;
    }

    const falla = this.inventario.fallas().find((item) => item.id === fallaId);
    const idEquipo = falla?.equipo?.id;

    if (!falla || idEquipo === undefined) {
      this.aviso.set({
        tipo: 'error',
        texto: 'No se encontró el equipo de esa falla. Actualiza la lista e inténtalo de nuevo.',
      });
      return;
    }

    this.guardando.set(true);

    /*
     * Una sola petición, no tres.
     *
     * El backend cierra la falla y devuelve el equipo a OPERATIVO dentro de la
     * misma transacción que guarda el mantenimiento, así que mandar además los
     * dos PATCH solo añadía peticiones que podían fallar por separado y dejar
     * el mantenimiento guardado con un error rojo en pantalla.
     */
    this.inventario.registrarMantenimiento({ diagnostico, solucion }, idEquipo, fallaId).subscribe({
      next: () => {
        this.guardando.set(false);
        this.formulario = { fallaId: null, diagnostico: '', solucion: '' };

        // El backend ya cerró la falla y reactivó el equipo; aquí solo se
        // refleja el cambio en las listas locales para no esperar un refresco.
        this.inventario.reflejarFallaResuelta(fallaId, idEquipo);

        this.aviso.set({
          tipo: 'exito',
          texto: `Mantenimiento guardado. ${falla.equipo.codigo ?? 'El equipo'} vuelve a estar operativo.`,
        });
      },
      error: (error: unknown) => {
        this.guardando.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }
}
