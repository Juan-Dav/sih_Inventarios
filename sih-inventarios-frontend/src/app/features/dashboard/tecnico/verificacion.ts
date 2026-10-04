import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  EstadoVerificacionSala,
  Verificacion,
  estiloDeEquipo,
  formatearFecha,
} from '../../../core/models/inventario.model';
import { InventarioService } from '../../../core/services/inventario.service';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Verificación física de las aulas.
 *
 * Responde a una pregunta que el inventario en papel no contesta: si lo que
 * está registrado es lo que hay en la sala. El técnico elige un aula, cuenta lo
 * que ve y lo deja registrado; de esa cuenta salen los equipos que aparecen
 * como faltantes.
 *
 * El número de equipos esperados lo pone el backend y viene en la lista de
 * `/api/verificaciones`, que además trae los equipos de la sala. Si se calculara
 * aquí, un conteo hecho después de un alta pendiente mostraría un número que el
 * técnico no podría comprobar en el aula.
 */
@Component({
  selector: 'app-verificacion-aulas',
  imports: [FormsModule],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Estado por aula ============ -->
      <section class="tarjeta">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="titulo-modulo">Verificación física</h2>
            <p class="subtitulo-modulo">
              Elige un aula, cuenta los equipos que hay y deja registrado el resultado.
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

        @if (salas().length === 0 && !cargando()) {
          <div class="vacio mt-4">
            <h3>No hay aulas registradas</h3>
            <p>Pide al administrador que registre un aula antes de hacer el conteo.</p>
          </div>
        } @else {
          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Aula</th>
                  <th>Registrados</th>
                  <th>Último conteo</th>
                  <th>Faltantes</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (sala of salas(); track sala.salaId) {
                  <tr>
                    <td class="font-semibold text-ink-900">{{ sala.sala }}</td>
                    <td>{{ sala.equiposRegistrados }}</td>
                    <td>
                      @if (sala.ultimaVerificacion; as ultima) {
                        <span class="whitespace-nowrap">
                          {{ fecha(ultima.fechaVerificacion) }}
                        </span>
                        <span class="block text-xs text-ink-500">
                          {{ ultima.cantidadEncontrada }} encontrados por
                          {{ ultima.tecnico?.nombre ?? 'sin autor' }}
                        </span>
                      } @else {
                        <span class="text-ink-500">Nunca se ha recorrido</span>
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
                    <td>
                      <span
                        class="etiqueta-estado"
                        [class]="
                          !sala.verificada
                            ? 'bg-white/5 text-ink-500 ring-1 ring-inset ring-ink-200'
                            : sala.equiposFaltantes === 0
                              ? 'bg-mint-400/15 txt-ok ring-1 ring-inset ring-mint-400/30'
                              : 'bg-sun-400/15 txt-espera ring-1 ring-inset ring-sun-400/30'
                        "
                      >
                        {{ !sala.verificada ? 'Sin verificar' : 'Verificada' }}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        class="botón boton-acento boton-fila"
                        (click)="elegirSala(sala)"
                      >
                        {{ sala.verificada ? 'Volver a contar' : 'Verificar' }}
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }

        @if (error(); as mensaje) {
          <p class="aviso mt-4 bg-pop-500/15 txt-alerta">{{ mensaje }}</p>
        }
      </section>

      <!-- ============ Conteo de la sala elegida ============ -->
      @if (elegida(); as sala) {
        <section class="tarjeta">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 class="titulo-modulo">Conteo de {{ sala.sala }}</h2>
              <p class="subtitulo-modulo">
                Hay {{ sala.equiposRegistrados }} equipos registrados en esta aula.
              </p>
            </div>

            <button type="button" class="botón boton-secundario" (click)="elegida.set(null)">
              Cambiar de aula
            </button>
          </div>

          <!-- Los equipos esperados, para poder contar con la lista a la vista. -->
          @if (sala.equipos.length > 0) {
            <div class="mt-5">
              <p class="rotulo">Equipos registrados en la sala</p>
              <ul class="flex flex-wrap gap-1.5">
                @for (equipo of sala.equipos; track equipo.id) {
                  <li
                    class="etiqueta-estado"
                    [class]="estilo(equipo.estado).clases"
                  >
                    {{ equipo.codigo }}
                  </li>
                }
              </ul>
            </div>
          }

          <form class="mt-6 grid gap-4 sm:grid-cols-2" (ngSubmit)="guardar()">
            <div>
              <label class="rotulo" for="ver-encontrados">Equipos encontrados</label>
              <input
                id="ver-encontrados"
                name="cantidadEncontrada"
                type="number"
                min="0"
                class="campo"
                [(ngModel)]="cantidadEncontrada"
                [disabled]="guardando()"
              />
            </div>

            <div>
              <label class="rotulo" for="ver-faltantes">Códigos que faltaron</label>
              <input
                id="ver-faltantes"
                name="equiposFaltantes"
                type="text"
                class="campo"
                placeholder="PC-01, PC-02"
                [(ngModel)]="equiposFaltantes"
                [disabled]="guardando()"
              />
              <p class="mt-1.5 text-xs text-ink-500">
                Opcional. Sepáralos con comas; si no los anotas solo queda el número.
              </p>
            </div>

            <div class="sm:col-span-2">
              <label class="rotulo" for="ver-danados">Equipos dañados o fuera de uso</label>
              <input
                id="ver-danados"
                name="equiposDanados"
                type="text"
                class="campo"
                placeholder="PC-07, PC-11"
                [(ngModel)]="equiposDanados"
                [disabled]="guardando()"
              />
              <p class="mt-1.5 text-xs text-ink-500">
                Los que están en la sala pero ya no sirven. Van aparte de los faltantes porque el
                conteo puede cuadrar y aun así haber equipos quemados.
              </p>
            </div>

            <div class="sm:col-span-2">
              <label class="rotulo" for="ver-obs">Observaciones</label>
              <input
                id="ver-obs"
                name="observaciones"
                type="text"
                class="campo"
                placeholder="Ej.: un equipo dañado, movido de otra aula, sin etiqueta…"
                [(ngModel)]="observaciones"
                [disabled]="guardando()"
              />
            </div>

            <div class="sm:col-span-2">
              <p class="aviso bg-white/5 text-ink-600">{{ comparacion() }}</p>

              @if (aviso(); as mensaje) {
                <p
                  class="aviso mt-3"
                  [class]="mensaje.tipo === 'exito' ? 'bg-mint-400/15 txt-ok' : 'bg-pop-500/15 txt-alerta'"
                >
                  {{ mensaje.texto }}
                </p>
              }
            </div>

            <div class="sm:col-span-2">
              <button
                type="submit"
                class="botón bg-gradient-to-r from-mint-500 to-aqua-500 text-white shadow-lg shadow-mint-500/25 hover:shadow-mint-500/40"
                [disabled]="guardando()"
              >
                {{ guardando() ? 'Guardando…' : 'Guardar verificación' }}
              </button>
            </div>
          </form>
        </section>

        <!-- ============ Antecedente de la sala ============ -->
        @if (sala.verificaciones.length > 0) {
          <section class="tarjeta">
            <h2 class="titulo-modulo">Conteos anteriores de {{ sala.sala }}</h2>

            <div class="tabla-envoltura mt-5">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Esperados</th>
                    <th>Encontrados</th>
                    <th>Faltantes</th>
                    <th>Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  @for (conteo of sala.verificaciones; track conteo.id) {
                    <tr>
                      <td class="whitespace-nowrap">{{ fecha(conteo.fechaVerificacion) }}</td>
                      <td>{{ conteo.cantidadEsperada }}</td>
                      <td>{{ conteo.cantidadEncontrada }}</td>
                      <td>
                        {{ conteo.cantidadFaltante }}
                        @if (conteo.equiposFaltantes) {
                          <span class="block text-xs text-ink-500">{{ conteo.equiposFaltantes }}</span>
                        }
                        @if (conteo.equiposDanados) {
                          <span class="block text-xs txt-espera">
                            Dañados: {{ conteo.equiposDanados }}
                          </span>
                        }
                      </td>
                      <td>
                        <span
                          class="etiqueta-estado"
                          [class]="conteo.completa ? 'bg-mint-400/15 txt-ok' : 'bg-sun-400/15 txt-espera'"
                        >
                          {{ conteo.completa ? 'Completo' : 'Incompleto' }}
                        </span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      }

      <!-- ============ Historial de la institución ============ -->
      <section class="tarjeta">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="titulo-modulo">Conteos de toda la institución</h2>
            <p class="subtitulo-modulo">Del más reciente al más antiguo.</p>
          </div>
        </div>

        @if (cargandoHistorial()) {
          <p class="aviso mt-5 bg-white/5 text-ink-600">Consultando…</p>
        } @else if (historial().length === 0) {
          <div class="vacio">
            <h3>Todavía no hay conteos</h3>
            <p>El primero que registres aparecerá aquí.</p>
          </div>
        } @else {
          <div class="tabla-envoltura mt-5">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Aula</th>
                  <th>Técnico</th>
                  <th>Esperados</th>
                  <th>Encontrados</th>
                  <th>Faltantes</th>
                </tr>
              </thead>
              <tbody>
                @for (conteo of historial(); track conteo.id) {
                  <tr>
                    <td class="whitespace-nowrap">{{ fecha(conteo.fechaVerificacion) }}</td>
                    <td class="font-semibold text-ink-900">{{ conteo.sala.nombre }}</td>
                    <td>{{ conteo.tecnico?.nombre ?? 'Sin autor' }}</td>
                    <td>{{ conteo.cantidadEsperada }}</td>
                    <td>{{ conteo.cantidadEncontrada }}</td>
                    <td>
                      @if (conteo.cantidadFaltante > 0) {
                        <span class="font-bold txt-alerta">{{ conteo.cantidadFaltante }}</span>
                        @if (conteo.equiposFaltantes) {
                          <span class="block text-xs text-ink-500">{{ conteo.equiposFaltantes }}</span>
                        }
                      } @else {
                        <span class="txt-ok">0</span>
                      }
                      @if (conteo.equiposDanados) {
                        <span class="block text-xs txt-espera">
                          Dañados: {{ conteo.equiposDanados }}
                        </span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerificacionAulas implements OnInit {
  protected readonly inventario = inject(InventarioService);

  /** Estado de verificación por aula; es la fuente de los equipos esperados. */
  protected readonly salas = signal<EstadoVerificacionSala[]>([]);
  protected readonly historial = signal<Verificacion[]>([]);
  protected readonly elegida = signal<EstadoVerificacionSala | null>(null);

  protected readonly aviso = signal<Aviso | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly cargandoHistorial = signal(false);

  protected readonly fecha = formatearFecha;

  /*
   * Campos del formulario.
   *
   * Son propiedades planas y no signals porque van atadas con `[(ngModel)]`. Por
   * eso `comparacion()` es un método y no un `computed`: un `computed` solo se
   * recalcula cuando cambia una signal que él mismo lea, y como estos campos son
   * planos se quedaría congelado con el primer número tecleado.
   */
  protected cantidadEncontrada = 0;
  protected equiposFaltantes = '';
  protected equiposDanados = '';
  protected observaciones = '';

  protected estilo = estiloDeEquipo;

  /** Lo que el técnico contó frente a lo que el backend dice que hay. */
  protected comparacion(): string {
    const sala = this.elegida();
    if (!sala) return '';

    const diferencia = sala.equiposRegistrados - this.cantidadEncontrada;

    if (diferencia > 0) {
      return `Faltan ${diferencia} equipo(s) frente a lo registrado. Al guardar queda asentado.`;
    }

    if (diferencia < 0) {
      return `Hay ${Math.abs(diferencia)} equipo(s) de más: revisa el inventario, porque eso es un sobrante y no un faltante.`;
    }

    return 'El conteo cuadra con lo registrado.';
  }

  ngOnInit(): void {
    this.cargar();
    this.cargarHistorial();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.inventario.listarEstadoPorSala().subscribe({
      next: (salas) => {
        this.salas.set(salas);
        this.cargando.set(false);

        // Si ya había un aula abierta, se refresca con los datos nuevos para no
        // dejar el formulario mostrando el conteo anterior.
        const idElegida = this.elegida()?.salaId;
        if (idElegida !== undefined) {
          this.elegida.set(salas.find((sala) => sala.salaId === idElegida) ?? null);
        }
      },
      error: (error: unknown) => {
        this.error.set(this.inventario.leerError(error));
        this.cargando.set(false);
      },
    });
  }

  protected elegirSala(sala: EstadoVerificacionSala): void {
    this.elegida.set(sala);
    this.aviso.set(null);

    // El conteo arranca en lo registrado: es lo más probable que se encuentre.
    this.cantidadEncontrada = sala.equiposRegistrados;
    this.equiposFaltantes = '';
    this.equiposDanados = '';
    this.observaciones = '';
  }

  protected guardar(): void {
    const sala = this.elegida();
    if (!sala) return;

    this.guardando.set(true);
    this.aviso.set(null);

    this.inventario
      .registrarVerificacion(
        {
          cantidadEncontrada: this.cantidadEncontrada,
          equiposFaltantes: this.equiposFaltantes.trim() || null,
          equiposDanados: this.equiposDanados.trim() || null,
          observaciones: this.observaciones.trim() || null,
        },
        sala.salaId,
      )
      .subscribe({
        next: (verificacion) => {
          this.guardando.set(false);
          this.historial.update((lista) => [verificacion, ...lista]);
          this.aviso.set({
            tipo: verificacion.completa ? 'exito' : 'error',
            texto: verificacion.completa
              ? 'Conteo guardado. La aula cuadra con lo registrado.'
              : `Conteo guardado. Faltan ${verificacion.cantidadFaltante} equipo(s). Revisa el inventario de la sala.`,
          });

          this.cargar();
        },
        error: (error: unknown) => {
          this.guardando.set(false);
          this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
        },
      });
  }

  protected cargarHistorial(): void {
    this.cargandoHistorial.set(true);

    this.inventario.listarHistorialVerificaciones().subscribe({
      next: (historial) => {
        this.historial.set(historial);
        this.cargandoHistorial.set(false);
      },
      error: (error: unknown) => {
        this.cargandoHistorial.set(false);
        this.error.set(this.inventario.leerError(error));
      },
    });
  }
}