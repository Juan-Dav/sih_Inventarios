import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Reveal } from '../../../shared/directives/reveal';

interface HeroStat {
  value: string;
  label: string;
  accent: string;
}

@Component({
  selector: 'app-hero',
  imports: [Reveal],
  templateUrl: './hero.html',
  styleUrl: './hero.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Hero {
  protected readonly stats: readonly HeroStat[] = [
    {
      value: '100%',
      label: 'Equipos vigilados',
      accent: 'bg-gradient-to-r from-brand-400 to-pop-500',
    },
    {
      value: '24/7',
      label: 'Monitoreo en vivo',
      accent: 'bg-gradient-to-r from-aqua-400 to-mint-400',
    },
    {
      value: '0',
      label: 'Pérdidas sin registro',
      accent: 'bg-gradient-to-r from-sun-400 to-pop-400',
    },
  ];

  protected readonly tags = ['Inventarios', 'Mantenimiento', 'Trazabilidad', 'Reportes'] as const;
}
