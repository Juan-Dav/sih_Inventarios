import {
  afterNextRender,
  Directive,
  ElementRef,
  inject,
  input,
  numberAttribute,
} from '@angular/core';

/**
 * Un solo IntersectionObserver por valor de umbral en lugar de uno por
 * elemento. La página tiene decenas de elementos con `appReveal` y cada
 * observador propio obliga al navegador a evaluar el árbol por separado.
 */
const observers = new Map<number, IntersectionObserver>();

/**
 * Techo del retardo escalonado.
 *
 * Los retardos se acumulan por elemento (`índice * 120`), así que en una
 * rejilla larga el último bloque esperaba casi un segundo. Encima, con la
 * transición de opacidad encima, el texto se quedaba translúcido durante todo
 * ese tiempo. 240ms mantiene el efecto de "van apareciendo en cascada" sin
 * llegar a delaying la lectura.
 */
const RETARDO_MAXIMO_MS = 240;

function observerFor(threshold: number): IntersectionObserver {
  let instance = observers.get(threshold);

  if (!instance) {
    instance = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          entry.target.classList.add('reveal-in');
          instance?.unobserve(entry.target);
        }
      },
      { threshold, rootMargin: '0px 0px -20px 0px' },
    );
    observers.set(threshold, instance);
  }

  return instance;
}

/**
 * Revela el elemento con una animación de entrada cuando entra en el viewport.
 * Se activa con `appReveal` y opcionalmente acepta un retardo en ms:
 *
 *   <div appReveal [appRevealDelay]="120">…</div>
 *
 * `afterNextRender` garantiza que IntersectionObserver solo se cree en el
 * navegador, por lo que es seguro durante el prerenderizado SSR.
 */
@Directive({
  selector: '[appReveal]',
})
export class Reveal {
  /** Retardo milisegundos antes de mostrar el elemento. */
  readonly appRevealDelay = input(0, { alias: 'appRevealDelay', transform: numberAttribute });

  /** Umbral de visibilidad (0-1) necesario para disparar la animación. */
  readonly appRevealThreshold = input(0.15, {
    alias: 'appRevealThreshold',
    transform: numberAttribute,
  });

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterNextRender(() => {
      const node = this.host.nativeElement;

      const retardo = Math.min(this.appRevealDelay(), RETARDO_MAXIMO_MS);
      node.style.setProperty('--reveal-delay', `${retardo}ms`);

      if (typeof IntersectionObserver === 'undefined') {
        node.classList.add('reveal-in');
        return;
      }

      observerFor(this.appRevealThreshold()).observe(node);
    });
  }
}
