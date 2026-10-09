import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  ESTADOS_EQUIPO,
  Equipo,
  EstadoEquipo,
  Sala,
  estiloDeEquipo,
  nombreEquipo,
} from '../../../core/models/inventario.model';
import { InventarioService } from '../../../core/services/inventario.service';
import { PlanService } from '../../../core/services/plan.service';
import { Kpi } from '../../../core/ui/kpi/kpi';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Módulo de aulas y equipos, compartido por los tres paneles.
 *
 * Es un único componente y tres instancias porque el contenido es el mismo y lo
 * único que cambia es el permiso. Los tres roles dan de alta y editan; el que
 * además puede cambiar el estado de una máquina o atender una falla es el
 * técnico y la administración, y eso llega en una bandera aparte. Separarlo en
 * tres archivos habría que mantener tres veces la misma tabla.
 */
@Component({
  selector: 'app-salas-equipos',
  imports: [FormsModule, Kpi],
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Indicadores ============ -->
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <app-kpi
          etiqueta="Aulas registradas"
          [valor]="inventario.totalSalas()"
          detalle="aulas dadas de alta en el sistema"
          icono="aula"
          [gradiente]="gradiente()"
          iconoClases="bg-brand-500/15 text-brand-300"
        />
        <app-kpi
          etiqueta="Equipos registrados"
          [valor]="inventario.totalEquipos()"
          detalle="máquinas dadas de alta"
          icono="equipo"
          [gradiente]="gradiente()"
          iconoClases="bg-pop-500/15 txt-alerta"
        />
        <app-kpi
          etiqueta="Equipos operativos"
          [valor]="porcentajeOperativo() + '%'"
          [detalle]="inventario.equiposOperativos() + ' de ' + inventario.totalEquipos() + ' en servicio'"
          icono="panel"
          [gradiente]="gradiente()"
          iconoClases="bg-mint-400/15 txt-ok"
        />
        <app-kpi
          etiqueta="Capacidad total"
          [valor]="inventario.capacidadTotal()"
          detalle="máquinas que caben en todas las aulas"
          icono="reporte"
          [gradiente]="gradiente()"
          iconoClases="bg-sun-400/15 txt-espera"
        />
      </div>

      @if (errorRed()) {
        <p class="aviso bg-pop-500/15 txt-alerta">{{ errorRed() }}</p>
      }

      <div class="grid gap-6 lg:grid-cols-[minmax(0,21rem)_minmax(0,1fr)]">
        <!-- ============ Altas ============ -->
        <div class="flex flex-col gap-6">
          @if (permiteCrear()) {
            <!-- ---- Aula ---- -->
            <section class="tarjeta">
              <h2 class="titulo-modulo">
                {{ editandoSala() ? 'Editar aula' : 'Registrar aula' }}
              </h2>
              <p class="subtitulo-modulo">
                {{
                  editandoSala()
                    ? 'Corrige el nombre o ajusta cuántas máquinas admite el espacio.'
                    : 'Define el espacio y cuántas máquinas admite. Después se registran los equipos dentro de ella.'
                }}
              </p>

              <form class="mt-5 flex flex-col gap-4" (ngSubmit)="registrarSala()">
                <div>
                  <label class="rotulo" for="se-sala-nombre">Nombre del aula</label>
                  <input
                    id="se-sala-nombre"
                    name="salaNombre"
                    type="text"
                    class="campo"
                    placeholder="Ej: Laboratorio de Sistemas 1"
                    maxlength="100"
                    [(ngModel)]="sala.nombre"
                    [disabled]="guardandoSala()"
                  />
                </div>

                <div>
                  <label class="rotulo" for="se-sala-capacidad">Capacidad de equipos</label>
                  <input
                    id="se-sala-capacidad"
                    name="salaCapacidad"
                    type="number"
                    class="campo"
                    min="1"
                    max="200"
                    [(ngModel)]="sala.capacidadMaxima"
                    [disabled]="guardandoSala()"
                  />
                </div>

<button
                    type="submit"
                    class="botón text-white"
                    [class]="gradienteSolido()"
                    [disabled]="guardandoSala() || (!editandoSala() && limiteDeSalasAlcanzado())"
                  >
                    {{ guardandoSala() ? 'Guardando…' : editandoSala() ? 'Guardar cambios' : 'Registrar aula' }}
                  </button>

                  @if (editandoSala()) {
                    <button
                      type="button"
                      class="botón boton-secundario"
                      [disabled]="guardandoSala()"
                      (click)="cancelarEdicionSala()"
                    >
                      Cancelar
                    </button>
                  }
              </form>

                <!-- Se avisa antes de intentar el alta, en vez de dejar que el backend la rechace. -->
                @if (limiteDeSalasAlcanzado()) {
                  <p class="aviso mt-4 bg-sun-400/15 txt-espera">
                    @if (maxSalas() === 1) {
                      El plan actual permite 1 sola aula. Pide a la administración que contrate
                      Premium para registrar más.
                    } @else {
                      El plan actual permite {{ maxSalas() }} aulas y ya están registradas. Pide a la
                      administración que contrate Premium para registrar más.
                    }
                  </p>
                }
              </section>

            <!-- ---- Equipo ---- -->
            <section class="tarjeta">
              <h2 class="titulo-modulo">
                {{ editandoEquipo() ? 'Editar equipo' : 'Registrar equipo' }}
              </h2>
              <p class="subtitulo-modulo">
                {{
                  editandoEquipo()
                    ? 'Actualiza sus datos o cámbialo de aula. Si lo mueves, se validan los límites del aula de destino.'
                    : 'El equipo se asigna a un aula, que es lo que limita su cantidad.'
                }}
              </p>

              @if (inventario.totalSalas() === 0) {
                <p class="aviso mt-4 bg-sun-400/15 txt-espera">
                  Registra primero un aula: un equipo siempre pertenece a una.
                </p>
              } @else {
                <form class="mt-5 flex flex-col gap-4" (ngSubmit)="registrarEquipo()">
                  <div>
                    <label class="rotulo" for="se-eq-sala">Aula</label>
                    <select
                      id="se-eq-sala"
                      name="equipoSala"
                      class="campo"
                      [(ngModel)]="equipo.salaId"
                      [disabled]="guardandoEquipo()"
                    >
                      <option [ngValue]="null">Selecciona un aula</option>
                      @for (aula of inventario.salas(); track aula.id) {
                        <option [ngValue]="aula.id">
                          {{ aula.nombre }} ({{ equiposDe(aula.id) }}/{{ aula.capacidadMaxima }})
                        </option>
                      }
                    </select>
                  </div>

                  <div>
                    <label class="rotulo" for="se-eq-codigo">Código</label>
                    <input
                      id="se-eq-codigo"
                      name="equipoCodigo"
                      type="text"
                      class="campo"
                      placeholder="Ej: PC-001"
                      maxlength="50"
                      [(ngModel)]="equipo.codigo"
                      [disabled]="guardandoEquipo()"
                    />
                  </div>

                  <div>
                    <label class="rotulo" for="se-eq-caracteristicas">Características</label>
                    <textarea
                      id="se-eq-caracteristicas"
                      name="equipoCaracteristicas"
                      class="campo min-h-24 resize-y"
                      rows="3"
                      maxlength="200"
                      placeholder="Ej: Intel i5, 8 GB RAM, disco 500 GB"
                      [(ngModel)]="equipo.caracteristicas"
                      [disabled]="guardandoEquipo()"
                    ></textarea>
                  </div>

                  <!--
                    Marca y modelo son opcionales a propósito: en una demo rápida
                    casi nunca se anotan y no deben frenar el alta. Cuando sí se
                            escriben, el inventario general los muestra junto al
                    código, que es donde sirven.
                  -->
                  <div>
                    <label class="rotulo" for="se-eq-marca">Marca</label>
                    <input
                      id="se-eq-marca"
                      name="equipoMarca"
                      type="text"
                      class="campo"
                      placeholder="Ej: Lenovo"
                      maxlength="60"
                      [(ngModel)]="equipo.marca"
                      [disabled]="guardandoEquipo()"
                    />
                  </div>

                  <div>
                    <label class="rotulo" for="se-eq-modelo">Modelo</label>
                    <input
                      id="se-eq-modelo"
                      name="equipoModelo"
                      type="text"
                      class="campo"
                      placeholder="Ej: ThinkCentre M70"
                      maxlength="60"
                      [(ngModel)]="equipo.modelo"
                      [disabled]="guardandoEquipo()"
                    />
                  </div>

                  <div>
                    <label class="rotulo" for="se-eq-estado">Estado inicial</label>
                    <select
                      id="se-eq-estado"
                      name="equipoEstado"
                      class="campo"
                      [(ngModel)]="equipo.estado"
                      [disabled]="guardandoEquipo()"
                    >
                      @for (estado of estados; track estado) {
                        <option [ngValue]="estado">{{ estilo(estado).texto }}</option>
                      }
                    </select>
                  </div>

                  <button type="submit" class="botón text-white" [class]="gradienteSolido()" [disabled]="guardandoEquipo()">
                    {{ guardandoEquipo() ? 'Guardando…' : editandoEquipo() ? 'Guardar cambios' : 'Registrar equipo' }}
                  </button>

                  @if (editandoEquipo()) {
                    <button
                      type="button"
                      class="botón boton-secundario"
                      [disabled]="guardandoEquipo()"
                      (click)="cancelarEdicionEquipo()"
                    >
                      Cancelar
                    </button>
                  }
                </form>
              }
            </section>
          } @else {
            <!-- El docente solo consulta: se le explica por qué no hay formulario -->
            <section class="tarjeta">
              <h2 class="titulo-modulo">Consulta de inventario</h2>
              <p class="subtitulo-modulo">
                Aquí puedes ver dónde está cada equipo, en qué aula y en qué estado. Dar de alta un
                aula o cambiar el estado de un equipo corresponde al técnico y a la administración.
              </p>
              <p class="mt-4 text-xs text-ink-500">
                Si un equipo no aparece o está en mal estado, repórtalo desde
                <span class="font-semibold txt-acento">Reportar falla</span>: el técnico lo recibe
                y lo resuelve.
              </p>
            </section>
          }

          <!-- Avisos de las altas -->
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

        <!-- ============ Listados ============ -->
        <div class="flex flex-col gap-6">
          <!-- ---- Aulas ---- -->
          <section class="tarjeta">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 class="titulo-modulo">Aulas del sistema</h2>
                <p class="subtitulo-modulo">Capacidad y equipos que tiene asignados cada una.</p>
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
                    <th>Aula</th>
                    <th>Capacidad</th>
                    <th>Equipos</th>
                    <th class="w-40">Ocupación</th>
                  </tr>
                </thead>
                <tbody>
                  @for (aula of inventario.salas(); track aula.id) {
                    <tr>
                      <td>
                        <div class="flex flex-wrap items-center gap-2">
                          <span class="font-bold text-ink-900">{{ aula.nombre }}</span>
                          @if (permiteEditar()) {
                            <button
                              type="button"
                              class="botón boton-secundario boton-fila"
                              [disabled]="operando()"
                              (click)="editarSala(aula)"
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              class="botón boton-fila border border-pop-400/40 bg-pop-500/15 text-[#f9a8d4] hover:bg-pop-500/25"
                              [disabled]="operando()"
                              (click)="eliminarSala(aula)"
                            >
                              Eliminar
                            </button>
                          }
                        </div>
                      </td>
                      <td class="text-ink-600">{{ aula.capacidadMaxima }}</td>
                      <td class="text-ink-600">{{ equiposDe(aula.id) }}</td>
                      <td>
                        @let porcentaje = ocupacion(aula.id);
                        <div class="flex items-center gap-2">
                          <div class="h-2 flex-1 overflow-hidden rounded-full bg-ink-100">
                            <div
                              class="h-full rounded-full transition-[width] duration-500"
                              [class]="gradiente()"
                              [style.width.%]="porcentaje"
                            ></div>
                          </div>
                          <span class="w-11 text-right text-xs font-bold text-ink-600">
                            {{ porcentaje }}%
                          </span>
                        </div>
                        @if (excedeCapacidad(aula.id)) {
                          <p class="mt-1 text-xs font-semibold txt-alerta">
                            Supera la capacidad del aula
                          </p>
                        }
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="4">
                        <div class="vacio">
                          <h3>Todavía no hay aulas</h3>
                          <p>
                            @if (permiteCrear()) {
                              Registra la primera con el formulario de al lado.
                            } @else {
                              Aún no se ha registrado ninguna aula en el sistema.
                            }
                          </p>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

          <!-- ---- Equipos ---- -->
          <section class="tarjeta">
            <h2 class="titulo-modulo">Equipos registrados</h2>
            <p class="subtitulo-modulo">
              {{ inventario.totalEquipos() }} en total.
              @if (permiteCambiarEstado()) {
                Cambia el estado para reflejar la situación real de cada máquina.
              }
            </p>

            <div class="tabla-envoltura mt-5">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Equipo</th>
                    <th>Aula</th>
                    <th>Características</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of inventario.equipos(); track item.id) {
                    <tr>
                      <!--
                        Código arriba y marca debajo, en la misma celda. Con dos
                        columnas separadas la marca quedaba lejos del código y en
                        pantallas estrechas desaparecía de la vista, que es justo
                        el dato que sirve para reconocer la máquina.
                      -->
                      <td>
                        <span class="font-bold text-ink-900">{{ item.codigo }}</span>
                        <span class="mt-0.5 block text-xs text-ink-500">
                          {{ nombreEquipo(item) }}
                        </span>
                      </td>
                      <td class="text-ink-600">{{ item.sala.nombre }}</td>
                      <td class="max-w-sm text-ink-600">{{ item.caracteristicas || '—' }}</td>
                      <td>
                        <div class="flex flex-wrap items-center gap-2">
                          <span class="etiqueta-estado" [class]="estilo(item.estado).clases">
                            {{ estilo(item.estado).texto }}
                          </span>
                          <!--
                            El selector de estado no se guía por
                            permiteEditar sino por permiteCambiarEstado:
                            documentar un equipo es cosa del aula, pero decidir
                            si una máquina está fuera de servicio es del
                            técnico. En modo lectura un desplegable que parece
                            editable pero luego el backend rechaza con un 403
                            solo confunde: la etiqueta al lado ya dice el
                            estado.
                          -->
                          @if (permiteCambiarEstado()) {
                            <select
                              class="campo !w-auto !py-1 !text-xs"
                              [disabled]="cambiandoEstado()"
                              [value]="item.estado"
                              (change)="cambiarEstadoDe(item.id, $event)"
                              [attr.aria-label]="'Cambiar estado de ' + item.codigo"
                            >
                              @for (estado of estados; track estado) {
                                <option [value]="estado">{{ estilo(estado).texto }}</option>
                              }
                            </select>
                          }
                          @if (permiteEditar()) {
                            <button
                              type="button"
                              class="botón boton-secundario boton-fila"
                              [disabled]="operando()"
                              (click)="editarEquipo(item)"
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              class="botón boton-fila border border-pop-400/40 bg-pop-500/15 text-[#f9a8d4] hover:bg-pop-500/25"
                              [disabled]="operando()"
                              (click)="eliminarEquipo(item)"
                            >
                              Eliminar
                            </button>
                          }
                        </div>
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="4">
                        <div class="vacio">
                          <h3>Todavía no hay equipos</h3>
                          <p>
                            @if (permiteCrear()) {
                              Registra el primero eligiendo el aula a la que pertenece.
                            } @else {
                              Aún no se ha registrado ningún equipo en el sistema.
                            }
                          </p>
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
export class SalasEquipos implements OnInit {
  protected readonly inventario = inject(InventarioService);

  /** Solo se consulta para avisar del límite de salas del plan gratuito. */
  protected readonly planService = inject(PlanService);

  /** Cuando es `false` el módulo es de solo lectura: no se pintan los formularios. */
  readonly permiteCrear = input(false);

  /**
   * Cuando es `true` aparece la edición de aulas y equipos existentes.
   *
   * El alta es de los tres roles —el docente que recibe la máquina también es
   * quien la documenta— pero la corrección es solo del técnico y de la
   * administración: por eso el panel del docente deja esta bandera en `false`.
   */
  readonly permiteEditar = input(false);

  /**
   * Cuando es `true` aparece el desplegable para cambiar el estado de un equipo.
   *
   * Va aparte de `permiteEditar` porque no es lo mismo. Documentar un equipo es
   * un dato administrativo del aula; ponerlo fuera de servicio o en
   * mantenimiento es una decisión de mantenimiento, y el backend solo se la deja
   * tomar al técnico y a la administración. Si esta bandera no existiera, el
   * docente vería un desplegable que parece editable y que siempre respondería
   * 403.
   */
  readonly permiteCambiarEstado = input(false);

  /** Degradado del rol, para las franjas y barras de ocupación. */
  readonly gradiente = input.required<string>();

  /** Degradado sólido del rol, para los botones de alta. */
  readonly gradienteSolido = input.required<string>();

  protected readonly estados = ESTADOS_EQUIPO;
  protected readonly estilo = estiloDeEquipo;
  protected readonly nombreEquipo = nombreEquipo;
  protected readonly errorRed = this.inventario.error;

  protected readonly guardandoSala = signal(false);
  protected readonly guardandoEquipo = signal(false);
  protected readonly cambiandoEstado = signal(false);
  protected readonly operando = signal(false);
  protected readonly aviso = signal<Aviso | null>(null);

  protected sala = { nombre: '', capacidadMaxima: 12 };

  protected equipo = {
    salaId: null as number | null,
    codigo: '',
    caracteristicas: '',
    marca: '',
    modelo: '',
    estado: 'OPERATIVO' as EstadoEquipo,
  };

  /** Aula abierta en edición, o `null` si el formulario es un alta nueva. */
  protected readonly editandoSala = signal<Sala | null>(null);

  /** Equipo abierto en edición, o `null` si el formulario es un alta nueva. */
  protected readonly editandoEquipo = signal<Equipo | null>(null);

  /**
   * `true` cuando el plan actual ya tiene todas las aulas que permite.
   *
   * El tope sale del propio `GET /api/plan` y no de un número escrito aquí: es
   * el mismo valor con el que el backend valida el alta, así que el aviso y la
   * regla no pueden contradecirse.
   *
   * Es una comprobación informativa. Si el plan aún no ha llegado (o falló la
   * petición) se devuelve `false` y el formulario se muestra: el backend sigue
   * rejecting el alta con su mensaje, que es la garantía real del límite.
   */
  protected readonly limiteDeSalasAlcanzado = computed(() => {
    const plan = this.planService.plan();
    if (!plan || plan.salasIlimitadas || !plan.maxSalas) return false;

    return this.inventario.totalSalas() >= plan.maxSalas;
  });

  /** Topo de aulas del plan actual, o `null` si no hay límite. */
  protected readonly maxSalas = computed(() => {
    const plan = this.planService.plan();
    return plan?.salasIlimitadas ? null : (plan?.maxSalas ?? null);
  });

  /** Si el alta de aula la puede hacer quien está viendo esta pantalla. */
  protected readonly puedeRegistrarAula = computed(
    () => this.permiteCrear() && !this.limiteDeSalasAlcanzado(),
  );

  protected readonly porcentajeOperativo = computed(() => {
    const total = this.inventario.totalEquipos();
    if (total === 0) return 0;
    return Math.round((this.inventario.equiposOperativos() / total) * 100);
  });

  ngOnInit(): void {
    this.inventario.cargarTodo();
  }

  protected recargar(): void {
    this.inventario.cargarTodo();
  }

  /** Equipos registrados en una aula concreta. */
  protected equiposDe(aulaId: number): number {
    return this.inventario.equipos().filter((item) => item.sala?.id === aulaId).length;
  }

  protected excedeCapacidad(aulaId: number): boolean {
    const aula = this.inventario.salas().find((item) => item.id === aulaId);
    return !!aula && this.equiposDe(aulaId) > aula.capacidadMaxima;
  }

  /** Porcentaje de ocupación, acotado a 100 para que la barra no se desborde. */
  protected ocupacion(aulaId: number): number {
    const aula = this.inventario.salas().find((item) => item.id === aulaId);
    if (!aula || aula.capacidadMaxima <= 0) return 0;
    return Math.min(100, Math.round((this.equiposDe(aulaId) / aula.capacidadMaxima) * 100));
  }

  /** Guarda un alta o una edición de aula, según haya una abierta. */
  protected registrarSala(): void {
    this.aviso.set(null);

    const nombre = this.sala.nombre.trim();
    const capacidad = Number(this.sala.capacidadMaxima);
    const enEdicion = this.editandoSala();

    if (!nombre) {
      this.aviso.set({ tipo: 'error', texto: 'Escribe el nombre del aula.' });
      return;
    }
    if (!Number.isFinite(capacidad) || capacidad < 1 || capacidad > 200) {
      this.aviso.set({ tipo: 'error', texto: 'La capacidad debe ser un número entre 1 y 200.' });
      return;
    }

    // Al editar se permite dejar el nombre igual: solo se compara contra otras aulas.
    if (
      this.inventario
        .salas()
        .some((item) => item.id !== enEdicion?.id && item.nombre.trim().toLowerCase() === nombre.toLowerCase())
    ) {
      this.aviso.set({ tipo: 'error', texto: 'Ya existe un aula con ese nombre.' });
      return;
    }

    // El aviso del formulario ya dice por qué; aquí solo se evita el viaje en
    // vano cuando el plan ya está en su tope.
    if (!enEdicion && this.limiteDeSalasAlcanzado()) {
      this.aviso.set({
        tipo: 'error',
        texto: `El plan actual solo permite ${this.maxSalas()} aula(s) y ya están registradas. Pide a la administración que contrate Premium.`,
      });
      return;
    }

    this.guardandoSala.set(true);

    const peticion = enEdicion
      ? this.inventario.actualizarSala(enEdicion.id, { nombre, capacidadMaxima: capacidad })
      : this.inventario.crearSala({ nombre, capacidadMaxima: capacidad });

    peticion.subscribe({
      next: (guardada) => {
        this.guardandoSala.set(false);
        this.cancelarEdicionSala();
        this.sala = { nombre: '', capacidadMaxima: 12 };
        this.aviso.set({
          tipo: 'exito',
          texto: enEdicion
            ? `Aula "${guardada.nombre}" actualizada.`
            : `Aula "${guardada.nombre}" registrada.`,
        });
      },
      error: (error: unknown) => {
        this.guardandoSala.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }

  /** Abre el formulario de aula en modo edición con los datos cargados. */
  protected editarSala(aula: Sala): void {
    this.aviso.set(null);
    this.editandoSala.set(aula);
    this.sala = { nombre: aula.nombre, capacidadMaxima: aula.capacidadMaxima };
  }

  protected eliminarSala(aula: Sala): void {
    this.aviso.set(null);
    if (confirm(`¿Estás seguro de eliminar el aula "${aula.nombre}"?`)) {
      this.operando.set(true);
      this.inventario.eliminarSala(aula.id).subscribe({
        next: () => {
          this.operando.set(false);
          this.aviso.set({ tipo: 'exito', texto: `Aula "${aula.nombre}" eliminada.` });
        },
        error: (error: unknown) => {
          this.operando.set(false);
          this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
        },
      });
    }
  }

  protected cancelarEdicionSala(): void {
    this.editandoSala.set(null);
    this.sala = { nombre: '', capacidadMaxima: 12 };
  }

  /** Abre el formulario de equipo en modo edición con los datos cargados. */
  protected editarEquipo(equipo: Equipo): void {
    this.aviso.set(null);
    this.editandoEquipo.set(equipo);
    this.equipo = {
      salaId: equipo.sala?.id ?? null,
      codigo: equipo.codigo,
      caracteristicas: equipo.caracteristicas ?? '',
      marca: equipo.marca ?? '',
      modelo: equipo.modelo ?? '',
      estado: equipo.estado,
    };
  }

  protected eliminarEquipo(item: Equipo): void {
    this.aviso.set(null);
    if (confirm(`¿Estás seguro de eliminar el equipo "${item.codigo}"?`)) {
      this.operando.set(true);
      this.inventario.eliminarEquipo(item.id).subscribe({
        next: () => {
          this.operando.set(false);
          this.aviso.set({ tipo: 'exito', texto: `Equipo "${item.codigo}" eliminado.` });
        },
        error: (error: unknown) => {
          this.operando.set(false);
          this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
        },
      });
    }
  }

  protected cancelarEdicionEquipo(): void {
    this.editandoEquipo.set(null);
    this.equipo = this.equipoVacio();
  }

  /** Formulario de equipo en blanco, sin rastro de la edición anterior. */
  private equipoVacio(salaId: number | null = null) {
    return {
      salaId,
      codigo: '',
      caracteristicas: '',
      marca: '',
      modelo: '',
      estado: 'OPERATIVO' as EstadoEquipo,
    };
  }

  /** Guarda un alta o una edición de equipo, según haya uno abierto. */
  protected registrarEquipo(): void {
    this.aviso.set(null);

    const codigo = this.equipo.codigo.trim();
    const caracteristicas = this.equipo.caracteristicas.trim();
    const marca = this.equipo.marca.trim();
    const modelo = this.equipo.modelo.trim();
    const salaId = this.equipo.salaId;
    const enEdicion = this.editandoEquipo();

    if (salaId === null) {
      this.aviso.set({ tipo: 'error', texto: 'Elige el aula a la que pertenece el equipo.' });
      return;
    }
    if (!codigo) {
      this.aviso.set({ tipo: 'error', texto: 'Escribe el código del equipo.' });
      return;
    }
    if (
      this.inventario
        .equipos()
        .some((item) => item.id !== enEdicion?.id && item.codigo.trim().toLowerCase() === codigo.toLowerCase())
    ) {
      this.aviso.set({ tipo: 'error', texto: 'Ya existe un equipo con ese código.' });
      return;
    }

    // El equipo que se está editando ya cuenta dentro de su aula: sin descontarlo,
    // el formulario rechazaría el guardado de un aula que está exactamente en su tope.
    const aula = this.inventario.salas().find((item) => item.id === salaId);
    if (aula) {
      const yaEnEstaSala = enEdicion?.sala?.id === salaId;
      const ocupados = this.equiposDe(salaId) - (yaEnEstaSala ? 1 : 0);

      if (ocupados >= aula.capacidadMaxima) {
        this.aviso.set({
          tipo: 'error',
          texto: `"${aula.nombre}" ya tiene ${aula.capacidadMaxima} equipos, su capacidad máxima.`,
        });
        return;
      }
    }

    this.guardandoEquipo.set(true);

    const cuerpo = {
      codigo,
      caracteristicas: caracteristicas || null,
      marca: marca || null,
      modelo: modelo || null,
      estado: this.equipo.estado,
    };

    const peticion = enEdicion
      ? this.inventario.actualizarEquipo(enEdicion.id, cuerpo, salaId)
      : this.inventario.crearEquipo(cuerpo, salaId);

    peticion.subscribe({
      next: (guardado) => {
        this.guardandoEquipo.set(false);
        this.editandoEquipo.set(null);
        // Se deja el aula puesta: es lo normal que se den de alta varios equipos
        // seguidos de la misma, y vaciarla obliga a elegirla otra vez cada vez.
        this.equipo = this.equipoVacio(salaId);
        this.aviso.set({
          tipo: 'exito',
          texto: enEdicion
            ? `Equipo "${guardado.codigo}" actualizado.`
            : `Equipo "${guardado.codigo}" registrado.`,
        });
      },
      error: (error: unknown) => {
        this.guardandoEquipo.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }

  protected cambiarEstadoDe(id: number, evento: Event): void {
    const nuevo = (evento.target as HTMLSelectElement).value as EstadoEquipo;

    const anterior = this.inventario.equipos().find((item) => item.id === id)?.estado;
    if (!anterior || anterior === nuevo) return;

    this.cambiandoEstado.set(true);

    this.inventario.cambiarEstadoEquipo(id, nuevo).subscribe({
      next: () => this.cambiandoEstado.set(false),
      error: (error: unknown) => {
        this.cambiandoEstado.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }
}