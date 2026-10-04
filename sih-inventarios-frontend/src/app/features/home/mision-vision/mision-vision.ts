import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Reveal } from '../../../shared/directives/reveal';

interface Pillar {
  key: string;
  title: string;
  body: string;
  icon: string;
  badge: string;
  /** Paleta watery */
  ring: string;
  iconWrap: string;
  bar: string;
  dot: string;
}

@Component({
  selector: 'app-mision-vision',
  imports: [Reveal],
  templateUrl: './mision-vision.html',
  styleUrl: './mision-vision.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisionVision {
  protected readonly pillars: readonly Pillar[] = [
    {
      key: 'mision',
      title: 'Nuestra Misión',
      badge: 'Misión',
      body: 'SIH Inventarios es una empresa dedicada al desarrollo de aplicaciones para controlar y administrar la información de entidades académicas y corporativas. Ofrecemos soluciones tecnológicas que optimizan los procesos de gestión, garantizando orden, seguridad y eficiencia en el manejo de inventarios.',
      icon: 'M13 10V3L4 14h7v7l9-11h-7z',
      ring: 'hover:border-brand-400/60 hover:shadow-brand-500/20',
      iconWrap: 'bg-brand-100 text-brand-600 group-hover:bg-brand-gradient group-hover:text-white',
      bar: 'bg-gradient-to-b from-brand-400 to-brand-600',
      dot: 'bg-brand-500',
    },
    {
      key: 'vision',
      title: 'Nuestra Visión',
      badge: 'Visión 2030',
      body: 'Para el año 2030, SIH Inventarios se consolidará como la aplicación líder en gestión de recursos para los colegios arquidiocesanos, destacándose por su innovación en el control de activos, la optimización de procesos administrativos y el fortalecimiento de la gestión tecnológica institucional.',
      icon: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z',
      ring: 'hover:border-pop-400/60 hover:shadow-pop-500/20',
      iconWrap:
        'bg-pop-100 text-pop-600 group-hover:bg-gradient-to-br group-hover:from-pop-500 group-hover:to-brand-600 group-hover:text-white',
      bar: 'bg-gradient-to-b from-pop-400 to-pop-600',
      dot: 'bg-pop-500',
    },
  ];
}
