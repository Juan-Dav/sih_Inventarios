import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Reveal } from '../../../shared/directives/reveal';

interface Member {
  name: string;
  role: string;
  gradient: string;
}

@Component({
  selector: 'app-integrantes',
  imports: [Reveal],
  templateUrl: './integrantes.html',
  styleUrl: './integrantes.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Integrantes {
  protected readonly members: readonly Member[] = [
    {
      name: 'Rweymond Gerard Lopez Tellez',
      role: 'Ingeniería & Desarrollo',
      gradient: 'from-brand-500 to-brand-700',
    },
    {
      name: 'Sara Camila Palacio Arredondo',
      role: 'Ingeniería & Desarrollo',
      gradient: 'from-pop-500 to-brand-600',
    },
    {
      name: 'Yuli Camila Valencia Ramirez',
      role: 'Ingeniería & Desarrollo',
      gradient: 'from-aqua-400 to-brand-600',
    },
    {
      name: 'Sara Michelle Valencia Rincón',
      role: 'Ingeniería & Desarrollo',
      gradient: 'from-mint-400 to-aqua-500',
    },
    {
      name: 'Valeria Villa Castro',
      role: 'Ingeniería & Desarrollo',
      gradient: 'from-sun-400 to-pop-500',
    },
  ];
}
