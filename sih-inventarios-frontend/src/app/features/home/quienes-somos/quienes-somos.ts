import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Reveal } from '../../../shared/directives/reveal';

interface Pill {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-quienes-somos',
  imports: [Reveal],
  templateUrl: './quienes-somos.html',
  styleUrl: './quienes-somos.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuienesSomos {
  protected readonly pills: readonly Pill[] = [
    {
      label: 'Inventarios',
      icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
    },
    {
      label: 'Hardware',
      icon: 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    },
    { label: 'Software', icon: 'M8 9l-3 3m0 0l3 3m-3-3h12M9 5l-3 3m0 0l3 3m-3-3h12' },
    { label: 'Soporte', icon: 'M18.487 18.487A12 12 0 005.513 5.513m17.974 0a12 12 0 010 12.974' },
  ];
}
