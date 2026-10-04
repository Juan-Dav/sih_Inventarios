import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import {
  LIMITE_GRATUITO_EQUIPOS_POR_SALA,
  LIMITE_GRATUITO_SALAS,
  PRECIO_PREMIUM,
  formatearPesos,
} from '../../../core/models/plan.model';
import { InventarioService } from '../../../core/services/inventario.service';
import { PlanService } from '../../../core/services/plan.service';

type Aviso = { tipo: 'exito' | 'error'; texto: string };

/**
 * Módulo de planes del técnico y del docente.
 *
 * Los tres paneles pueden contratar y cancelar el Premium. El plan es una sola
 * fila de la institución, así que el botón que pulse cualquiera de los dos
 * cambia el mismo dato que ve el tercero: no hay un plan por persona que se
 * pueda contradecir.
 *
 * Cancelar no borra inventario. Si ya hay más aulas de las que el gratuito
 * permite, el módulo lo dice antes de confirmar en vez de dejar que el
 * administrador descubra el límite cuando intente registrar la siguiente.
 *
 * No hace falta pedir el plan al entrar: `PlanService` es singleton y lo carga
 * una vez al arrancar, y el propio servicio lo refresca al comprar o cancelar.
 */
@Component({
  selector: 'app-planes',
  styleUrls: ['../../../core/ui/formulario.css'],
  template: `
    <div class="flex flex-col gap-6">
      <!-- ============ Estado actual ============ -->
      <section class="tarjeta">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 class="titulo-modulo">Plan del sistema</h2>
            <p class="subtitulo-modulo">
              Define cuántas aulas y cuántos equipos se pueden registrar. El plan es de la
              institución: lo que contrate cualquiera de los tres paneles aplica para todos.
            </p>
          </div>

          <span class="etiqueta-estado" [class]="plan().esPremium ? et.premium : et.gratuito">
            {{ plan().esPremium ? 'Premium activo' : 'Plan gratuito' }}
          </span>
        </div>

        <div class="mt-6 grid gap-4 sm:grid-cols-2">
          <div class="rounded-2xl border border-ink-200 bg-white/5 p-5">
            <p class="text-[0.65rem] font-bold tracking-[0.14em] text-ink-500 uppercase">Aulas</p>
            @if (plan().esPremium) {
              <p class="mt-2 font-display text-2xl font-extrabold text-ink-900">Ilimitadas</p>
              <p class="mt-1 text-xs text-ink-600">
                Se pueden registrar todas las aulas que necesite la institución.
              </p>
            } @else {
              <p class="mt-2 font-display text-2xl font-extrabold text-ink-900">
                {{ plan().salasRegistradas }} / {{ limiteSalas() }}
              </p>
              <div class="mt-3 h-2 overflow-hidden rounded-full bg-ink-100">
                <div
                  class="h-full rounded-full bg-gradient-to-r from-sun-400 to-pop-500 transition-[width] duration-500"
                  [style.width.%]="consumoSalas()"
                ></div>
              </div>
              <p class="mt-2 text-xs text-ink-600">
                @if (plan().salasRestantes === 0) {
                  Alcanzaste el límite del plan gratuito.
                } @else {
                  {{ plan().salasRestantes === 1 ? 'Queda' : 'Quedan' }}
                  {{ plan().salasRestantes }}
                  {{ plan().salasRestantes === 1 ? 'aula' : 'aulas' }} disponible{{
                    plan().salasRestantes === 1 ? '' : 's'
                  }}.
                }
              </p>
            }
          </div>

          <div class="rounded-2xl border border-ink-200 bg-white/5 p-5">
            <p class="text-[0.65rem] font-bold tracking-[0.14em] text-ink-500 uppercase">
              Equipos por aula
            </p>
            @if (plan().esPremium) {
              <p class="mt-2 font-display text-2xl font-extrabold text-ink-900">Ilimitados</p>
              <p class="mt-1 text-xs text-ink-600">
                Cada aula admite tantos equipos como se necesiten.
              </p>
            } @else {
              <p class="mt-2 font-display text-2xl font-extrabold text-ink-900">
                {{ limiteEquipos() }} por aula
              </p>
              <p class="mt-1 text-xs text-ink-600">
                {{ plan().equiposRegistrados }} equipos registrados en total.
              </p>
            }
          </div>
        </div>

        @if (excedeElGratuito()) {
          <p class="aviso mt-6 bg-sun-400/15 txt-espera">
            Hay {{ plan().salasRegistradas }} aulas registradas y el plan gratuito solo admite
            {{ limiteSalas() }}. No se borra nada: simplemente no se podrán registrar más
            hasta volver a contratar el Premium.
          </p>
        }
      </section>

      <!-- ============ Opciones ============
           Las dos tarjetas se muestran siempre, no solo la que se puede pulsar.
           Si aparece únicamente "Contratar Premium", quien está en el gratuito no
           tiene forma de saber qué está dejando atrás, y quien ya tiene Premium no
           ve que el plan gratuito sigue existiendo como opción de vuelta. -->
      <section class="tarjeta">
        <h2 class="titulo-modulo">Opciones</h2>
        <p class="subtitulo-modulo">
          El Premium no agrega apartados nuevos: quita los topes de cantidad. Por eso lo único que
          cambia entre las dos tarjetas es cuántos registros caben.
        </p>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div
            class="rounded-2xl border p-5"
            [class]="plan().esPremium ? 'border-ink-200' : 'border-sun-400/40 bg-sun-400/5'"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <h3 class="font-display text-lg font-extrabold text-ink-900">Gratuito</h3>
              @if (!plan().esPremium) {
                <span class="etiqueta-estado" [class]="et.gratuito">Tu plan</span>
              }
            </div>

            <p class="mt-3 font-display text-2xl font-extrabold text-ink-900">
              {{ formatearPesos(0) }}
            </p>

            <ul class="mt-4 space-y-2 text-sm text-ink-600">
              <li>Hasta {{ limiteGratuitoSalas }} aulas registradas.</li>
              <li>Hasta {{ limiteGratuitoEquipos }} equipos por aula.</li>
              <li>Historiales, verificaciones y reportes sin límite de tiempo.</li>
            </ul>
          </div>

          <div
            class="rounded-2xl border p-5"
            [class]="plan().esPremium ? 'border-mint-400/40 bg-mint-400/5' : 'border-ink-200'"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <h3 class="font-display text-lg font-extrabold text-ink-900">Premium</h3>
              @if (plan().esPremium) {
                <span class="etiqueta-estado" [class]="et.premium">Tu plan</span>
              }
            </div>

            <p class="mt-3 font-display text-2xl font-extrabold text-ink-900">
              {{ formatearPesos(precio) }}
            </p>
            <p class="text-xs text-ink-500">de la institución, compra simulada</p>

            <ul class="mt-4 space-y-2 text-sm text-ink-600">
              <li>Aulas ilimitadas.</li>
              <li>Equipos ilimitados por aula.</li>
              <li>Todo lo del gratuito, sin los topes.</li>
            </ul>
          </div>
        </div>
      </section>

      <!-- ============ Compra ============ -->
      @if (plan().esPremium) {
        <section class="tarjeta">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 class="titulo-modulo">Premium activo</h2>
              <p class="subtitulo-modulo">
                Comprobante <span class="font-bold">{{ plan().referenciaPago }}</span>. Las aulas
                y los equipos no tienen tope.
              </p>
            </div>

            <button
              type="button"
              class="botón border border-pop-400/40 bg-pop-500/15 text-[#f9a8d4] hover:bg-pop-500/25"
              [disabled]="operando()"
              (click)="pedirCancelacion()"
            >
              {{ cancelando() ? 'Cancelando…' : 'Cancelar plan' }}
            </button>
          </div>
        </section>
      } @else {
        <section class="tarjeta">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h2 class="titulo-modulo">Contratar plan Premium</h2>
              <p class="mt-1 max-w-md text-sm text-ink-600">
                Quita los límites de aulas y equipos y deja la institución con el inventario
                completo. Compra simulada: no se pide tarjeta ni se cobra nada real.
              </p>
            </div>

            <button
              type="button"
              class="botón bg-gradient-to-r from-brand-600 to-pop-600 text-white shadow-lg shadow-brand-600/25 hover:shadow-brand-600/40"
              [disabled]="operando()"
              (click)="comprar()"
            >
              {{ comprando() ? 'Activando…' : 'Contratar por ' + formatearPesos(precio) }}
            </button>
          </div>
        </section>
      }

      <!-- ============ Gracias ============
           Solo aparece después de contratar, no como estado permanente: si se
           quedara en pantalla, la próxima visita abriría con un cartel de
           bienvenida que ya no significa nada. -->
      @if (agradecido(); as referencia) {
        <section class="tarjeta border-mint-400/30">
          <div class="flex flex-wrap items-center gap-5">
            <!-- Cara feliz dibujada, no un emoji del sistema: hereda el color
                 del rol y no depende de la fuente que tenga el equipo. -->
            <svg
              class="h-16 w-16 shrink-0"
              viewBox="0 0 64 64"
              role="img"
              aria-label="Cara feliz"
            >
              <circle
                cx="32"
                cy="32"
                r="28"
                fill="rgb(255 255 255 / 0.06)"
                stroke="var(--panel-acento)"
                stroke-width="3"
              />
              <circle cx="23" cy="26" r="3.5" fill="var(--panel-acento)" />
              <circle cx="41" cy="26" r="3.5" fill="var(--panel-acento)" />
              <path
                d="M20 38c3.4 5.2 7.4 7.5 12 7.5s8.6-2.3 12-7.5"
                fill="none"
                stroke="var(--panel-acento)"
                stroke-width="3.5"
                stroke-linecap="round"
              />
            </svg>

            <div class="min-w-0 flex-1">
              <h2 class="titulo-modulo">¡Gracias por tu compra!</h2>
              <p class="mt-1 text-sm text-ink-600">
                Tu plan Premium ya está activo. A partir de ahora se pueden registrar todas las
                aulas y equipos que necesite la institución.
              </p>
              <p class="mt-2 text-xs text-ink-500">
                Comprobante <span class="font-bold">{{ referencia }}</span
                >. Puedes cancelarlo cuando quieras desde este mismo apartado.
              </p>
            </div>

            <button type="button" class="botón boton-secundario" (click)="agradecido.set(null)">
              Entendido
            </button>
          </div>
        </section>
      }

      @if (aviso(); as mensaje) {
        <p class="aviso" [class]="mensaje.tipo === 'error' ? et.error : et.exito">
          {{ mensaje.texto }}
        </p>
      }

      <!-- ============ Confirmación de la baja ============ -->
      @if (confirmandoCancelacion()) {
        <section class="tarjeta border-pop-400/30">
          <h2 class="titulo-modulo">¿Cancelar el plan Premium?</h2>
          <p class="mt-1 text-sm text-ink-600">
            Volverá el plan gratuito y con él los topes de aulas y equipos. Nada de lo que ya
            está registrado se borra.
          </p>

          @if (excedeElGratuito()) {
            <p class="aviso mt-4 bg-sun-400/15 txt-espera">
              Ojo: ya hay {{ plan().salasRegistradas }} aulas y el gratuito admite
              {{ limiteSalas() }}. Vas a poder verlas todas, pero no registrar ninguna más hasta
              volver a contratar.
            </p>
          }

          <div class="mt-5 flex flex-wrap gap-3">
            <button
              type="button"
              class="botón border border-pop-400/40 bg-pop-500/15 text-[#f9a8d4] hover:bg-pop-500/25"
              [disabled]="operando()"
              (click)="cancelar()"
            >
              {{ cancelando() ? 'Cancelando…' : 'Sí, cancelar el Premium' }}
            </button>
            <button
              type="button"
              class="botón boton-secundario"
              [disabled]="operando()"
              (click)="confirmandoCancelacion.set(false)"
            >
              Mejor no
            </button>
          </div>
        </section>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Planes {
  private readonly planService = inject(PlanService);
  private readonly inventario = inject(InventarioService);

  protected readonly et = {
    premium: 'bg-mint-400/15 txt-ok ring-1 ring-inset ring-mint-400/30',
    gratuito: 'bg-sun-400/15 txt-espera ring-1 ring-inset ring-sun-400/30',
    exito: 'bg-mint-400/15 txt-ok',
    error: 'bg-pop-500/15 txt-alerta',
  } as const;

  protected readonly formatearPesos = formatearPesos;

  protected readonly aviso = signal<Aviso | null>(null);
  protected readonly comprando = signal(false);
  protected readonly cancelando = signal(false);
  protected readonly confirmandoCancelacion = signal(false);

  /**
   * `true` mientras cualquiera de las dos operaciones esté en vuelo.
   *
   * Los botones se bloquean con esto y no con `comprando()` o `cancelando()`
   * por separado: son operaciones excluyentes sobre la misma fila del plan, y
   * durante una el otro botón tendría que estar muerto igual.
   */
  protected readonly operando = computed(() => this.comprando() || this.cancelando());

  /**
   * Comprobante de la última compra, o `null` si en esta sesión no se contrató.
   *
   * Es un signal aparte del plan porque el plan Premium puede seguir activo de
   * una visita anterior: el cartel de agradecimiento tiene que hablar de "tu
   * compra" y solo existe si ocurrió en esta sesión.
   */
  protected readonly agradecido = signal<string | null>(null);

  /**
   * Vista del plan con los `null` ya resueltos.
   *
   * Si el GET aún no ha respondido, se parte de un plan gratuito en cero: así
   * la pantalla muestra números en vez de campos vacíos mientras carga.
   */
  protected readonly plan = computed(() => {
    const estado = this.planService.plan();
    const base = estado ?? {
      plan: 'GRATUITO',
      precio: PRECIO_PREMIUM,
      maxSalas: LIMITE_GRATUITO_SALAS,
      maxEquiposPorSala: LIMITE_GRATUITO_EQUIPOS_POR_SALA,
      referenciaPago: null,
      salasRegistradas: 0,
      equiposRegistrados: 0,
    };

    return {
      ...base,
      esPremium: base.plan === 'PREMIUM',
      /** Aulas que aún se pueden registrar; 0 cuando el plan ya no da más. */
      salasRestantes:
        base.maxSalas === null ? 0 : Math.max(0, base.maxSalas - base.salasRegistradas),
    };
  });

  /**
   * Precio del Premium que se está ofreciendo, no el del plan que hay activo.
   *
   * Mientras el plan es gratuito el backend responde `precio = 0` porque eso es
   * lo que cuesta ese plan; leer el campo haría que el botón anunciara
   * "Contratar por $ 0".
   */
  protected readonly precio = PRECIO_PREMIUM;

  /**
   * Topes del plan gratuito, tal y como los pinta la tarjeta de "Opciones".
   *
   * No salen de `plan()` a propósito: esa tarjeta compara las dos opciones, y el
   * Premium ya activo no manda ningún `maxSalas`, así que al leerlos de ahí el
   * "hasta 1 aula" desaparecería justo cuando se necesita para entender qué se
   * pierde al cancelar.
   */
  protected readonly limiteGratuitoSalas = LIMITE_GRATUITO_SALAS;
  protected readonly limiteGratuitoEquipos = LIMITE_GRATUITO_EQUIPOS_POR_SALA;

  protected readonly limiteSalas = computed(() => this.plan().maxSalas);
  protected readonly limiteEquipos = computed(() => this.plan().maxEquiposPorSala);

  /**
   * `true` cuando lo registrado ya no cabe en el plan gratuito.
   *
   * Cancelar en ese estado está permitido a propósito: el inventario se
   * conserva y solo deja de poder crecer. Lo que no se puede es que el técnico
   * se entere de eso por un rechazo al intentar registrar.
   */
  protected readonly excedeElGratuito = computed(() => {
    const plan = this.plan();
    const maxSalas = plan.maxSalas;

    return !plan.esPremium && maxSalas !== null && plan.salasRegistradas > maxSalas;
  });

  /** Porcentaje de aulas ocupadas, acotado a 100 para que la barra no se desborde. */
  protected consumoSalas(): number {
    const estado = this.plan();
    if (!estado.maxSalas || estado.maxSalas === 0) return 0;
    return Math.min(100, Math.round((estado.salasRegistradas / estado.maxSalas) * 100));
  }

  protected pedirCancelacion(): void {
    this.aviso.set(null);
    this.confirmandoCancelacion.set(true);
  }

  protected comprar(): void {
    this.aviso.set(null);
    this.comprando.set(true);

    this.planService.comprarPremium().subscribe({
      next: (estado) => {
        this.comprando.set(false);
        // El plan nuevo destapa más aulas de las que ya hay, así que se recarga
        // el inventario para que los contadores no queden cortos.
        this.inventario.cargarTodo();
        this.agradecido.set(estado.referenciaPago ?? 'SIM-Confirmada');
        this.aviso.set(null);
      },
      error: (error: unknown) => {
        this.comprando.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }

  protected cancelar(): void {
    this.aviso.set(null);
    this.cancelando.set(true);

    this.planService.cancelarPremium().subscribe({
      next: (estado) => {
        this.cancelando.set(false);
        this.confirmandoCancelacion.set(false);
        this.agradecido.set(null);
        // Los avisos de límite del panel se calculan desde este mismo estado,
        // así que sin recargar quedarían mostrando el Premium un rato más.
        this.inventario.cargarTodo();
        this.aviso.set({
          tipo: 'exito',
          texto:
            estado.estadoPago === 'CANCELADO'
              ? 'Plan Premium cancelado. Volviste al plan gratuito y ya aplican sus límites.'
              : 'Ya estabas en el plan gratuito.',
        });
      },
      error: (error: unknown) => {
        this.cancelando.set(false);
        this.aviso.set({ tipo: 'error', texto: this.inventario.leerError(error) });
      },
    });
  }
}