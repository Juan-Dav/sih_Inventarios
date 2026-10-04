import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Footer } from '../../core/layout/footer/footer';
import { Hero } from './hero/hero';
import { Integrantes } from './integrantes/integrantes';
import { MisionVision } from './mision-vision/mision-vision';
import { Objetivos } from './objetivos/objetivos';
import { QuienesSomos } from './quienes-somos/quienes-somos';

/**
 * Página de inicio pública.
 *
 * Antes sus secciones vivían directamente en app.html, lo que hacía que
 * cualquier ruta del router se dibujara debajo de la portada. Al grouping
 * las rutas, la portada pasó a ser una ruta más.
 */
@Component({
  selector: 'app-home-page',
  imports: [Hero, QuienesSomos, MisionVision, Objetivos, Integrantes, Footer],
  template: `
    <main>
      <app-hero />
      <app-quienes-somos />
      <app-mision-vision />
      <app-objetivos />
      <app-integrantes />
    </main>
    <app-footer />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {}
