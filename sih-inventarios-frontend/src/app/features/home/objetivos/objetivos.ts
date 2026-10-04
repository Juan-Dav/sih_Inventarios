import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Reveal } from '../../../shared/directives/reveal';

interface Objective {
  index: string;
  title: string;
  body: string;
  icon: string;
  accent: string;
  glow: string;
}

@Component({
  selector: 'app-objetivos',
  imports: [Reveal],
  templateUrl: './objetivos.html',
  styleUrl: './objetivos.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Objetivos {
  protected readonly objectives: readonly Objective[] = [
    {
      index: '01',
      title: 'Registro detallado',
      body: 'Identificar y registrar los equipos tecnológicos recopilando información precisa sobre su ubicación, estado y características de hardware.',
      icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4',
      accent: 'bg-brand-100 text-brand-600 group-hover:bg-brand-gradient group-hover:text-white',
      glow: 'bg-brand-500',
    },
    {
      index: '02',
      title: 'Verificación periódica',
      body: 'Auditar el estado físico y funcional de los equipos constantemente para mantener actualizada la base de datos del inventario.',
      icon: 'M12 8v4l3 2m6-2a9 9 0 11-18 0 9 9 0 0118 0z',
      accent:
        'bg-aqua-300/25 text-aqua-500 group-hover:bg-gradient-to-br group-hover:from-aqua-400 group-hover:to-brand-600 group-hover:text-white',
      glow: 'bg-aqua-400',
    },
    {
      index: '03',
      title: 'Detección de fallas',
      body: 'Registrar equipos faltantes, reportar daños y gestionar alertas de fallas para agilizar la respuesta del servicio técnico.',
      icon: 'M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z',
      accent:
        'bg-sun-400/20 text-sun-500 group-hover:bg-gradient-to-br group-hover:from-sun-400 group-hover:to-pop-600 group-hover:text-white',
      glow: 'bg-sun-400',
    },
    {
      index: '04',
      title: 'Organización centralizada',
      body: 'Estructurar la información de todos los activos tecnológicos para empoderar la toma de decisiones y la administración de recursos.',
      icon: 'M4 6h16M4 10h16M4 14h10M4 18h7',
      accent:
        'bg-pop-200 text-pop-600 group-hover:bg-gradient-to-br group-hover:from-pop-500 group-hover:to-brand-700 group-hover:text-white',
      glow: 'bg-pop-500',
    },
  ];
}
